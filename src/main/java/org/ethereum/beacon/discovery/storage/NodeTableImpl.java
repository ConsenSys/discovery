/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.ethereum.beacon.discovery.storage;

import com.google.common.annotations.VisibleForTesting;
import java.math.BigInteger;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.tuweni.bytes.Bytes;
import org.ethereum.beacon.discovery.database.DataSource;
import org.ethereum.beacon.discovery.database.HoleyList;
import org.ethereum.beacon.discovery.schema.NodeRecordInfo;
import org.ethereum.beacon.discovery.util.Functions;

/**
 * Stores Ethereum Node Records in {@link NodeRecordInfo} containers. Also stores home node as node
 * record. Uses indexes, {@link NodeIndex} for quick access to nodes that are close to others.
 */
public class NodeTableImpl implements NodeTable {
  static final long NUMBER_OF_INDEXES = 256;
  private static final int MAXIMUM_INFO_IN_ONE_BYTE = 256;
  private static final boolean START_FROM_BEGINNING = true;
  private final DataSource<Bytes, NodeRecordInfo> nodeTable;
  private final HoleyList<NodeIndex> indexTable;

  public NodeTableImpl(
      DataSource<Bytes, NodeRecordInfo> nodeTable, HoleyList<NodeIndex> indexTable) {
    this.nodeTable = nodeTable;
    this.indexTable = indexTable;
  }

  @VisibleForTesting
  static long getNodeIndex(Bytes nodeKey) {
    int activeBytes = 1;
    long required = NUMBER_OF_INDEXES;
    while (required > 0) {
      if (required == MAXIMUM_INFO_IN_ONE_BYTE) {
        required = 0;
      } else {
        required = required / MAXIMUM_INFO_IN_ONE_BYTE;
      }

      if (required > 0) {
        activeBytes++;
      }
    }

    int start = START_FROM_BEGINNING ? 0 : nodeKey.size() - activeBytes;
    Bytes active = nodeKey.slice(start, activeBytes);
    BigInteger activeNumber = new BigInteger(1, active.toArray());
    // XXX: could be optimized for small NUMBER_OF_INDEXES
    BigInteger index = activeNumber.mod(BigInteger.valueOf(NUMBER_OF_INDEXES));

    return index.longValue();
  }

  @Override
  public void save(NodeRecordInfo node) {
    Bytes nodeKey = node.getNode().getNodeId();
    nodeTable.put(nodeKey, node);
    NodeIndex activeIndex = indexTable.get(getNodeIndex(nodeKey)).orElseGet(NodeIndex::new);
    List<Bytes> nodes = activeIndex.getEntries();
    if (!nodes.contains(nodeKey)) {
      nodes.add(nodeKey);
      indexTable.put(getNodeIndex(nodeKey), activeIndex);
    }
  }

  @Override
  public void remove(NodeRecordInfo node) {
    Bytes nodeKey = node.getNode().getNodeId();
    nodeTable.remove(nodeKey);
    NodeIndex activeIndex = indexTable.get(getNodeIndex(nodeKey)).orElseGet(NodeIndex::new);
    List<Bytes> nodes = activeIndex.getEntries();
    if (nodes.contains(nodeKey)) {
      nodes.remove(nodeKey);
      indexTable.put(getNodeIndex(nodeKey), activeIndex);
    }
  }

  @Override
  public Optional<NodeRecordInfo> getNode(Bytes nodeId) {
    return nodeTable.get(nodeId);
  }

  @Override
  public Stream<NodeRecordInfo> streamClosestNodes(Bytes nodeId, int logLimit) {
    return StreamSupport.stream(
        Spliterators.spliteratorUnknownSize(
            new ClosestNodeIterator(nodeId, logLimit), Spliterator.ORDERED),
        false);
  }

  /**
   * Returns list of nodes including `nodeId` (if it's found) in logLimit distance from it. Uses
   * {@link Functions#logDistance(Bytes, Bytes)} as distance function. A logLimit of zero implies
   * finding all nodes from entries.
   */
  @Override
  public List<NodeRecordInfo> findClosestNodes(Bytes nodeId, int logLimit) {
    return streamClosestNodes(nodeId, logLimit).collect(Collectors.toList());
  }

  private class ClosestNodeIterator implements Iterator<NodeRecordInfo> {
    private final Bytes nodeId;
    private final int logLimit;
    private boolean limitReached = false;
    private long currentIndexUp;
    private long currentIndexDown;
    private Iterator<NodeRecordInfo> currentBatch = Collections.emptyIterator();

    public ClosestNodeIterator(final Bytes nodeId, final int logLimit) {
      this.nodeId = nodeId;
      this.logLimit = logLimit;
      final long start = getNodeIndex(nodeId);
      this.currentIndexUp = start;
      this.currentIndexDown = start;
    }

    @Override
    public boolean hasNext() {
      if (!currentBatch.hasNext()) {
        loadNextBatch();
      }
      return currentBatch.hasNext();
    }

    @Override
    public NodeRecordInfo next() {
      return currentBatch.next();
    }

    private void loadNextBatch() {
      Set<NodeRecordInfo> res = new HashSet<>();
      while (!limitReached && res.isEmpty()) {
        Optional<NodeIndex> upNodesOptional =
            currentIndexUp >= NUMBER_OF_INDEXES ? Optional.empty() : indexTable.get(currentIndexUp);
        Optional<NodeIndex> downNodesOptional =
            currentIndexDown < 0 ? Optional.empty() : indexTable.get(currentIndexDown);
        if (currentIndexUp >= NUMBER_OF_INDEXES && currentIndexDown < 0) {
          // Bounds are reached from both top and bottom
          break;
        }
        upNodesOptional.ifPresent(
            upNodes -> {
              for (Bytes currentNodeId : upNodes.getEntries()) {
                if (logLimit != 0 && Functions.logDistance(currentNodeId, nodeId) >= logLimit) {
                  limitReached = true;
                  break;
                } else {
                  getNode(currentNodeId).ifPresent(res::add);
                }
              }
            });
        downNodesOptional.ifPresent(
            downNodes -> {
              List<Bytes> entries = downNodes.getEntries();
              // XXX: iterate in reverse order to reach logDistance limit from the right side
              for (int i = entries.size() - 1; i >= 0; i--) {
                Bytes currentNodeId = entries.get(i);
                if (logLimit != 0 && Functions.logDistance(currentNodeId, nodeId) >= logLimit) {
                  limitReached = true;
                  break;
                } else {
                  getNode(currentNodeId).ifPresent(res::add);
                }
              }
            });
        currentIndexUp++;
        currentIndexDown--;
      }
      this.currentBatch = res.iterator();
    }
  }
}
