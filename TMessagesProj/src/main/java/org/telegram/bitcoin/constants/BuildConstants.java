package org.telegram.bitcoin.constants;

/**
 * Created by Max van de Wiel on 23-3-15.
 */
public enum BuildConstants {
    SDK_JELLY_BEAN(16),
    SDK_JELLY_BEAN_MR2(18);

    private int value;

    private BuildConstants(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
