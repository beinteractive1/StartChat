package org.telegram.bitcoin.service;

import org.bitcoinj.core.Peer;
import org.bitcoinj.core.StoredBlock;

import java.util.List;

import javax.annotation.CheckForNull;

/**
 * Created by Max van de Wiel on 9-3-15.
 */
public interface BlockChainService
{
    public static final String ACTION_PEER_STATE = BlockChainService.class.getPackage().getName() + ".peer_state";
    public static final String ACTION_PEER_STATE_NUM_PEERS = "num_peers";

    public static final String ACTION_BLOCKCHAIN_STATE = BlockChainService.class.getPackage().getName() + ".blockchain_state";

    public static final String ACTION_CANCEL_COINS_RECEIVED = BlockChainService.class.getPackage().getName() + ".cancel_coins_received";
    public static final String ACTION_RESET_BLOCKCHAIN = BlockChainService.class.getPackage().getName() + ".reset_blockchain";
    public static final String ACTION_BROADCAST_TRANSACTION = BlockChainService.class.getPackage().getName() + ".broadcast_transaction";
    public static final String ACTION_BROADCAST_TRANSACTION_HASH = "hash";

    BlockchainState getBlockchainState();

    @CheckForNull
    List<Peer> getConnectedPeers();

    List<StoredBlock> getRecentBlocks(int maxBlocks);
}