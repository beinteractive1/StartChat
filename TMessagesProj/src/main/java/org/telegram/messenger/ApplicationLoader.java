/*
 * This is the source code of Telegram for Android v. 1.3.2.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013.
 */

package org.telegram.messenger;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.DateUtils;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.VerificationException;
import org.bitcoinj.core.VersionMessage;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.store.UnreadableWalletException;
import org.bitcoinj.store.WalletProtobufSerializer;
import org.bitcoinj.utils.Threading;
import org.bitcoinj.wallet.Protos;
import org.bitcoinj.wallet.WalletFiles;
import org.telegram.android.AndroidUtilities;
import org.telegram.android.ContactsController;
import org.telegram.android.MediaController;
import org.telegram.android.NotificationsService;
import org.telegram.android.SendMessagesHelper;
import org.telegram.android.LocaleController;
import org.telegram.android.MessagesController;
import org.telegram.android.NativeLoader;
import org.telegram.android.ScreenReceiver;
import org.telegram.bitcoin.constants.FileConstants;
import org.telegram.bitcoin.constants.Network;
import org.telegram.bitcoin.constants.TimeoutConstants;
import org.telegram.bitcoin.service.BlockChainService;
import org.telegram.bitcoin.service.BlockChainServiceImpl;
import org.telegram.utils.CrashReporter;
import org.telegram.utils.Io;
import org.telegram.utils.LinuxSecureRandom;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;

public class ApplicationLoader extends Application {
    private GoogleCloudMessaging gcm;
    private AtomicInteger msgId = new AtomicInteger();
    private String regid;
    public static final String EXTRA_MESSAGE = "message";
    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    public static Drawable cachedWallpaper = null;

    public static volatile Context applicationContext = null;
    public static volatile Handler applicationHandler = null;
    private static volatile boolean applicationInited = false;

    public static volatile boolean isScreenOn = false;
    public static volatile boolean mainInterfacePaused = true;

    private PackageInfo packageInfo;
    private static volatile org.telegram.bitcoin.Configuration bitCoinConfig;
    private ActivityManager activityManager;
    private File walletFile;
    private static Wallet wallet;
    private Intent blockchainServiceIntent;
    private Intent blockchainServiceCancelCoinsReceivedIntent;
    private Intent blockchainServiceResetBlockchainIntent;
    public static final String ACTION_WALLET_CHANGED = ApplicationLoader.class.getPackage().getName() + ".wallet_changed";

    public static void postInitApplication() {
        if (applicationInited) {
            return;
        }

        applicationInited = true;

        try {
            LocaleController.getInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            final IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            final BroadcastReceiver mReceiver = new ScreenReceiver();
            applicationContext.registerReceiver(mReceiver, filter);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            PowerManager pm = (PowerManager)ApplicationLoader.applicationContext.getSystemService(Context.POWER_SERVICE);
            isScreenOn = pm.isScreenOn();
            FileLog.e("tmessages", "screen state = " + isScreenOn);
        } catch (Exception e) {
            FileLog.e("tmessages", e);
        }

        UserConfig.loadConfig();
        if (UserConfig.getCurrentUser() != null) {
            MessagesController.getInstance().putUser(UserConfig.getCurrentUser(), true);
            ConnectionsManager.getInstance().applyCountryPortNumber(UserConfig.getCurrentUser().phone);
            ConnectionsManager.getInstance().initPushConnection();
            MessagesController.getInstance().getBlockedUsers(true);
            SendMessagesHelper.getInstance().checkUnsentMessages();
        }

        ApplicationLoader app = (ApplicationLoader)ApplicationLoader.applicationContext;
        app.initPlayServices();
        FileLog.e("tmessages", "app initied");

        ContactsController.getInstance().checkAppAccount();
        MediaController.getInstance();
    }

    @Override
    public void onCreate() {
        // MvdWiel
        new LinuxSecureRandom();
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectAll().permitDiskReads().permitDiskWrites().penaltyLog().build());
        packageInfo = packageInfoFromContext(this);
        Threading.throwOnLockCycles();

        StringBuffer buf = new StringBuffer();
        buf.append("Initiated bitcoin network parameters using configuration: ")
                .append(Network.TEST ? "test, " : "prod, ")
                .append(Network.NETWORK_PARAMETERS.getId());

        FileLog.e("tmessages", buf.toString());

        super.onCreate();

        if (Build.VERSION.SDK_INT < 13) {
            java.lang.System.setProperty("java.net.preferIPv4Stack", "true");
            java.lang.System.setProperty("java.net.preferIPv6Addresses", "false");
        }

        CrashReporter.init(getCacheDir());

        bitCoinConfig = new org.telegram.bitcoin.Configuration(PreferenceManager.getDefaultSharedPreferences(this));
        activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        Threading.uncaughtExceptionHandler = new Thread.UncaughtExceptionHandler()
        {
            @Override
            public void uncaughtException(final Thread thread, final Throwable throwable)
            {
                FileLog.e("tmessages", "uncaught exception", throwable);
                CrashReporter.saveBackgroundTrace(throwable, packageInfo);
            }
        };

        blockchainServiceIntent = new Intent(this, BlockChainServiceImpl.class);
        blockchainServiceCancelCoinsReceivedIntent = new Intent(BlockChainService.ACTION_CANCEL_COINS_RECEIVED, null, this,
                BlockChainServiceImpl.class);
        blockchainServiceResetBlockchainIntent = new Intent(BlockChainService.ACTION_RESET_BLOCKCHAIN, null, this, BlockChainServiceImpl.class);
        walletFile = getFileStreamPath(FileConstants.WALLET_FILENAME_PROTOBUF.reference());

        applicationContext = getApplicationContext();
        NativeLoader.initNativeLibs(ApplicationLoader.applicationContext);

        applicationHandler = new Handler(applicationContext.getMainLooper());

        startPushService();
    }

    public static Wallet getWallet()
    {
        return wallet;
    }

    public static void startPushService() {
        SharedPreferences preferences = applicationContext.getSharedPreferences("Notifications", MODE_PRIVATE);

        if (preferences.getBoolean("pushService", true)) {
            applicationContext.startService(new Intent(applicationContext, NotificationsService.class));

            if (android.os.Build.VERSION.SDK_INT >= 19) {
//                Calendar cal = Calendar.getInstance();
//                PendingIntent pintent = PendingIntent.getService(applicationContext, 0, new Intent(applicationContext, NotificationsService.class), 0);
//                AlarmManager alarm = (AlarmManager) applicationContext.getSystemService(Context.ALARM_SERVICE);
//                alarm.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), 30000, pintent);

                PendingIntent pintent = PendingIntent.getService(applicationContext, 0, new Intent(applicationContext, NotificationsService.class), 0);
                AlarmManager alarm = (AlarmManager)applicationContext.getSystemService(Context.ALARM_SERVICE);
                alarm.cancel(pintent);
            }
        } else {
            stopPushService();
        }
    }

    public static void stopPushService() {
        applicationContext.stopService(new Intent(applicationContext, NotificationsService.class));

        PendingIntent pintent = PendingIntent.getService(applicationContext, 0, new Intent(applicationContext, NotificationsService.class), 0);
        AlarmManager alarm = (AlarmManager)applicationContext.getSystemService(Context.ALARM_SERVICE);
        alarm.cancel(pintent);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        try {
            LocaleController.getInstance().onDeviceConfigurationChange(newConfig);
            AndroidUtilities.checkDisplaySize();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initPlayServices() {
        if (checkPlayServices()) {
            gcm = GoogleCloudMessaging.getInstance(this);
            regid = getRegistrationId();

            if (regid.length() == 0) {
                registerInBackground();
            } else {
                sendRegistrationIdToBackend(false);
            }
        } else {
            FileLog.d("tmessages", "No valid Google Play Services APK found.");
        }
    }

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        return resultCode == ConnectionResult.SUCCESS;
        /*if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this, PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i("tmessages", "This device is not supported.");
            }
            return false;
        }
        return true;*/
    }

    private String getRegistrationId() {
        final SharedPreferences prefs = getGCMPreferences(applicationContext);
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.length() == 0) {
            FileLog.d("tmessages", "Registration not found.");
            return "";
        }
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion();
        if (registeredVersion != currentVersion) {
            FileLog.d("tmessages", "App version changed.");
            return "";
        }
        return registrationId;
    }

    private SharedPreferences getGCMPreferences(Context context) {
        return getSharedPreferences(ApplicationLoader.class.getSimpleName(), Context.MODE_PRIVATE);
    }

    public static int getAppVersion() {
        try {
            PackageInfo packageInfo = applicationContext.getPackageManager().getPackageInfo(applicationContext.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    private void registerInBackground() {
        AsyncTask<String, String, Boolean> task = new AsyncTask<String, String, Boolean>() {
            @Override
            protected Boolean doInBackground(String... objects) {
                if (gcm == null) {
                    gcm = GoogleCloudMessaging.getInstance(applicationContext);
                }
                int count = 0;
                while (count < 1000) {
                    try {
                        count++;
                        regid = gcm.register(BuildVars.GCM_SENDER_ID);
                        sendRegistrationIdToBackend(true);
                        storeRegistrationId(applicationContext, regid);
                        return true;
                    } catch (Exception e) {
                        FileLog.e("tmessages", e);
                    }
                    try {
                        if (count % 20 == 0) {
                            Thread.sleep(60000 * 30);
                        } else {
                            Thread.sleep(5000);
                        }
                    } catch (InterruptedException e) {
                        FileLog.e("tmessages", e);
                    }
                }
                return false;
            }
        };

        if (android.os.Build.VERSION.SDK_INT >= 11) {
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null, null, null);
        } else {
            task.execute(null, null, null);
        }
    }

    private void sendRegistrationIdToBackend(final boolean isNew) {
        Utilities.stageQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                UserConfig.pushString = regid;
                UserConfig.registeredForPush = !isNew;
                UserConfig.saveConfig(false);
                if (UserConfig.getClientUserId() != 0) {
                    AndroidUtilities.runOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            MessagesController.getInstance().registerForPush(regid);
                        }
                    });
                }
            }
        });
    }

    private void storeRegistrationId(Context context, String regId) {
        final SharedPreferences prefs = getGCMPreferences(context);
        int appVersion = getAppVersion();
        FileLog.e("tmessages", "Saving regId on app version " + appVersion);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.commit();
    }

    public static String httpUserAgent(final String versionName)
    {
        final VersionMessage versionMessage = new VersionMessage(Network.NETWORK_PARAMETERS, 0);
        versionMessage.appendToSubVer(Network.USER_AGENT, versionName, null);
        return versionMessage.subVer;
    }

    public String httpUserAgent()
    {
        return httpUserAgent(packageInfo().versionName);
    }

    public PackageInfo packageInfo()
    {
        return packageInfo;
    }

    public org.telegram.bitcoin.Configuration getBitCoinConfig() {
        return this.bitCoinConfig;
    }

    public static PackageInfo packageInfoFromContext(final Context context)
    {
        try
        {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        }
        catch (final PackageManager.NameNotFoundException x)
        {
            throw new RuntimeException(x);
        }
    }

    private void loadWalletFromProtobuf()
    {
        if (walletFile.exists())
        {
            final long start = System.currentTimeMillis();

            FileInputStream walletStream = null;

            try
            {
                walletStream = new FileInputStream(walletFile);

                wallet = new WalletProtobufSerializer().readWallet(walletStream);

                if (!wallet.getParams().equals(Network.NETWORK_PARAMETERS))
                    throw new UnreadableWalletException("bad wallet network parameters: " + wallet.getParams().getId());

                FileLog.e("tmessages", "wallet loaded from: '" + walletFile + "', took " + (System.currentTimeMillis() - start) + "ms");
            }
            catch (final FileNotFoundException x)
            {
                FileLog.e("tmessages", "problem loading wallet", x);

                Toast.makeText(ApplicationLoader.this, x.getClass().getName(), Toast.LENGTH_LONG).show();

                wallet = restoreWalletFromBackup();
            }
            catch (final UnreadableWalletException x)
            {
                FileLog.e("tmessages", "problem loading wallet", x);

                Toast.makeText(ApplicationLoader.this, x.getClass().getName(), Toast.LENGTH_LONG).show();

                wallet = restoreWalletFromBackup();
            }
            finally
            {
                if (walletStream != null)
                {
                    try
                    {
                        walletStream.close();
                    }
                    catch (final IOException x)
                    {
                        // swallow
                    }
                }
            }

            if (!wallet.isConsistent())
            {
                Toast.makeText(this, "inconsistent wallet: " + walletFile, Toast.LENGTH_LONG).show();

                wallet = restoreWalletFromBackup();
            }

            if (!wallet.getParams().equals(Network.NETWORK_PARAMETERS))
                throw new Error("bad wallet network parameters: " + wallet.getParams().getId());
        }
        else
        {
            wallet = new Wallet(Network.NETWORK_PARAMETERS);

            backupWallet();

            getBitCoinConfig().armBackupReminder();

            FileLog.e("tmessages", "new wallet created");
        }
    }

    private Wallet restoreWalletFromBackup()
    {
        InputStream is = null;FileLog.e("tmessages", "wallet restored from backup: '" + FileConstants.WALLET_KEY_BACKUP_PROTOBUF.reference());

        try
        {
            is = openFileInput(FileConstants.WALLET_KEY_BACKUP_PROTOBUF.reference());

            final Wallet wallet = new WalletProtobufSerializer().readWallet(is);

            if (!wallet.isConsistent())
                throw new Error("inconsistent backup");

            resetBlockchain();

            Toast.makeText(this, R.string.toast_wallet_reset, Toast.LENGTH_LONG).show();

            FileLog.e("tmessages", "wallet restored from backup: '" + FileConstants.WALLET_KEY_BACKUP_PROTOBUF.reference() + "'");

            return wallet;
        }
        catch (final IOException x)
        {
            throw new Error("cannot read backup", x);
        }
        catch (final UnreadableWalletException x)
        {
            throw new Error("cannot read backup", x);
        }
        finally
        {
            try
            {
                is.close();
            }
            catch (final IOException x)
            {
                // swallow
            }
        }
    }

    public void saveWallet()
    {
        try
        {
            protobufSerializeWallet(wallet);
        }
        catch (final IOException x)
        {
            throw new RuntimeException(x);
        }
    }

    private void protobufSerializeWallet(@Nonnull final Wallet wallet) throws IOException
    {
        final long start = System.currentTimeMillis();

        wallet.saveToFile(walletFile);

        // make wallets world accessible in test mode
        if (Network.TEST)
            Io.chmod(walletFile, 0777);

        FileLog.e("tmessages", "wallet saved to: '" + walletFile + "', took " + (System.currentTimeMillis() - start) + "ms");
    }

    public void backupWallet()
    {
        final Protos.Wallet.Builder builder = new WalletProtobufSerializer().walletToProto(wallet).toBuilder();

        // strip redundant
        builder.clearTransaction();
        builder.clearLastSeenBlockHash();
        builder.setLastSeenBlockHeight(-1);
        builder.clearLastSeenBlockTimeSecs();
        final Protos.Wallet walletProto = builder.build();

        OutputStream os = null;

        try
        {
            os = openFileOutput(FileConstants.WALLET_KEY_BACKUP_PROTOBUF.reference(), Context.MODE_PRIVATE);
            walletProto.writeTo(os);
        }
        catch (final IOException x)
        {
            FileLog.e("tmessages", "problem writing key backup", x);
        }
        finally
        {
            try
            {
                os.close();
            }
            catch (final IOException x)
            {
                // swallow
            }
        }
    }

    private void migrateBackup()
    {
        if (!getFileStreamPath(FileConstants.WALLET_KEY_BACKUP_PROTOBUF.reference()).exists())
        {
            FileLog.e("tmessages", "migrating automatic backup to protobuf");

            // make sure there is at least one recent backup
            backupWallet();
        }
    }

    private void cleanupFiles()
    {
        for (final String filename : fileList())
        {
            if (filename.startsWith(FileConstants.WALLET_KEY_BACKUP_BASE58.reference())
                    || filename.startsWith(FileConstants.WALLET_KEY_BACKUP_PROTOBUF.reference() + '.') || filename.endsWith(".tmp"))
            {
                final File file = new File(getFilesDir(), filename);
                StringBuffer buf = new StringBuffer();
                buf.append("removing obsolete file: '").append(file).append("'");
                FileLog.e("tmessages", buf.toString());
                file.delete();
            }
        }
    }

    public void startBlockchainService(final boolean cancelCoinsReceived)
    {
        if (cancelCoinsReceived)
            startService(blockchainServiceCancelCoinsReceivedIntent);
        else
            startService(blockchainServiceIntent);
    }

    public void stopBlockchainService()
    {
        stopService(blockchainServiceIntent);
    }

    public void resetBlockchain()
    {
        internalResetBlockchain();

        final Intent broadcast = new Intent(ACTION_WALLET_CHANGED);
        broadcast.setPackage(getPackageName());
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
    }

    private void internalResetBlockchain()
    {
        // actually stops the service
        startService(blockchainServiceResetBlockchainIntent);
    }

    public void replaceWallet(final Wallet newWallet)
    {
        internalResetBlockchain(); // implicitly stops blockchain service
        wallet.shutdownAutosaveAndWait();

        wallet = newWallet;
        bitCoinConfig.maybeIncrementBestChainHeightEver(newWallet.getLastBlockSeenHeight());

        afterLoadWallet();

        final Intent broadcast = new Intent(ACTION_WALLET_CHANGED);
        broadcast.setPackage(getPackageName());
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
    }

    private void afterLoadWallet()
    {
        wallet.autosaveToFile(walletFile, 10, TimeUnit.SECONDS, new WalletAutosaveEventListener());

        // clean up spam
        wallet.cleanup();

        migrateBackup();
    }

    public void processDirectTransaction(@Nonnull final Transaction tx) throws VerificationException
    {
        if (wallet.isTransactionRelevant(tx))
        {
            wallet.receivePending(tx, null);
            broadcastTransaction(tx);
        }
    }

    public void broadcastTransaction(@Nonnull final Transaction tx)
    {
        final Intent intent = new Intent(BlockChainService.ACTION_BROADCAST_TRANSACTION, null, this, BlockChainServiceImpl.class);
        intent.putExtra(BlockChainService.ACTION_BROADCAST_TRANSACTION_HASH, tx.getHash().getBytes());
        startService(intent);
    }

    private static final class WalletAutosaveEventListener implements WalletFiles.Listener
    {
        @Override
        public void onBeforeAutoSave(final File file)
        {
        }

        @Override
        public void onAfterAutoSave(final File file)
        {
            // make wallets world accessible in test mode
            if (Network.TEST)
                Io.chmod(file, 0777);
        }
    }

    public final String applicationPackageFlavor()
    {
        final String packageName = getPackageName();
        final int index = packageName.lastIndexOf('_');

        if (index != -1)
            return packageName.substring(index + 1);
        else
            return null;
    }

    public int maxConnectedPeers()
    {
        final int memoryClass = activityManager.getMemoryClass();
        if (memoryClass <= Network.MEMORY_CLASS_LOWEND)
            return 4;
        else
            return 6;
    }

    public org.telegram.bitcoin.Configuration getBitCoinConfiguration() {
        return this.bitCoinConfig;
    }

    public static void scheduleStartBlockchainService(@Nonnull final Context context)
    {
        final org.telegram.bitcoin.Configuration config = new org.telegram.bitcoin.Configuration(PreferenceManager.getDefaultSharedPreferences(context));
        final long lastUsedAgo = config.getLastUsedAgo();

        // apply some backoff
        final long alarmInterval;
        if (lastUsedAgo < TimeoutConstants.LAST_USAGE_THRESHOLD_JUST_MS.timeout())
            alarmInterval = AlarmManager.INTERVAL_FIFTEEN_MINUTES;
        else if (lastUsedAgo < TimeoutConstants.LAST_USAGE_THRESHOLD_RECENTLY_MS.timeout())
            alarmInterval = AlarmManager.INTERVAL_HALF_DAY;
        else
            alarmInterval = AlarmManager.INTERVAL_DAY;

        FileLog.e("tmessages", "last used " + (lastUsedAgo / DateUtils.MINUTE_IN_MILLIS) + " minutes ago, rescheduling blockchain sync in roughly " + alarmInterval / DateUtils.MINUTE_IN_MILLIS + " minutes");

        final AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        final PendingIntent alarmIntent = PendingIntent.getService(context, 0, new Intent(context, BlockChainServiceImpl.class), 0);
        alarmManager.cancel(alarmIntent);

        // workaround for no inexact set() before KitKat
        final long now = System.currentTimeMillis();
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, now + alarmInterval, AlarmManager.INTERVAL_DAY, alarmIntent);
    }
}
