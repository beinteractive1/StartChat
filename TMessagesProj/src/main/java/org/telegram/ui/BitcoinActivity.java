package org.telegram.ui;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Dialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.Html;
import android.text.format.DateUtils;
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

import com.google.common.base.Charsets;

import org.bitcoinj.core.Wallet;
import org.bitcoinj.protocols.payments.PaymentProtocol;
import org.bitcoinj.store.WalletProtobufSerializer;
import org.bitcoinj.wallet.Protos;
import org.telegram.android.AndroidUtilities;
import org.telegram.android.LocaleController;
import org.telegram.android.NotificationCenter;
import org.telegram.bitcoin.Configuration;
import org.telegram.bitcoin.constants.DirectoryConstants;
import org.telegram.bitcoin.constants.FileConstants;
import org.telegram.bitcoin.constants.Network;
import org.telegram.bitcoin.constants.URLS;
import org.telegram.bitcoin.data.PaymentIntent;
import org.telegram.bitcoin.providers.ExchangeRatesProvider;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.utils.CrashReporter;
import org.telegram.utils.Crypto;
import org.telegram.utils.Io;
import org.telegram.utils.Iso8601Format;
import org.telegram.utils.Nfc;
import org.telegram.utils.WalletUtils;
import org.telegram.utils.WholeStringBuilder;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;

public class BitcoinActivity extends AbstractWalletActivity  implements NfcAdapter.CreateNdefMessageCallback {
    private static final int DIALOG_RESTORE_WALLET = 0;
    private static final int DIALOG_BACKUP_WALLET = 1;
    private static final int DIALOG_TIMESKEW_ALERT = 2;
    private static final int DIALOG_VERSION_ALERT = 3;
    private static final int DIALOG_LOW_STORAGE_ALERT = 4;

    private AbstractBindServiceActivity activity;
    private AtomicReference<byte[]> paymentRequestRef = new AtomicReference<byte[]>();

    private TextView emptyView;
    private View doneButton;
    private TextView doneButtonTextView;
    private TextView doneButtonBadgeTextView;

    private ApplicationLoader application;
    private Configuration config;
    private Wallet wallet;

    private Handler handler = new Handler();


    @Override
    protected void onCreate(final Bundle savedInstanceState)
    {
        setTheme(R.style.Theme_TMessages);
        super.onCreate(savedInstanceState);

        if (AndroidUtilities.isTablet()) {
            setContentView(R.layout.activity_bitcoin);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            setContentView(R.layout.activity_bitcoin);
        }

        setTitle("Send Coins");


        application = getWalletApplication();
        config = application.getBitCoinConfig();
        wallet = application.getWallet();

        setContentView(R.layout.activity_bitcoin);


        Wallet wallet = ApplicationLoader.getWallet();
        wallet.getBalance().getValue();
        // bitCoin Amount
        TextView textView = (TextView) findViewById(R.id.textView);
        textView.setText(wallet.getBalance().getValue() + " BTC");

        ExchangeRatesProvider exchangeRateProvider = new ExchangeRatesProvider();

        // euro Amount
        TextView textView2 = (TextView) findViewById(R.id.textView2);
        textView2.setText("(€ (0.01)");

        // Send button action
        Button sendCoinsButton = (Button) findViewById(R.id.send_coins_button);

        if (savedInstanceState == null)
            checkAlerts();

        config.touchLastUsed();

        handleIntent(getIntent());

        MaybeMaintenanceFragment.add(getFragmentManager());
    }

    @Override
    protected void onPause()
    {
        handler.removeCallbacksAndMessages(null);

        super.onPause();
    }

    @Override
    protected void onNewIntent(final Intent intent)
    {
        handleIntent(intent);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        handler.postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                // delayed start so that UI has enough time to initialize
                getWalletApplication().startBlockchainService(true);
            }
        }, 1000);

        checkLowStorageAlert();
    }

    private void checkLowStorageAlert()
    {
        final Intent stickyIntent = registerReceiver(null, new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW));
        if (stickyIntent != null)
            showDialog(DIALOG_LOW_STORAGE_ALERT);
    }

    /*@Override
    public View createView(LayoutInflater inflater, ViewGroup container)  {
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
            TextView textView = (TextView) fragmentView.findViewById(R.id.textView);
            textView.setText(wallet.getBalance().getValue() + " BTC");


            ExchangeRatesProvider exchangeRateProvider = new ExchangeRatesProvider();

            // euro Amount
            TextView textView2 = (TextView) fragmentView.findViewById(R.id.textView2);
            textView2.setText("(€ (0.01)");

            // Send button action
            Button sendCoinsButton = (Button) fragmentView.findViewById(R.id.send_coins_button);


        } else {
            ViewGroup parent = (ViewGroup) fragmentView.getParent();
            if (parent != null) {
                parent.removeView(fragmentView);
            }
        }
        return fragmentView;
    }*/

    private void handleIntent(@Nonnull final Intent intent)
    {
        final String action = intent.getAction();

        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action))
        {
            final String inputType = intent.getType();
            final NdefMessage ndefMessage = (NdefMessage) intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)[0];
            final byte[] input = Nfc.extractMimePayload(Network.MIMETYPE_TRANSACTION, ndefMessage);

            new InputParser.BinaryInputParser(inputType, input)
            {
                @Override
                protected void handlePaymentIntent(final PaymentIntent paymentIntent)
                {
                    cannotClassify(inputType);
                }

                @Override
                protected void error(final int messageResId, final Object... messageArgs)
                {
                    dialog(BitcoinActivity.this, null, 0, messageResId, messageArgs);
                }
            }.parse();
        }
    }

    @Override
    public NdefMessage createNdefMessage(final NfcEvent event) {
        final byte[] paymentRequest = paymentRequestRef.get();
        if (paymentRequest != null)
            return new NdefMessage(new NdefRecord[]{Nfc.createMime(PaymentProtocol.MIMETYPE_PAYMENTREQUEST, paymentRequest)});
        else
            return null;
    }

    private void checkAlerts()
    {
        /*final PackageInfo packageInfo = getWalletApplication().packageInfo();
        final int versionNameSplit = packageInfo.versionName.indexOf('-');
        final String base = Constants.VERSION_URL + (versionNameSplit >= 0 ? packageInfo.versionName.substring(versionNameSplit) : "");
        final String url = base + "?package=" + packageInfo.packageName + "&current=" + packageInfo.versionCode;

        new HttpGetThread(getAssets(), url, application.httpUserAgent())
        {
            @Override
            protected void handleLine(final String line, final long serverTime)
            {
                final int serverVersionCode = Integer.parseInt(line.split("\\s+")[0]);

                log.info("according to \"" + url + "\", strongly recommended minimum app version is " + serverVersionCode);

                if (serverTime > 0)
                {
                    final long diffMinutes = Math.abs((System.currentTimeMillis() - serverTime) / DateUtils.MINUTE_IN_MILLIS);

                    if (diffMinutes >= 60)
                    {
                        log.info("according to \"" + url + "\", system clock is off by " + diffMinutes + " minutes");

                        runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                final Bundle args = new Bundle();
                                args.putLong("diff_minutes", diffMinutes);
                                showDialog(DIALOG_TIMESKEW_ALERT, args);
                            }
                        });

                        return;
                    }
                }

                if (serverVersionCode > packageInfo.versionCode)
                {
                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            showDialog(DIALOG_VERSION_ALERT);
                        }
                    });

                    return;
                }
            }

            @Override
            protected void handleException(final Exception x)
            {
                if (x instanceof UnknownHostException || x instanceof SocketException || x instanceof SocketTimeoutException)
                {
                    // swallow
                    log.debug("problem reading", x);
                }
                else
                {
                    CrashReporter.saveBackgroundTrace(new RuntimeException(url, x), packageInfo);
                }
            }
        }.start();

        if (CrashReporter.hasSavedCrashTrace())
        {
            final StringBuilder stackTrace = new StringBuilder();

            try
            {
                CrashReporter.appendSavedCrashTrace(stackTrace);
            }
            catch (final IOException x)
            {
                log.info("problem appending crash info", x);
            }

            *//*final ReportIssueDialogBuilder dialog = new ReportIssueDialogBuilder(this, R.string.report_issue_dialog_title_crash,
                    R.string.report_issue_dialog_message_crash)
            {
                @Override
                protected CharSequence subject()
                {
                    return Constants.REPORT_SUBJECT_CRASH + " " + packageInfo.versionName;
                }

                @Override
                protected CharSequence collectApplicationInfo() throws IOException
                {
                    final StringBuilder applicationInfo = new StringBuilder();
                    CrashReporter.appendApplicationInfo(applicationInfo, application);
                    return applicationInfo;
                }

                @Override
                protected CharSequence collectStackTrace() throws IOException
                {
                    if (stackTrace.length() > 0)
                        return stackTrace;
                    else
                        return null;
                }

                @Override
                protected CharSequence collectDeviceInfo() throws IOException
                {
                    final StringBuilder deviceInfo = new StringBuilder();
                    CrashReporter.appendDeviceInfo(deviceInfo, WalletActivity.this);
                    return deviceInfo;
                }

                @Override
                protected CharSequence collectWalletDump()
                {
                    return wallet.toString(false, true, true, null);
                }
            };*//*

            dialog.show();
        }*/
    }

    private Dialog createTimeskewAlertDialog(final long diffMinutes)
    {
        final PackageManager pm = getPackageManager();
        final Intent settingsIntent = new Intent(android.provider.Settings.ACTION_DATE_SETTINGS);

        final DialogBuilder dialog = DialogBuilder.warn(this, R.string.wallet_timeskew_dialog_title);
        dialog.setMessage(getString(R.string.wallet_timeskew_dialog_msg, diffMinutes));

        if (pm.resolveActivity(settingsIntent, 0) != null)
        {
            dialog.setPositiveButton(R.string.button_settings, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(final DialogInterface dialog, final int id)
                {
                    startActivity(settingsIntent);
                    finish();
                }
            });
        }

        dialog.setNegativeButton(R.string.button_dismiss, null);
        return dialog.create();
    }

    private Dialog createVersionAlertDialog()
    {
        final PackageManager pm = getPackageManager();
        final Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(String.format(URLS.WEB_MARKET_APP_URL.url(), getPackageName())));
        final Intent binaryIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(URLS.BINARY_URL.url()));

        final DialogBuilder dialog = DialogBuilder.warn(this, R.string.wallet_version_dialog_title);
        final StringBuilder message = new StringBuilder(getString(R.string.wallet_version_dialog_msg));
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH)
            message.append("\n\n").append(getString(R.string.wallet_version_dialog_msg_deprecated));
        dialog.setMessage(message);

        if (pm.resolveActivity(marketIntent, 0) != null)
        {
            dialog.setPositiveButton(R.string.wallet_version_dialog_button_market, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(final DialogInterface dialog, final int id)
                {
                    startActivity(marketIntent);
                    finish();
                }
            });
        }

        if (pm.resolveActivity(binaryIntent, 0) != null)
        {
            dialog.setNeutralButton(R.string.wallet_version_dialog_button_binary, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(final DialogInterface dialog, final int id)
                {
                    startActivity(binaryIntent);
                    finish();
                }
            });
        }

        dialog.setNegativeButton(R.string.button_dismiss, null);
        return dialog.create();
    }

    private void restoreWalletFromEncrypted(@Nonnull final File file, @Nonnull final String password)
    {
        try
        {
            final BufferedReader cipherIn = new BufferedReader(new InputStreamReader(new FileInputStream(file), Charsets.UTF_8));
            final StringBuilder cipherText = new StringBuilder();
            Io.copy(cipherIn, cipherText, Io.BACKUP_MAX_CHARS);
            cipherIn.close();

            final byte[] plainText = Crypto.decryptBytes(cipherText.toString(), password.toCharArray());
            final InputStream is = new ByteArrayInputStream(plainText);

            restoreWallet(WalletUtils.restoreWalletFromProtobufOrBase58(is));

            log.info("successfully restored encrypted wallet: {}", file);
        }
        catch (final IOException x)
        {
            final DialogBuilder dialog = DialogBuilder.warn(this, R.string.import_export_keys_dialog_failure_title);
            dialog.setMessage(getString(R.string.import_keys_dialog_failure, x.getMessage()));
            dialog.setPositiveButton(R.string.button_dismiss, null);
            dialog.setNegativeButton(R.string.button_retry, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(final DialogInterface dialog, final int id)
                {
                    showDialog(DIALOG_RESTORE_WALLET);
                }
            });
            dialog.show();

            log.info("problem restoring wallet", x);
        }
    }

    private void restoreWalletFromProtobuf(@Nonnull final File file)
    {
        FileInputStream is = null;
        try
        {
            is = new FileInputStream(file);
            restoreWallet(WalletUtils.restoreWalletFromProtobuf(is));

            log.info("successfully restored unencrypted wallet: {}", file);
        }
        catch (final IOException x)
        {
            final DialogBuilder dialog = DialogBuilder.warn(this, R.string.import_export_keys_dialog_failure_title);
            dialog.setMessage(getString(R.string.import_keys_dialog_failure, x.getMessage()));
            dialog.setPositiveButton(R.string.button_dismiss, null);
            dialog.setNegativeButton(R.string.button_retry, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(final DialogInterface dialog, final int id)
                {
                    showDialog(DIALOG_RESTORE_WALLET);
                }
            });
            dialog.show();

            log.info("problem restoring wallet", x);
        }
        finally
        {
            try
            {
                if (is != null)
                    is.close();
            }
            catch (final IOException x2)
            {
                // swallow
            }
        }
    }

    private void restorePrivateKeysFromBase58(@Nonnull final File file)
    {
        FileInputStream is = null;
        try
        {
            is = new FileInputStream(file);
            restoreWallet(WalletUtils.restorePrivateKeysFromBase58(is));

            log.info("successfully restored unencrypted private keys: {}", file);
        }
        catch (final IOException x)
        {
            final DialogBuilder dialog = DialogBuilder.warn(this, R.string.import_export_keys_dialog_failure_title);
            dialog.setMessage(getString(R.string.import_keys_dialog_failure, x.getMessage()));
            dialog.setPositiveButton(R.string.button_dismiss, null);
            dialog.setNegativeButton(R.string.button_retry, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(final DialogInterface dialog, final int id)
                {
                    showDialog(DIALOG_RESTORE_WALLET);
                }
            });
            dialog.show();

            log.info("problem restoring private keys", x);
        }
        finally
        {
            try
            {
                if (is != null)
                    is.close();
            }
            catch (final IOException x2)
            {
                // swallow
            }
        }
    }

    private void restoreWallet(final Wallet wallet) throws IOException
    {
        application.replaceWallet(wallet);

        config.disarmBackupReminder();

        final DialogBuilder dialog = new DialogBuilder(this);
        final StringBuilder message = new StringBuilder();
        message.append(getString(R.string.restore_wallet_dialog_success));
        message.append("\n\n");
        message.append(getString(R.string.restore_wallet_dialog_success_replay));
        dialog.setMessage(message);
        dialog.setNeutralButton(R.string.button_ok, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(final DialogInterface dialog, final int id)
            {
                getWalletApplication().resetBlockchain();
                finish();
            }
        });
        dialog.show();
    }

    private void backupWallet(@Nonnull final String password)
    {
        DirectoryConstants.EXTERNAL_WALLET_BACKUP_DIR.directory().mkdir();
        final DateFormat dateFormat = Iso8601Format.newDateFormat();
        dateFormat.setTimeZone(TimeZone.getDefault());
        final File file = new File(DirectoryConstants.EXTERNAL_WALLET_BACKUP_DIR.directory(), FileConstants.EXTERNAL_WALLET_BACKUP.reference() + "-"
                + dateFormat.format(new Date()));

        final Protos.Wallet walletProto = new WalletProtobufSerializer().walletToProto(wallet);

        Writer cipherOut = null;

        try
        {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            walletProto.writeTo(baos);
            baos.close();
            final byte[] plainBytes = baos.toByteArray();

            cipherOut = new OutputStreamWriter(new FileOutputStream(file), Charsets.UTF_8);
            cipherOut.write(Crypto.encrypt(plainBytes, password.toCharArray()));
            cipherOut.flush();

            final DialogBuilder dialog = new DialogBuilder(this);
            dialog.setMessage(Html.fromHtml(getString(R.string.export_keys_dialog_success, file)));
            dialog.setPositiveButton(WholeStringBuilder.bold(getString(R.string.export_keys_dialog_button_archive)), new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(final DialogInterface dialog, final int which)
                {
                    archiveWalletBackup(file);
                }
            });
            dialog.setNegativeButton(R.string.button_dismiss, null);
            dialog.show();

            log.info("backed up wallet to: '" + file + "'");
        }
        catch (final IOException x)
        {
            final DialogBuilder dialog = DialogBuilder.warn(this, R.string.import_export_keys_dialog_failure_title);
            dialog.setMessage(getString(R.string.export_keys_dialog_failure, x.getMessage()));
            dialog.singleDismissButton(null);
            dialog.show();

            log.error("problem backing up wallet", x);
        }
        finally
        {
            try
            {
                cipherOut.close();
            }
            catch (final IOException x)
            {
                // swallow
            }
        }
    }

    private void archiveWalletBackup(@Nonnull final File file)
    {
        final Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.export_keys_dialog_mail_subject));
        intent.putExtra(Intent.EXTRA_TEXT,
                getString(R.string.export_keys_dialog_mail_text) + "\n\n" + String.format(URLS.WEB_MARKET_APP_URL.url(), getPackageName()) + "\n\n"
                        + URLS.SOURCE_URL.url() + '\n');
        intent.setType(Network.MIMETYPE_WALLET_BACKUP);
        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));

        try
        {
            startActivity(Intent.createChooser(intent, getString(R.string.export_keys_dialog_mail_intent_chooser)));
            log.info("invoked chooser for archiving wallet backup");
        }
        catch (final Exception x)
        {
            longToast(R.string.export_keys_dialog_mail_intent_failed);
            log.error("archiving wallet backup failed", x);
        }
    }
}

