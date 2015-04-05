package org.telegram.ui;

import android.content.Context;
import android.content.CursorLoader;
import android.content.SharedPreferences;

import org.telegram.bitcoin.Configuration;
import org.telegram.bitcoin.providers.ExchangeRatesProvider;

/**
 * Created by Max van de Wiel on 23-3-15.
 */
public final class ExchangeRateLoader extends CursorLoader implements SharedPreferences.OnSharedPreferenceChangeListener {
    private final Configuration config;

    public ExchangeRateLoader(final Context context, final Configuration config) {
        super(context, ExchangeRatesProvider.contentUri(context.getPackageName(), false), null, ExchangeRatesProvider.KEY_CURRENCY_CODE,
                new String[]{null}, null);

        this.config = config;
    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();

        config.registerOnSharedPreferenceChangeListener(this);

        onCurrencyChange();
    }

    @Override
    protected void onStopLoading() {
        config.unregisterOnSharedPreferenceChangeListener(this);

        super.onStopLoading();
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key) {
        if (Configuration.PREFS_KEY_EXCHANGE_CURRENCY.equals(key))
            onCurrencyChange();
    }

    private void onCurrencyChange() {
        final String exchangeCurrency = config.getExchangeCurrencyCode();

        setSelectionArgs(new String[]{exchangeCurrency});

        forceLoad();
    }
}
