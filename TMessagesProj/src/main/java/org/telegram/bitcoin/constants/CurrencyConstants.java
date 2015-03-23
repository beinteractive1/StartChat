package org.telegram.bitcoin.constants;

/**
 * Created by Max van de Wiel on 9-3-15.
 */
public enum CurrencyConstants {
    US_EXCHANGE_CURRENCY("USD"),
    EUR_EXCHANGE_CURRENCY("EUR"),
    DEFAULT_EXCHANGE_CURRENCY("EUR");

    private String value;

    private CurrencyConstants(String value) {
        this.value = value;
    }

    public String value() {
        return this.value;
    }
}
