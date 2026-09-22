package com.danghb.wechatnfctest;

import android.app.Activity;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.cardemulation.CardEmulation;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.Arrays;

public class MainActivity extends Activity implements NfcAdapter.ReaderCallback {
    private static final String DEFAULT_TEST_URI = "weixin://dl/business/?t=QDZVQEO2z9f";

    private SharedPreferences prefs;
    private EditText uriEdit;
    private TextView status;
    private Button toggleButton;
    private Button writeButton;
    private NfcAdapter nfcAdapter;
    private ComponentName serviceComponent;

    private boolean writeMode;
    private boolean pendingVerification;
    private byte[] pendingExpectedNdef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("nfc", MODE_PRIVATE);
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        serviceComponent = new ComponentName(this, NdefHostApduService.class);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(28), dp(20), dp(20));

        TextView title = new TextView(this);
        title.setText("微信小程序 NFC 工具");
        title.setTextSize(24);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(title);

        TextView info = new TextView(this);
        info.setText("支持：\n"
                + "1. 模拟 NFC Forum Type 4 NDEF 标签\n"
                + "2. 写入实体 NDEF 卡并读回校验\n\n"
                + "URI 支持 weixin://、http://、https://；"
                + "NDEF 固定为 URI Record + 微信 AAR (com.tencent.mm)。");
        info.setTextSize(15);
        info.setPadding(0, dp(14), 0, dp(14));
        root.addView(info);

        uriEdit = new EditText(this);
        uriEdit.setHint("weixin://... 或 https://... 或 http://...");
        uriEdit.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_URI
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        uriEdit.setSingleLine(false);
        uriEdit.setMinLines(3);

        String savedUri = prefs.getString("uri", "");
        if (savedUri == null || savedUri.trim().isEmpty()
                || savedUri.equals("weixin://dl/business/?t=B6pHVrURvPk")) {
            savedUri = DEFAULT_TEST_URI;
        }
        uriEdit.setText(savedUri);

        root.addView(uriEdit, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        toggleButton = new Button(this);
        toggleButton.setOnClickListener(v -> toggleSimulation());
        LinearLayout.LayoutParams buttonLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        buttonLp.topMargin = dp(16);
        root.addView(toggleButton, buttonLp);

        writeButton = new Button(this);
        writeButton.setText("写入实体 NFC 卡");
        writeButton.setOnClickListener(v -> toggleWriteMode());
        LinearLayout.LayoutParams writeLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        writeLp.topMargin = dp(8);
        root.addView(writeButton, writeLp);

        status = new TextView(this);
        status.setTextSize(15);
        status.setPadding(0, dp(16), 0, 0);
        root.addView(status);

        TextView tip = new TextView(this);
        tip.setText("写卡流程：点击“写入实体 NFC 卡” → 贴卡 → 写入 → 立即读回逐字节校验。\n"
                + "如果是未格式化空白卡，首次会先格式化并写入，再提示移开后重新贴一次完成校验。\n"
                + "不会锁卡，也不会设为只读。");
        tip.setTextSize(13);
        tip.setPadding(0, dp(18), 0, 0);
        root.addView(tip);

        setContentView(root);
        refreshUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        preferServiceIfNeeded();
        refreshUi();
    }

    @Override
    protected void onPause() {
        stopReaderMode(false);

        if (nfcAdapter != null
                && getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION)) {
            try {
                CardEmulation.getInstance(nfcAdapter).unsetPreferredService(this);
            } catch (Exception ignored) {}
        }

        super.onPause();
    }

    private void toggleSimulation() {
        if (writeMode) {
            Toast.makeText(this, "请先取消写卡模式", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean enabled = prefs.getBoolean("enabled", false);
        if (enabled) {
            prefs.edit().putBoolean("enabled", false).apply();
            refreshUi();
            return;
        }

        String uri = getValidatedUri();
        if (uri == null) return;

        prefs.edit()
                .putString("uri", uri)
                .putBoolean("enabled", true)
                .apply();

        preferServiceIfNeeded();
        refreshUi();
    }

    private void toggleWriteMode() {
        if (writeMode) {
            stopReaderMode(true);
            return;
        }

        String uri = getValidatedUri();
        if (uri == null) return;

        if (nfcAdapter == null) {
            Toast.makeText(this, "本机没有 NFC", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!nfcAdapter.isEnabled()) {
            Toast.makeText(this, "请先打开系统 NFC", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            pendingExpectedNdef = NdefBuilder.build(uri);
            // 先解析一次，确保我们即将写入的原始 NDEF 字节本身合法。
            new NdefMessage(pendingExpectedNdef);
        } catch (FormatException e) {
            Toast.makeText(this, "NDEF 数据格式错误", Toast.LENGTH_SHORT).show();
            return;
        }

        prefs.edit().putString("uri", uri).apply();

        writeMode = true;
        pendingVerification = false;

        unsetPreferredService();

        try {
            nfcAdapter.enableReaderMode(
                    this,
                    this,
                    NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_NFC_B,
                    null
            );
        } catch (Exception e) {
            writeMode = false;
            pendingExpectedNdef = null;
            Toast.makeText(this, "无法进入 NFC 读写模式", Toast.LENGTH_SHORT).show();
            preferServiceIfNeeded();
        }

        refreshUi();
    }

    @Override
    public void onTagDiscovered(Tag tag) {
        if (!writeMode || pendingExpectedNdef == null) return;

        if (pendingVerification) {
            verifyTag(tag);
        } else {
            writeTag(tag);
        }
    }

    private void writeTag(Tag tag) {
        final byte[] expected = pendingExpectedNdef;

        try {
            NdefMessage message = new NdefMessage(expected);
            Ndef ndef = Ndef.get(tag);

            if (ndef != null) {
                ndef.connect();

                if (!ndef.isWritable()) {
                    ndef.close();
                    showWriteStatus("写入失败：这张卡是只读卡，可换一张卡继续重试。");
                    return;
                }

                if (ndef.getMaxSize() < expected.length) {
                    int maxSize = ndef.getMaxSize();
                    ndef.close();
                    showWriteStatus("写入失败：容量不足。需要 " + expected.length
                            + " 字节，卡片最多 " + maxSize + " 字节。");
                    return;
                }

                ndef.writeNdefMessage(message);

                NdefMessage actualMessage = ndef.getNdefMessage();
                ndef.close();

                if (actualMessage == null) {
                    showWriteStatus("写入后读取失败：没有读到 NDEF，请换卡或重新贴卡。");
                    return;
                }

                byte[] actual = actualMessage.toByteArray();
                if (Arrays.equals(expected, actual)) {
                    finishWriteSuccess("写入成功，校验通过："
                            + actualMessage.getRecords().length + " 条 Record，"
                            + actual.length + " 字节。");
                } else {
                    showWriteStatus("写入完成但校验失败：读回内容与写入内容不一致，可重新贴卡重试。");
                }
                return;
            }

            NdefFormatable formatable = NdefFormatable.get(tag);
            if (formatable != null) {
                formatable.connect();
                formatable.format(message);
                formatable.close();

                pendingVerification = true;
                showWriteStatus("空白卡已格式化并写入。请把卡移开，再贴一次进行读回校验。");
                return;
            }

            showWriteStatus("不支持：这张卡既不是 NDEF，也不能格式化为 NDEF。");

        } catch (FormatException e) {
            showWriteStatus("写入失败：NDEF 格式错误。");
        } catch (IOException e) {
            showWriteStatus("写入失败：NFC 通信中断，请保持卡片贴紧后重试。");
        } catch (Exception e) {
            showWriteStatus("写入失败：" + e.getClass().getSimpleName());
        }
    }

    private void verifyTag(Tag tag) {
        try {
            Ndef ndef = Ndef.get(tag);
            if (ndef == null) {
                showWriteStatus("校验失败：重新贴卡后仍未识别为 NDEF。");
                return;
            }

            ndef.connect();
            NdefMessage actualMessage = ndef.getNdefMessage();
            ndef.close();

            if (actualMessage == null) {
                showWriteStatus("校验失败：没有读到 NDEF 内容。");
                return;
            }

            byte[] actual = actualMessage.toByteArray();
            if (Arrays.equals(pendingExpectedNdef, actual)) {
                finishWriteSuccess("写入成功，二次读回校验通过："
                        + actualMessage.getRecords().length + " 条 Record，"
                        + actual.length + " 字节。");
            } else {
                showWriteStatus("校验失败：读回内容与预期不一致，可移开后重新贴卡再试。");
            }

        } catch (IOException e) {
            showWriteStatus("校验失败：NFC 通信中断，请重新贴卡。");
        } catch (Exception e) {
            showWriteStatus("校验失败：" + e.getClass().getSimpleName());
        }
    }

    private String getValidatedUri() {
        String uri = uriEdit.getText().toString().trim();
        if (uri.isEmpty()) {
            Toast.makeText(this, "先输入 URI", Toast.LENGTH_SHORT).show();
            return null;
        }

        String lower = uri.toLowerCase();
        if (!(lower.startsWith("weixin://")
                || lower.startsWith("http://")
                || lower.startsWith("https://"))) {
            Toast.makeText(this,
                    "仅支持 weixin://、http:// 或 https://",
                    Toast.LENGTH_SHORT).show();
            return null;
        }

        return uri;
    }

    private void finishWriteSuccess(String message) {
        runOnUiThread(() -> {
            Toast.makeText(this, "写卡并校验成功", Toast.LENGTH_SHORT).show();
            stopReaderMode(false);
            status.setText(message);
        });
    }

    private void showWriteStatus(String message) {
        runOnUiThread(() -> {
            status.setText(message);
            if (writeMode) {
                writeButton.setText(pendingVerification ? "取消校验" : "取消写卡");
            }
        });
    }

    private void stopReaderMode(boolean showCancelled) {
        if (nfcAdapter != null && writeMode) {
            try {
                nfcAdapter.disableReaderMode(this);
            } catch (Exception ignored) {}
        }

        writeMode = false;
        pendingVerification = false;
        pendingExpectedNdef = null;

        if (writeButton != null) {
            writeButton.setText("写入实体 NFC 卡");
        }

        preferServiceIfNeeded();

        if (showCancelled) {
            status.setText("写卡模式已取消");
        } else {
            refreshUi();
        }
    }

    private void unsetPreferredService() {
        if (nfcAdapter == null) return;
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION)) return;

        try {
            CardEmulation.getInstance(nfcAdapter).unsetPreferredService(this);
        } catch (Exception ignored) {}
    }

    private void preferServiceIfNeeded() {
        if (writeMode) return;
        if (!prefs.getBoolean("enabled", false)) return;
        if (nfcAdapter == null || !nfcAdapter.isEnabled()) return;
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION)) return;

        try {
            CardEmulation.getInstance(nfcAdapter).setPreferredService(this, serviceComponent);
        } catch (Exception ignored) {}
    }

    private void refreshUi() {
        boolean hce = getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION);
        boolean enabled = prefs.getBoolean("enabled", false);

        if (nfcAdapter == null) {
            status.setText("状态：本机没有 NFC");
            toggleButton.setText("本机不支持 NFC");
            toggleButton.setEnabled(false);
            writeButton.setEnabled(false);
            return;
        }

        if (!nfcAdapter.isEnabled()) {
            status.setText("状态：NFC 未开启，请先在系统设置里打开 NFC");
            toggleButton.setEnabled(hce);
            toggleButton.setText(enabled ? "停止模拟" : "开始模拟");
            writeButton.setEnabled(false);
            return;
        }

        writeButton.setEnabled(true);

        if (!hce) {
            toggleButton.setText("本机不支持 HCE");
            toggleButton.setEnabled(false);
        } else {
            toggleButton.setEnabled(!writeMode);
            toggleButton.setText(enabled ? "停止模拟" : "开始模拟");
        }

        if (writeMode) {
            writeButton.setText(pendingVerification ? "取消校验" : "取消写卡");
            status.setText(pendingVerification
                    ? "校验模式：请把刚写入的卡移开后重新贴近手机。"
                    : "写卡模式：请把可写 NFC 卡贴近手机。");
            return;
        }

        writeButton.setText("写入实体 NFC 卡");

        if (enabled && hce) {
            String uri = prefs.getString("uri", DEFAULT_TEST_URI);
            status.setText("状态：正在模拟\n" + uri);
        } else {
            status.setText("状态：未开始");
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
