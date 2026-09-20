package com.danghb.wechatnfctest;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

public final class NdefBuilder {
    private static final byte[] TYPE_URI = new byte[]{0x55};
    private static final byte[] AAR_TYPE = "android.com:pkg".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] WECHAT_PACKAGE = "com.tencent.mm".getBytes(StandardCharsets.US_ASCII);

    private NdefBuilder() {}

    public static byte[] build(String uri) {
        if (uri == null) uri = "";
        byte[] uriText = uri.getBytes(StandardCharsets.UTF_8);
        byte[] uriPayload = new byte[1 + uriText.length];
        uriPayload[0] = 0x00;
        System.arraycopy(uriText, 0, uriPayload, 1, uriText.length);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeRecord(out, true, false, 0x01, TYPE_URI, uriPayload);
        writeRecord(out, false, true, 0x04, AAR_TYPE, WECHAT_PACKAGE);
        return out.toByteArray();
    }

    private static void writeRecord(ByteArrayOutputStream out, boolean mb, boolean me,
                                    int tnf, byte[] type, byte[] payload) {
        boolean shortRecord = payload.length <= 255;
        int flags = (mb ? 0x80 : 0) | (me ? 0x40 : 0) | (shortRecord ? 0x10 : 0) | (tnf & 0x07);
        out.write(flags);
        out.write(type.length & 0xFF);
        if (shortRecord) {
            out.write(payload.length & 0xFF);
        } else {
            out.write((payload.length >>> 24) & 0xFF);
            out.write((payload.length >>> 16) & 0xFF);
            out.write((payload.length >>> 8) & 0xFF);
            out.write(payload.length & 0xFF);
        }
        out.write(type, 0, type.length);
        out.write(payload, 0, payload.length);
    }
}
