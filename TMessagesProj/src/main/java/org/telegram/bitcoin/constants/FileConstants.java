package org.telegram.bitcoin.constants;

import org.bitcoinj.core.NetworkParameters;

/**
 * Created by Max van de Wiel on 9-3-15.
 */
public enum FileConstants implements Network {
    FILENAME_NETWORK_SUFFIX(NETWORK_PARAMETERS.getId().equals(NetworkParameters.ID_MAINNET) ? "" : "-testnet"),

    /** Filename of the wallet. */
    WALLET_FILENAME_PROTOBUF("wallet-protobuf" + FILENAME_NETWORK_SUFFIX),

    /** Filename of the automatic key backup (old format, can only be read). */
    WALLET_KEY_BACKUP_BASE58("key-backup-base58" + FILENAME_NETWORK_SUFFIX),

    /** Filename of the automatic wallet backup. */
    WALLET_KEY_BACKUP_PROTOBUF("key-backup-protobuf" + FILENAME_NETWORK_SUFFIX),

    /** Filename of the manual key backup (old format, can only be read). */
    EXTERNAL_WALLET_KEY_BACKUP("bitcoin-wallet-keys" + FILENAME_NETWORK_SUFFIX),

    /** Filename of the manual wallet backup. */
    EXTERNAL_WALLET_BACKUP("bitcoin-wallet-backup" + FILENAME_NETWORK_SUFFIX),

    /** Filename of the block store for storing the chain. */
    BLOCKCHAIN_FILENAME("blockchain" + FILENAME_NETWORK_SUFFIX),

    /** Filename of the block checkpoints file. */
    CHECKPOINTS_FILENAME("checkpoints" + FILENAME_NETWORK_SUFFIX + ".txt");

    private String reference;

    private FileConstants(String reference) {
        this.reference = reference;
    }

    public String reference() {
        return this.reference;
    }
}
