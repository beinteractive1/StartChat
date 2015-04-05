package org.telegram.ui;

import android.view.View;

import org.bitcoinj.core.Coin;
import org.bitcoinj.utils.ExchangeRate;
import org.bitcoinj.utils.Fiat;
import org.telegram.ui.Layouts.CurrencyAmountView;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Created by Max van de Wiel on 23-3-15.
 */
public final class CurrencyCalculatorLink {
    private final CurrencyAmountView btcAmountView;
    private final CurrencyAmountView localAmountView;

    private CurrencyAmountView.Listener listener = null;
    private boolean enabled = true;
    private ExchangeRate exchangeRate = null;
    private boolean exchangeDirection = true;

    private final CurrencyAmountView.Listener btcAmountViewListener = new CurrencyAmountView.Listener() {
        @Override
        public void changed() {
            if (btcAmountView.getAmount() != null)
                setExchangeDirection(true);
            else
                localAmountView.setHint(null);

            if (listener != null)
                listener.changed();
        }

        @Override
        public void focusChanged(final boolean hasFocus) {
            if (listener != null)
                listener.focusChanged(hasFocus);
        }
    };

    private final CurrencyAmountView.Listener localAmountViewListener = new CurrencyAmountView.Listener() {
        @Override
        public void changed() {
            if (localAmountView.getAmount() != null)
                setExchangeDirection(false);
            else
                btcAmountView.setHint(null);

            if (listener != null)
                listener.changed();
        }

        @Override
        public void focusChanged(final boolean hasFocus) {
            if (listener != null)
                listener.focusChanged(hasFocus);
        }
    };

    public CurrencyCalculatorLink(@Nonnull final CurrencyAmountView btcAmountView, @Nonnull final CurrencyAmountView localAmountView) {
        this.btcAmountView = btcAmountView;
        this.btcAmountView.setListener(btcAmountViewListener);

        this.localAmountView = localAmountView;
        this.localAmountView.setListener(localAmountViewListener);

        update();
    }

    public void setListener(@Nullable final CurrencyAmountView.Listener listener) {
        this.listener = listener;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;

        update();
    }

    public void setExchangeRate(@Nonnull final ExchangeRate exchangeRate) {
        this.exchangeRate = exchangeRate;

        update();
    }

    public ExchangeRate getExchangeRate() {
        return exchangeRate;
    }

    @CheckForNull
    public Coin getAmount() {
        if (exchangeDirection) {
            return (Coin) btcAmountView.getAmount();
        } else if (exchangeRate != null) {
            final Fiat localAmount = (Fiat) localAmountView.getAmount();
            try {
                return localAmount != null ? exchangeRate.fiatToCoin(localAmount) : null;
            } catch (ArithmeticException x) {
                return null;
            }
        } else {
            return null;
        }
    }

    public boolean hasAmount() {
        return getAmount() != null;
    }

    private void update() {
        btcAmountView.setEnabled(enabled);

        if (exchangeRate != null) {
            localAmountView.setEnabled(enabled);
            localAmountView.setCurrencySymbol(exchangeRate.fiat.currencyCode);

            if (exchangeDirection) {
                final Coin btcAmount = (Coin) btcAmountView.getAmount();
                if (btcAmount != null) {
                    localAmountView.setAmount(null, false);
                    localAmountView.setHint(exchangeRate.coinToFiat(btcAmount));
                    btcAmountView.setHint(null);
                }
            } else {
                final Fiat localAmount = (Fiat) localAmountView.getAmount();
                if (localAmount != null) {
                    localAmountView.setHint(null);
                    btcAmountView.setAmount(null, false);
                    try {
                        btcAmountView.setHint(exchangeRate.fiatToCoin(localAmount));
                    } catch (final ArithmeticException x) {
                        btcAmountView.setHint(null);
                    }
                }
            }
        } else {
            localAmountView.setEnabled(false);
            localAmountView.setHint(null);
            btcAmountView.setHint(null);
        }
    }

    public void setExchangeDirection(final boolean exchangeDirection) {
        this.exchangeDirection = exchangeDirection;

        update();
    }

    public boolean getExchangeDirection() {
        return exchangeDirection;
    }

    public View activeTextView() {
        if (exchangeDirection)
            return btcAmountView.getTextView();
        else
            return localAmountView.getTextView();
    }

    public void requestFocus() {
        activeTextView().requestFocus();
    }

    public void setBtcAmount(@Nonnull final Coin amount) {
        final CurrencyAmountView.Listener listener = this.listener;
        this.listener = null;

        btcAmountView.setAmount(amount, true);

        this.listener = listener;
    }

    public void setNextFocusId(final int nextFocusId) {
        btcAmountView.setNextFocusId(nextFocusId);
        localAmountView.setNextFocusId(nextFocusId);
    }
}
