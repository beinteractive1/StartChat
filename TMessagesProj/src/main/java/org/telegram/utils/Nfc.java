package org.telegram.utils;

import android.nfc.NdefMessage;
import android.nfc.NdefRecord;

import com.google.common.base.Charsets;

import java.util.Arrays;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 * Created by Max van de Wiel on 23-3-15.
 */
public class Nfc
{
    public static NdefRecord createMime(@Nonnull final String mimeType, @Nonnull final byte[] payload)
    {
        final byte[] mimeBytes = mimeType.getBytes(Charsets.US_ASCII);
        final NdefRecord mimeRecord = new NdefRecord(NdefRecord.TNF_MIME_MEDIA, mimeBytes, new byte[0], payload);
        return mimeRecord;
    }

    @CheckForNull
    public static byte[] extractMimePayload(@Nonnull final String mimeType, @Nonnull final NdefMessage message)
    {
        final byte[] mimeBytes = mimeType.getBytes(Charsets.US_ASCII);

        for (final NdefRecord record : message.getRecords())
        {
            if (record.getTnf() == NdefRecord.TNF_MIME_MEDIA && Arrays.equals(record.getType(), mimeBytes))
                return record.getPayload();
        }

        return null;
    }
}
