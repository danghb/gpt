package com.danghb.wechatnfctest;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.Test;

public class NfcProtocolTest {
    private static final String TEST_SCHEME = "weixin://dl/business/?t=QDZVQEO2z9f";
    private static final String TEST_HTTPS = "https://wxaurl.cn/test";
    private static final String TEST_HTTP = "http://example.com/test";

    @Test
    public void ndefContainsWechatSchemeAndWechatAar() {
        assertNdefUriAndAar(TEST_SCHEME);
    }

    @Test
    public void ndefSupportsHttpsUrlAndWechatAar() {
        assertNdefUriAndAar(TEST_HTTPS);
    }

    @Test
    public void ndefSupportsHttpUrlAndWechatAar() {
        assertNdefUriAndAar(TEST_HTTP);
    }

    @Test
    public void type4ReadSequenceReturnsNdefFile() {
        byte[] ndef = NdefBuilder.build(TEST_SCHEME);
        Type4NdefEngine engine = new Type4NdefEngine();

        assertArrayEquals(hex("9000"),
                engine.process(hex("00A4040007D276000085010100"), ndef, true));

        assertArrayEquals(hex("9000"),
                engine.process(hex("00A4000C02E103"), ndef, true));

        byte[] cc = engine.process(hex("00B000000F"), ndef, true);
        assertArrayEquals(concat(Type4NdefEngine.CC_FILE, hex("9000")), cc);

        assertArrayEquals(hex("9000"),
                engine.process(hex("00A4000C02E104"), ndef, true));

        byte[] nlen = engine.process(hex("00B0000002"), ndef, true);
        assertEquals((ndef.length >>> 8) & 0xFF, nlen[0] & 0xFF);
        assertEquals(ndef.length & 0xFF, nlen[1] & 0xFF);
        assertEquals(0x90, nlen[2] & 0xFF);
        assertEquals(0x00, nlen[3] & 0xFF);

        int readLength = 2 + ndef.length;
        byte[] full = engine.process(new byte[] {
                0x00, (byte) 0xB0, 0x00, 0x00, (byte) readLength
        }, ndef, true);

        assertEquals(readLength + 2, full.length);
        assertEquals((ndef.length >>> 8) & 0xFF, full[0] & 0xFF);
        assertEquals(ndef.length & 0xFF, full[1] & 0xFF);
        assertArrayEquals(ndef, Arrays.copyOfRange(full, 2, 2 + ndef.length));
        assertEquals(0x90, full[full.length - 2] & 0xFF);
        assertEquals(0x00, full[full.length - 1] & 0xFF);
    }

    @Test
    public void disabledTagRejectsNdefAid() {
        Type4NdefEngine engine = new Type4NdefEngine();
        assertArrayEquals(hex("6A82"),
                engine.process(hex("00A4040007D276000085010100"), new byte[0], false));
    }

    private static void assertNdefUriAndAar(String uri) {
        byte[] ndef = NdefBuilder.build(uri);
        int p = 0;

        assertEquals(0x91, ndef[p++] & 0xFF);
        int typeLen1 = ndef[p++] & 0xFF;
        int payloadLen1 = ndef[p++] & 0xFF;
        assertEquals(1, typeLen1);
        assertEquals('U', ndef[p++] & 0xFF);
        assertEquals(0x00, ndef[p++] & 0xFF);

        byte[] uriBytes = Arrays.copyOfRange(ndef, p, p + payloadLen1 - 1);
        assertEquals(uri, new String(uriBytes, StandardCharsets.UTF_8));
        p += payloadLen1 - 1;

        assertEquals(0x54, ndef[p++] & 0xFF);
        int typeLen2 = ndef[p++] & 0xFF;
        int payloadLen2 = ndef[p++] & 0xFF;

        assertEquals("android.com:pkg",
                new String(ndef, p, typeLen2, StandardCharsets.US_ASCII));
        p += typeLen2;

        assertEquals("com.tencent.mm",
                new String(ndef, p, payloadLen2, StandardCharsets.US_ASCII));
        p += payloadLen2;

        assertEquals(ndef.length, p);
    }

    private static byte[] hex(String s) {
        return Type4NdefEngine.hex(s);
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] r = new byte[a.length + b.length];
        System.arraycopy(a, 0, r, 0, a.length);
        System.arraycopy(b, 0, r, a.length, b.length);
        return r;
    }
}
