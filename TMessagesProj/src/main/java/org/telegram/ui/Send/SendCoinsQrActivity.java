package org.telegram.ui.Send;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.VerificationException;
import org.bitcoinj.core.VersionedChecksummedBytes;
import org.telegram.bitcoin.data.PaymentIntent;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.ui.InputParser;
import org.telegram.ui.ScanActivity;

import javax.annotation.Nonnull;

/**
 * Created by Max van de Wiel on 23-3-15.
 */
public final class SendCoinsQrActivity extends Activity {
    private static final int REQUEST_CODE_SCAN = 0;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        startActivityForResult(new Intent(this, ScanActivity.class), REQUEST_CODE_SCAN);
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
        if (requestCode == REQUEST_CODE_SCAN && resultCode == Activity.RESULT_OK) {
            final String input = intent.getStringExtra(ScanActivity.INTENT_EXTRA_RESULT);

            new InputParser.StringInputParser(input) {
                @Override
                protected void handlePaymentIntent(@Nonnull final PaymentIntent paymentIntent) {
                    SendCoinsActivity.start(SendCoinsQrActivity.this, paymentIntent);

                    SendCoinsQrActivity.this.finish();
                }

                @Override
                protected void handlePrivateKey(@Nonnull final VersionedChecksummedBytes key) {
                    SweepWalletActivity.start(SendCoinsQrActivity.this, key);

                    SendCoinsQrActivity.this.finish();
                }

                @Override
                protected void handleDirectTransaction(final Transaction transaction) throws VerificationException {
                    final ApplicationLoader application = (ApplicationLoader) getApplication();
                    application.processDirectTransaction(transaction);

                    SendCoinsQrActivity.this.finish();
                }


                @Override
                protected void error(final int messageResId, final Object... messageArgs) {
                    dialog(SendCoinsQrActivity.this, dismissListener, 0, messageResId, messageArgs);
                }

                private final DialogInterface.OnClickListener dismissListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int which) {
                        SendCoinsQrActivity.this.finish();
                    }
                };
            }.parse();
        } else {
            finish();
        }
    }
}
