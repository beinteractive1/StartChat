package org.telegram.bitcoin.constants;

/**
 * Created by Max van de Wiel on 9-3-15.
 */
public enum URLS {
    EXPLORE_BASE_URL_PROD("https://www.biteasy.com/"),
    EXPLORE_BASE_URL_TEST("https://www.biteasy.com/testnet/"),
    SOURCE_URL("https://github.com/AlrickMaduro/StartChat"),
    BINARY_URL("https://TODO_ALIRCK!!"),
    MARKET_APP_URL("market://details?id=%s"),
    WEB_MARKET_APP_URL("https://play.google.com/store/apps/details?id=%s");

    private String url;

    private URLS(String url) {
        this.url = url;
    }

    public String url() {
        return this.url;
    }
}
