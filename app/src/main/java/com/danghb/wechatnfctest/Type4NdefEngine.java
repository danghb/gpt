package com.danghb.wechatnfctest;

import java.util.Arrays;

public final class Type4NdefEngine {
    private static final byte[] NDEF_AID = hex("D2760000850101");
    private static final byte[] CC_FILE_ID = hex("E103");
    private static final byte[] NDEF_FILE_ID = hex("E104");

    static final byte[] OK = hex("9000");
    static final byte[] FILE_NOT_FOUND = hex("6A82");
    static final byte[] WRONG_PARAMS = hex("6B00");
    static final byte[] COMMAND_NOT_ALLOWED = hex("6986");
    static final byte[] INS_NOT_SUPPORTED = hex("6D00");
    static final byte[] CC_FILE = hex("000F2000FF00FF0406E1047FFF00FF");

    private static final int FILE_NONE = 0;
    private static final int FILE_CC = 1;
    private static final int FILE_NDEF = 2;

    private int selectedFile = FILE_NONE;

    public byte[] process(byte[] apdu, byte[] ndefMessage, boolean enabled) {
        if (apdu == null || apdu.length < 4) return WRONG_PARAMS;
        if (ndefMessage == null) ndefMessage = new byte[0];

        int ins = apdu[1] & 0xFF;

        if (ins == 0xA4) {
            byte[] data = commandData(apdu);
            int p1 = apdu[2] & 0xFF;

            if (p1 == 0x04 && Arrays.equals(data, NDEF_AID)) {
                if (!enabled) return FILE_NOT_FOUND;
                selectedFile = FILE_NONE;
                return OK;
            }

            if (p1 == 0x00 && Arrays.equals(data, CC_FILE_ID)) {
                selectedFile = FILE_CC;
                return OK;
            }

            if (p1 == 0x00 && Arrays.equals(data, NDEF_FILE_ID)) {
                selectedFile = FILE_NDEF;
                return OK;
            }

            return FILE_NOT_FOUND;
        }

        if (ins == 0xB0) {
            if (selectedFile == FILE_NONE) return COMMAND_NOT_ALLOWED;

            int offset = ((apdu[2] & 0xFF) << 8) | (apdu[3] & 0xFF);
            int le = readLe(apdu);
            if (le <= 0) le = 256;

            byte[] file = selectedFile == FILE_CC
                    ? CC_FILE
                    : buildNdefFile(ndefMessage);

            if (offset > file.length) return WRONG_PARAMS;

            int end = Math.min(file.length, offset + le);
            return concat(Arrays.copyOfRange(file, offset, end), OK);
        }

        return INS_NOT_SUPPORTED;
    }

    public void reset() {
        selectedFile = FILE_NONE;
    }

    private static byte[] buildNdefFile(byte[] ndefMessage) {
        byte[] file = new byte[2 + ndefMessage.length];
        file[0] = (byte) ((ndefMessage.length >>> 8) & 0xFF);
        file[1] = (byte) (ndefMessage.length & 0xFF);
        System.arraycopy(ndefMessage, 0, file, 2, ndefMessage.length);
        return file;
    }

    private static byte[] commandData(byte[] apdu) {
        if (apdu.length < 5) return new byte[0];
        int lc = apdu[4] & 0xFF;
        if (lc == 0 || apdu.length < 5 + lc) return new byte[0];
        return Arrays.copyOfRange(apdu, 5, 5 + lc);
    }

    private static int readLe(byte[] apdu) {
        if (apdu.length >= 5) return apdu[4] & 0xFF;
        return 0;
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] r = new byte[a.length + b.length];
        System.arraycopy(a, 0, r, 0, a.length);
        System.arraycopy(b, 0, r, a.length, b.length);
        return r;
    }

    static byte[] hex(String s) {
        byte[] out = new byte[s.length() / 2];
        for (int i = 0; i < s.length(); i += 2) {
            out[i / 2] = (byte) Integer.parseInt(s.substring(i, i + 2), 16);
        }
        return out;
    }
}
