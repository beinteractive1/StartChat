package org.telegram.bitcoin.constants;

import android.text.format.DateUtils;

/**
 * Created by Max van de Wiel on 9-3-15.
 */
public enum TimeoutConstants {
    HTTP_TIMEOUT_MS(15 * (int) DateUtils.SECOND_IN_MILLIS),
    PEER_TIMEOUT_MS(15 * (int) DateUtils.SECOND_IN_MILLIS),
    LAST_USAGE_THRESHOLD_JUST_MS((int)DateUtils.HOUR_IN_MILLIS),
    LAST_USAGE_THRESHOLD_RECENTLY_MS(2 * (int)DateUtils.DAY_IN_MILLIS);

    private int timeout;

    private TimeoutConstants(int timeout) {
        this.timeout = timeout;
    }

    public int timeout() {
        return this.timeout;
    }
}