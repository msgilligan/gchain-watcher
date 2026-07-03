/*
 * Copyright 2026 M. Sean Gilligan.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.bitcoinj.jfx.model;

import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import org.bitcoinj.base.BitcoinNetwork;
import org.bitcoinj.base.Sha256Hash;

/// A JavaFX Model of a live Bitcoin Network.
public final class NetworkModel {
    private final BitcoinNetwork network;
    private final ReadOnlyIntegerWrapper blocks = new ReadOnlyIntegerWrapper( this, "blocks", 0);
    private final ReadOnlyIntegerWrapper headers = new ReadOnlyIntegerWrapper(this, "headers", 0);
    private final ReadOnlyIntegerWrapper peerCount = new ReadOnlyIntegerWrapper(this, "peerCount", -1);
    private final ReadOnlyIntegerWrapper bloomCount = new ReadOnlyIntegerWrapper(this, "bloomCount", -1);
    private final ReadOnlyIntegerWrapper maxPeers = new ReadOnlyIntegerWrapper(this, "maxPeers", -1);
    private final ReadOnlyObjectWrapper<Sha256Hash> bestBlockHash = new ReadOnlyObjectWrapper<>(this, "bestBlockHash", Sha256Hash.ZERO_HASH);

    public NetworkModel(BitcoinNetwork network) {
        this.network = network;
    }

    public BitcoinNetwork network() {
        return network;
    }

    public int blocks() {
        return blocks.get();
    }

    public int headers() {
        return headers.get();
    }

    public int peerCount() {
        return peerCount.get();
    }

    public int bloomCount() {
        return bloomCount.get();
    }

    public int maxPeers() {
        return maxPeers.get();
    }

    public Sha256Hash bestBlockHash() {
        return bestBlockHash.get();
    }

    public ReadOnlyIntegerProperty blocksProperty() {
        return blocks.getReadOnlyProperty();
    }

    public ReadOnlyIntegerProperty headersProperty() {
        return headers.getReadOnlyProperty();
    }

    public ReadOnlyObjectProperty<Sha256Hash> bestBlockHashProperty() {
        return bestBlockHash.getReadOnlyProperty();
    }

    public ReadOnlyIntegerProperty peerCountProperty() {
        return peerCount.getReadOnlyProperty();
    }

    public ReadOnlyIntegerProperty bloomCountProperty() {
        return bloomCount.getReadOnlyProperty();
    }

    public ReadOnlyIntegerProperty maxPeersProperty() {
        return maxPeers.getReadOnlyProperty();  
    }

    // Package-private mutators for model owner
    void setBlocks(int value)               { blocks.set(value); }
    void setHeaders(int value)              { headers.set(value); }
    void setBestBlockHash(Sha256Hash value) { bestBlockHash.set(value); }
    void setPeerCount(int value)            { peerCount.set(value); }
    void setBloomPeers(int value)           { bloomCount.set(value); }
    void setMaxPeers(int value)             { maxPeers.set(value); }
}
