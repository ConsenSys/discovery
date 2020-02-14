/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.ethereum.beacon.discovery.network;

import java.net.InetSocketAddress;
import java.util.Optional;
import org.apache.tuweni.bytes.Bytes;
import org.ethereum.beacon.discovery.schema.NodeRecord;

/** Discovery client sends outgoing messages */
public interface DiscoveryClient {
  void stop();

  void send(Bytes data, NodeRecord recipient, Optional<InetSocketAddress> destination);
}
