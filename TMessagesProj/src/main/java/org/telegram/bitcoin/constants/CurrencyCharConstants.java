package org.telegram.bitcoin.constants;

/**
 * Created by Max van de Wiel on 23-3-15.
 */
public enum CurrencyCharConstants {
    CURRENCY_MINUS_SIGN('\uff0d'),
    CURRENCY_PLUS_SIGN('\uff0b'),
    CHAR_ALMOST_EQUAL_TO('\u2248'),
    CHAR_HAIR_SPACE('\u200a'),
    CHAR_THIN_SPACE('\u2009');

    private char value;

    private CurrencyCharConstants(char value) {
        this.value = value;
    }

    public char value() {
        return this.value;
    }
}
