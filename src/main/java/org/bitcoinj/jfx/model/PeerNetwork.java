package org.bitcoinj.jfx.model;

import org.bitcoinj.base.BitcoinNetwork;
import org.bitcoinj.base.ScriptType;
import org.bitcoinj.base.Sha256Hash;
import org.bitcoinj.core.AbstractBlockChain;
import org.bitcoinj.core.Block;
import org.bitcoinj.core.BlockChain;
import org.bitcoinj.core.FilteredBlock;
import org.bitcoinj.core.FullPrunedBlockChain;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Peer;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.ProtocolVersion;
import org.bitcoinj.core.Services;
import org.bitcoinj.core.VersionMessage;
import org.bitcoinj.core.listeners.BlockchainDownloadEventListener;
import org.bitcoinj.core.listeners.BlocksDownloadedEventListener;
import org.bitcoinj.core.listeners.PeerConnectedEventListener;
import org.bitcoinj.core.listeners.PeerDisconnectedEventListener;
import org.bitcoinj.net.discovery.DnsDiscovery;
import org.bitcoinj.store.BlockStoreException;
import org.bitcoinj.store.SPVBlockStore;
import org.bitcoinj.wallet.KeyChainGroup;
import org.bitcoinj.wallet.Wallet;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.bitcoinj.wallet.KeyChainGroupStructure.BIP43;

/**
 *
 */
public class PeerNetwork implements BlockchainDownloadEventListener, BlocksDownloadedEventListener, PeerConnectedEventListener, PeerDisconnectedEventListener {
    private static final Logger log = LoggerFactory.getLogger(PeerNetwork.class);
    private static final int initialDelay = 0;
    private static final int period = 10;

    private final NetworkModel networkModel;
    private final AbstractBlockChain blockChain;
    private final PeerGroup peerGroup;
    private final CompletableFuture<Void> groupStarted;
    private ScheduledExecutorService stpe;
    // Will never return normally, and will throw an exception upon task
    // cancellation or abnormal termination of a task execution. Use for cancelling
    // the ScheduledExecutorService
    private ScheduledFuture<?> scheduledFuture;

    public PeerNetwork(NetworkModel networkModel) {
        this.networkModel = networkModel;
        BitcoinNetwork network = networkModel.network();
        networkModel.setBlocks(0);
        networkModel.setHeaders(0);
        networkModel.setBestBlockHash(Sha256Hash.ZERO_HASH);

        //var blockStore = new MemoryFullPrunedBlockStore(NetworkParameters.of(network), 10);
        File dataDirectory = null;
        try {
            dataDirectory = Files.createTempDirectory("temp-wallet-dir").toFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        File file = new File(dataDirectory, "java-gi-ChainWatcher" + ".spvchain");
        SPVBlockStore blockStore;
        try {
            blockStore = new SPVBlockStore(NetworkParameters.of(network), file);
        } catch (BlockStoreException e) {
            throw new RuntimeException(e);
        }

        try {
            //blockChain = new FullPrunedBlockChain(NetworkParameters.of(network), blockStore);
            blockChain = new BlockChain(network, blockStore);
        } catch (BlockStoreException e) {
            throw new RuntimeException(e);
        }

        peerGroup = new PeerGroup(networkModel.network(), blockChain);
        peerGroup.addWallet(createWallet(network));
        peerGroup.setUserAgent("ChainWatcher", "0.1");
        peerGroup.addPeerDiscovery(new DnsDiscovery(network));

        //peerGroup.addBlocksDownloadedEventListener(this);
        peerGroup.addConnectedEventListener(this);
        peerGroup.addDisconnectedEventListener(this);

        // Start the PeerGroup (asynchronously) and start downloading the blockchain (asynchronously)
        groupStarted = peerGroup.startAsync().whenComplete((result, t) -> {
            if (t == null) {
                Objects.requireNonNull(peerGroup);
                peerGroup.startBlockChainDownload(this);
            } else {
                throw new RuntimeException(t);
            }
        });
        
        final int MAX_PEERS = 4;
        peerGroup.setMaxConnections(MAX_PEERS);
        networkModel.setMaxPeers(MAX_PEERS);

        stpe = Executors.newScheduledThreadPool(2);
        scheduledFuture = stpe.scheduleAtFixedRate(this::updateBlockHeight, initialDelay, period, TimeUnit.SECONDS);
    }

    public NetworkModel networkModel() {
        return networkModel;
    }

    private void updateBlockHeight() {
        int blocks = blockChain.getBestChainHeight();
        int newHeight = Math.max(peerGroup.getMostCommonChainHeight(), blocks);
        if (blocks != networkModel.blocks() || newHeight != networkModel.headers()) {
            log.warn("===== block height: {}", newHeight);
            networkModel.setHeaders(newHeight);
            networkModel.setBlocks(blocks);
            networkModel.setBestBlockHash(blockChain.getChainHead().getHeader().getHash());
        }
    }

    @Override
    public void onBlocksDownloaded(Peer peer, Block block, @Nullable FilteredBlock filteredBlock, int blocksLeft) {
        int newHeight = peerGroup.getMostCommonChainHeight();
        int blocks = Math.max(newHeight - blocksLeft, 0);
        if (blocks != networkModel.blocks() || newHeight != networkModel.headers()) {
            //log.warn("===== onBlocksDownloaded, block height: {}, blocksLeft: {}", newHeight, blocksLeft);
            networkModel.setHeaders(newHeight);
            networkModel.setBlocks(blocks);
            networkModel.setBestBlockHash(block.getHash());
        } else {
            //log.info("===== onBlocksDownloaded, blocksLeft: {}", blocksLeft);
        }
    }

    // Dummy wallet, so we can attach to a PeerGroup
    protected Wallet createWallet(BitcoinNetwork network) {
        KeyChainGroup kc = KeyChainGroup.builder(network, BIP43)
                .fromRandom(ScriptType.P2PKH)
                .build();
        return new Wallet(network, kc);
    }

    public void close() {
        stpe.shutdown();
        peerGroup.stopAsync();
        try {
            boolean terminated = stpe.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onPeerConnected(Peer peer, int peerCount) {
        log.warn("===== onPeerConnected, peerCount: {}", peerCount);
        networkModel.setPeerCount(peerCount);
        networkModel.setBloomPeers(countBloomPeers(peerGroup));
//        if (peerCount > 2 && !isBloomFilteringSupported(peer)) {
//            log.warn("===== Peer {} does not support Bloom Filters, closing.", peer.getAddress());
//            peer.close();
//        }
    }

    @Override
    public void onPeerDisconnected(Peer peer, int peerCount) {
        log.warn("===== onPeerDisconnected, peerCount: {}", peerCount);
        networkModel.setPeerCount(peerCount);
        networkModel.setBloomPeers(countBloomPeers(peerGroup));
    }

    int countBloomPeers(PeerGroup peerGroup) {
        List<Peer> peers = peerGroup.getConnectedPeers();
        return Math.toIntExact(peers.stream()
                .filter(this::isBloomFilteringSupported)
                .count());
    }

    private boolean isBloomFilteringSupported(Peer peer) {
        return isBloomFilteringSupported(peer.getVersionMessage());
    }

    private boolean isBloomFilteringSupported(VersionMessage version) {
        int clientVersion = version.clientVersion();
        return (clientVersion >= ProtocolVersion.BLOOM_FILTER.intValue()
                && clientVersion < ProtocolVersion.BLOOM_FILTER_BIP111.intValue())
                || version.services().has(Services.NODE_BLOOM);
    }                                                                                             

    @Override
    public void onChainDownloadStarted(Peer peer, int blocksLeft) {
        log.warn("++++++++++++++++++ onChainDownloadStarted, blocksLeft: {} ++++++++++++", blocksLeft);
    }
}
