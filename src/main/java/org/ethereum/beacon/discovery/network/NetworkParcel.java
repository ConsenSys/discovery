/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.ethereum.beacon.discovery.network;

import java.net.InetSocketAddress;
import org.ethereum.beacon.discovery.packet.Packet;
import org.ethereum.beacon.discovery.packet.RawPacket;

/**
 * Abstraction on the top of the {@link Packet}.
 *
 * <p>Stores `packet` and associated node record. Record could be a sender or recipient, depends on
 * session.
 */
public interface NetworkParcel {
  RawPacket getPacket();

  InetSocketAddress getDestination();
}
