package org.telegram.ui;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;

import org.bitcoinj.core.Wallet;
import org.telegram.android.AndroidUtilities;
import org.telegram.android.LocaleController;
import org.telegram.bitcoin.providers.ExchangeRatesProvider;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.BaseFragment;

public class BitcoinActivity extends BaseFragment {
        private TextView emptyView;
        private View doneButton;
        private TextView doneButtonTextView;
        private TextView doneButtonBadgeTextView;
        @Override
        public View createView(LayoutInflater inflater, ViewGroup container) {
            if (fragmentView == null) {
                actionBar.setBackgroundColor(0xff333333);
                actionBar.setItemsBackground(R.drawable.bar_selector_picker);
                actionBar.setBackButtonImage(R.drawable.ic_ab_back);
                actionBar.setTitle("Send Coins");

                actionBar.setActionBarMenuOnItemClick(new org.telegram.ui.ActionBar.ActionBar.ActionBarMenuOnItemClick() {
                    @Override
                    public void onItemClick(int id) {
                        if (id == -1) {
                                finishFragment();
                        } else if (id == 1) {
                                finishFragment(false);
                        }
                    }
                });

                ActionBarMenu menu = actionBar.createMenu();
                menu.addItem(1, R.drawable.ic_ab_other);

                fragmentView = inflater.inflate(R.layout.activity_bitcoin, container, false);

                Wallet wallet = ApplicationLoader.getWallet();
                wallet.getBalance().getValue();
                // bitCoin Amount
                TextView textView = (TextView)fragmentView.findViewById(R.id.textView);
                textView.setText(wallet.getBalance().getValue() + " BTC");


                ExchangeRatesProvider exchangeRateProvider = new ExchangeRatesProvider();

                // euro Amount
                TextView textView2 = (TextView)fragmentView.findViewById(R.id.textView2);
                textView2.setText("(€ (0.01)");

                // Send button action
                Button sendCoinsButton = (Button)fragmentView.findViewById(R.id.send_coins_button);



            } else {
                ViewGroup parent = (ViewGroup)fragmentView.getParent();
                if (parent != null) {
                    parent.removeView(fragmentView);
                }
            }
            return fragmentView;
        }
    }

