package org.telegram.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import org.telegram.bitcoin.service.BlockChainService;
import org.telegram.bitcoin.service.BlockChainServiceImpl;

import javax.annotation.CheckForNull;

/**
 * Created by Max van de Wiel on 23-3-15.
 */
public abstract class AbstractBindServiceActivity extends AbstractWalletActivity {
    @CheckForNull
    private BlockChainService blockchainService;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(final ComponentName name, final IBinder binder) {
            blockchainService = ((BlockChainServiceImpl.LocalBinder) binder).getService();
        }

        @Override
        public void onServiceDisconnected(final ComponentName name) {
            blockchainService = null;
        }
    };

    @Override
    protected void onResume() {
        super.onResume();

        bindService(new Intent(this, BlockChainServiceImpl.class), serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        unbindService(serviceConnection);

        super.onPause();
    }

    protected BlockChainService getBlockchainService() {
        return blockchainService;
    }
}
