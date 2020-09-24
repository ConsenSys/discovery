/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.ethereum.beacon.discovery;

import java.util.concurrent.CompletableFuture;
import org.apache.tuweni.bytes.Bytes;
import org.ethereum.beacon.discovery.schema.NodeRecord;

/**
 * Discovery Manager, top interface for peer discovery mechanism as described at <a
 * href="https://github.com/ethereum/devp2p/blob/master/discv5/discv5.md">https://github.com/ethereum/devp2p/blob/master/discv5/discv5.md</a>
 */
public interface DiscoveryManager {

  interface TalkHandler {

    CompletableFuture<Bytes> talk(Bytes request);
  }

  CompletableFuture<Void> start();

  void stop();

  NodeRecord getLocalNodeRecord();

  void updateCustomFieldValue(final String fieldName, final Bytes value);

  /**
   * Initiates FINDNODE with node `nodeRecord`
   *
   * @param nodeRecord Ethereum Node record
   * @param distance Distance to search for
   * @return Future which is fired when reply is received or fails in timeout/not successful
   *     handshake/bad message exchange.
   */
  CompletableFuture<Void> findNodes(NodeRecord nodeRecord, int distance);

  /**
   * Initiates PING with node `nodeRecord`
   *
   * @param nodeRecord Ethereum Node record
   * @return Future which is fired when reply is received or fails in timeout/not successful
   *     handshake/bad message exchange.
   */
  CompletableFuture<Void> ping(NodeRecord nodeRecord);

  CompletableFuture<Bytes> talk(NodeRecord nodeRecord, String protocol, Bytes request);

  void addTalkHandler(String protocol, TalkHandler talkHandler);

  void removeTalkHandler(String protocol);
}
