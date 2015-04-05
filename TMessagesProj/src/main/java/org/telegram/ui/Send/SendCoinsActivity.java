package org.telegram.ui.Send;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import org.telegram.bitcoin.data.PaymentIntent;
import org.telegram.messenger.R;
import org.telegram.ui.AbstractBindServiceActivity;
import org.telegram.ui.HelpDialogFragment;

import org.telegram.messenger.R;

import javax.annotation.Nonnull;

/**
 * Created by Max van de Wiel on 23-3-15.
 */
public final class SendCoinsActivity extends AbstractBindServiceActivity {
    public static final String INTENT_EXTRA_PAYMENT_INTENT = "payment_intent";

    public static void start(final Context context, @Nonnull PaymentIntent paymentIntent) {
        final Intent intent = new Intent(context, SendCoinsActivity.class);
        intent.putExtra(INTENT_EXTRA_PAYMENT_INTENT, paymentIntent);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.send_coins_content);

        getWalletApplication().startBlockchainService(false);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.send_coins_activity_options, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;

            case R.id.send_coins_options_help:
                HelpDialogFragment.page(getFragmentManager(), R.string.help_send_coins);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
