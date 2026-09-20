package com.danghb.wechatnfctest;

import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;

public class NdefHostApduService extends HostApduService {
    private final Type4NdefEngine engine = new Type4NdefEngine();

    @Override
    public byte[] processCommandApdu(byte[] apdu, Bundle extras) {
        String uri = getSharedPreferences("nfc", MODE_PRIVATE).getString("uri", "");
        byte[] ndef = NdefBuilder.build(uri == null ? "" : uri);
        return engine.process(apdu, ndef, isEnabled());
    }

    @Override
    public void onDeactivated(int reason) {
        engine.reset();
    }

    private boolean isEnabled() {
        return getSharedPreferences("nfc", MODE_PRIVATE).getBoolean("enabled", false);
    }
}
