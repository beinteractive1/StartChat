package org.telegram.bitcoin.constants;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;

/**
 * Created by Max van de Wiel on 9-3-15.
 */
public interface Network {
    boolean TEST = false;
    NetworkParameters NETWORK_PARAMETERS = TEST ? TestNet3Params.get() : MainNetParams.get();
    String FILENAME_NETWORK_SUFFIX = NETWORK_PARAMETERS.getId().equals(NetworkParameters.ID_MAINNET) ? "" : "-testnet";

    public static final int MEMORY_CLASS_LOWEND = 48;

    /** MIME type used for transmitting single transactions. */
    public static final String MIMETYPE_TRANSACTION = "application/x-btctx";

    /** MIME type used for transmitting wallet backups. */
    public static final String MIMETYPE_WALLET_BACKUP = "application/x-bitcoin-wallet-backup";

    String EXPLORE_BASE_URL_PROD = "https://www.biteasy.com/";
    String EXPLORE_BASE_URL_TEST = "https://www.biteasy.com/testnet/";
    /** Base URL for browsing transactions, blocks or addresses. */
    String EXPLORE_BASE_URL = NETWORK_PARAMETERS.getId().equals(NetworkParameters.ID_MAINNET) ? EXPLORE_BASE_URL_PROD : EXPLORE_BASE_URL_TEST;

    String BITEASY_API_URL_PROD = "https://api.biteasy.com/blockchain/v1/";
    String BITEASY_API_URL_TEST = "https://api.biteasy.com/testnet/v1/";
    /** Base URL for blockchain API. */
    String BITEASY_API_URL = NETWORK_PARAMETERS.getId().equals(NetworkParameters.ID_MAINNET) ? BITEASY_API_URL_PROD : BITEASY_API_URL_TEST;

    /** URL to fetch version alerts from. */
    String VERSION_URL = "http://todo";

    /** User-agent to use for network access. */
    public static final String USER_AGENT = "Bitcoin Wallet";


}
