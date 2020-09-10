/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.ethereum.beacon.discovery.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.tuweni.bytes.Bytes;
import org.ethereum.beacon.discovery.schema.NodeRecordFactory;
import org.ethereum.beacon.discovery.schema.NodeRecordInfo;
import org.ethereum.beacon.discovery.schema.NodeStatus;
import org.ethereum.beacon.discovery.util.RlpUtil;
import org.web3j.rlp.RlpEncoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpString;

/**
 * Storage for nodes, K-Bucket. Holds only {@link #K} nodes, replacing nodes with the same nodeId
 * and nodes with old lastRetry. Also throws out DEAD nodes without taking any notice on other
 * fields.
 */
public class NodeBucket {
  /** Bucket size, number of nodes */
  public static final int K = 16;

  private static final Predicate<NodeRecordInfo> FILTER =
      nodeRecord -> nodeRecord.getStatus().equals(NodeStatus.ACTIVE);
  private final TreeSet<NodeRecordInfo> bucket =
      new TreeSet<>((o1, o2) -> o2.getNode().hashCode() - o1.getNode().hashCode());

  public static NodeBucket fromRlpBytes(Bytes bytes, NodeRecordFactory nodeRecordFactory) {
    NodeBucket nodeBucket = new NodeBucket();
    RlpUtil.decodeListOfStrings(bytes)
        .stream()
        .map(bytes1 -> NodeRecordInfo.fromRlpBytes(bytes1, nodeRecordFactory))
        .forEach(nodeBucket::put);
    return nodeBucket;
  }

  public synchronized boolean put(NodeRecordInfo nodeRecord) {
    if (FILTER.test(nodeRecord)) {
      if (!bucket.contains(nodeRecord)) {
        boolean modified = bucket.add(nodeRecord);
        if (bucket.size() > K) {
          NodeRecordInfo worst = null;
          for (NodeRecordInfo nodeRecordInfo : bucket) {
            if (worst == null) {
              worst = nodeRecordInfo;
            } else if (worst.getLastRetry() > nodeRecordInfo.getLastRetry()) {
              worst = nodeRecordInfo;
            }
          }
          bucket.remove(worst);
        }
        return modified;
      } else {
        NodeRecordInfo bucketNode = bucket.subSet(nodeRecord, true, nodeRecord, true).first();
        if (nodeRecord.getLastRetry() > bucketNode.getLastRetry()) {
          bucket.remove(bucketNode);
          bucket.add(nodeRecord);
          return true;
        }
      }
    } else {
      return bucket.remove(nodeRecord);
    }

    return false;
  }

  public synchronized boolean contains(NodeRecordInfo nodeRecordInfo) {
    return bucket.contains(nodeRecordInfo);
  }

  public synchronized Bytes toRlpBytes() {
    byte[] res =
        RlpEncoder.encode(
            new RlpList(
                bucket.stream()
                    .map(NodeRecordInfo::toRlpBytes)
                    .map(Bytes::toArray)
                    .map(RlpString::create)
                    .collect(Collectors.toList())));
    return Bytes.wrap(res);
  }

  public int size() {
    return bucket.size();
  }

  public synchronized List<NodeRecordInfo> getNodeRecords() {
    return new ArrayList<>(bucket);
  }
}
