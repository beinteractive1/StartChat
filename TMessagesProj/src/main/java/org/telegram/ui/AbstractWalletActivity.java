package org.telegram.ui;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.R;

/**
 * Created by Max van de Wiel on 23-3-15.
 */
public abstract class AbstractWalletActivity extends Activity {
    private ApplicationLoader application;

    protected static final Logger log = LoggerFactory.getLogger(AbstractWalletActivity.class);

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        application = (ApplicationLoader) getApplication();

        super.onCreate(savedInstanceState);
    }

    protected ApplicationLoader getWalletApplication() {
        return application;
    }

    protected final void toast(@NonNull final String text, final Object... formatArgs) {
        toast(text, 0, Toast.LENGTH_SHORT, formatArgs);
    }

    protected final void longToast(@NonNull final String text, final Object... formatArgs) {
        toast(text, 0, Toast.LENGTH_LONG, formatArgs);
    }

    protected final void toast(@NonNull final String text, final int imageResId, final int duration, final Object... formatArgs) {
        final View view = getLayoutInflater().inflate(R.layout.transient_notification, null);
        TextView tv = (TextView) view.findViewById(R.id.transient_notification_text);
        tv.setText(String.format(text, formatArgs));
        tv.setCompoundDrawablesWithIntrinsicBounds(imageResId, 0, 0, 0);

        final Toast toast = new Toast(this);
        toast.setView(view);
        toast.setDuration(duration);
        toast.show();
    }

    protected final void toast(final int textResId, final Object... formatArgs) {
        toast(textResId, 0, Toast.LENGTH_SHORT, formatArgs);
    }

    protected final void longToast(final int textResId, final Object... formatArgs) {
        toast(textResId, 0, Toast.LENGTH_LONG, formatArgs);
    }

    protected final void toast(final int textResId, final int imageResId, final int duration, final Object... formatArgs) {
        final View view = getLayoutInflater().inflate(R.layout.transient_notification, null);
        TextView tv = (TextView) view.findViewById(R.id.transient_notification_text);
        tv.setText(getString(textResId, formatArgs));
        tv.setCompoundDrawablesWithIntrinsicBounds(imageResId, 0, 0, 0);

        final Toast toast = new Toast(this);
        toast.setView(view);
        toast.setDuration(duration);
        toast.show();
    }
}
