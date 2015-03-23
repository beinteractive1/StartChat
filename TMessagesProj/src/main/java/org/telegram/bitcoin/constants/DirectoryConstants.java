package org.telegram.bitcoin.constants;

import android.os.Environment;

import java.io.File;

/**
 * Created by Max van de Wiel on 9-3-15.
 */
public enum DirectoryConstants {
    EXTERNAL_WALLET_BACKUP_DIR(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));

    private File directory;

    private DirectoryConstants(File directory) {
        this.directory = directory;
    }

    public File directory() {
        return this.directory;
    }
}
