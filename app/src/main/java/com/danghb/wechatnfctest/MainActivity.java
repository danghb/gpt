package com.danghb.wechatnfctest;

import android.app.Activity;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.nfc.NfcAdapter;
import android.nfc.cardemulation.CardEmulation;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final String DEFAULT_TEST_URI = "weixin://dl/business/?t=B6pHVrURvPk";

    private SharedPreferences prefs;
    private EditText uriEdit;
    private TextView status;
    private Button toggleButton;
    private NfcAdapter nfcAdapter;
    private ComponentName serviceComponent;

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
        title.setText("微信小程序 NFC 模拟器");
        title.setTextSize(24);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(title);

        TextView info = new TextView(this);
        info.setText("已内置 NFC Scheme 测试样本\n\n按照微信 NFC 文档模拟：\nURI Record + 微信 AAR (com.tencent.mm)\n\n默认值来自 2026-04-24 公开的 generateNFCScheme 实战记录，也可以直接替换成你自己的 NFC Scheme。");
        info.setTextSize(15);
        info.setPadding(0, dp(14), 0, dp(14));
        root.addView(info);

        uriEdit = new EditText(this);
        uriEdit.setHint("weixin://dl/business/?t=...");
        uriEdit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        uriEdit.setSingleLine(false);
        uriEdit.setMinLines(3);

        String savedUri = prefs.getString("uri", "");
        if (savedUri == null || savedUri.trim().isEmpty()
                || savedUri.startsWith("https://wxaurl.cn/")) {
            savedUri = DEFAULT_TEST_URI;
        }
        uriEdit.setText(savedUri);

        root.addView(uriEdit, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        toggleButton = new Button(this);
        toggleButton.setOnClickListener(v -> toggle());
        LinearLayout.LayoutParams buttonLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        buttonLp.topMargin = dp(16);
        root.addView(toggleButton, buttonLp);

        status = new TextView(this);
        status.setTextSize(15);
        status.setPadding(0, dp(16), 0, 0);
        root.addView(status);

        TextView tip = new TextView(this);
        tip.setText("直接点击“开始模拟”，再用另一台 Android 或 iPhone 靠近本机 NFC 天线。\n\n默认 Scheme：weixin://dl/business/?t=B6pHVrURvPk");
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
        if (nfcAdapter != null && getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION)) {
            try {
                CardEmulation.getInstance(nfcAdapter).unsetPreferredService(this);
            } catch (Exception ignored) {}
        }
        super.onPause();
    }

    private void toggle() {
        boolean enabled = prefs.getBoolean("enabled", false);
        if (enabled) {
            prefs.edit().putBoolean("enabled", false).apply();
            refreshUi();
            return;
        }

        String uri = uriEdit.getText().toString().trim();
        if (uri.isEmpty()) {
            Toast.makeText(this, "先输入 NFC Scheme", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!uri.startsWith("weixin://")) {
            Toast.makeText(this, "微信 NFC 应使用 weixin:// URL Scheme", Toast.LENGTH_SHORT).show();
            return;
        }

        prefs.edit().putString("uri", uri).putBoolean("enabled", true).apply();
        preferServiceIfNeeded();
        refreshUi();
    }

    private void preferServiceIfNeeded() {
        if (!prefs.getBoolean("enabled", false)) return;
        if (nfcAdapter == null || !nfcAdapter.isEnabled()) return;
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION)) return;
        try {
            CardEmulation.getInstance(nfcAdapter).setPreferredService(this, serviceComponent);
        } catch (Exception ignored) {}
    }

    private void refreshUi() {
        boolean hce = getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION);
        boolean enabled = prefs.getBoolean("enabled", false);

        if (nfcAdapter == null || !hce) {
            status.setText("状态：本机不支持 NFC HCE，不能模拟标签");
            toggleButton.setText("本机不支持 HCE");
            toggleButton.setEnabled(false);
            return;
        }

        toggleButton.setEnabled(true);
        if (!nfcAdapter.isEnabled()) {
            status.setText("状态：NFC 未开启，请先在系统设置里打开 NFC");
            toggleButton.setText(enabled ? "停止模拟" : "开始模拟");
            return;
        }

        if (enabled) {
            String uri = prefs.getString("uri", DEFAULT_TEST_URI);
            status.setText("状态：正在模拟\n" + uri);
            toggleButton.setText("停止模拟");
        } else {
            status.setText("状态：未开始");
            toggleButton.setText("开始模拟");
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
