package org.telegram.bitcoin.constants;

/**
 * Created by Max van de Wiel on 9-3-15.
 */
public enum URLS {
    EXPLORE_BASE_URL_PROD("https://www.biteasy.com/"),
    EXPLORE_BASE_URL_TEST("https://www.biteasy.com/testnet/");

    private String url;

    private URLS(String url) {
        this.url = url;
    }
}
