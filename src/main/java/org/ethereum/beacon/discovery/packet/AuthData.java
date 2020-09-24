package org.ethereum.beacon.discovery.packet;

import org.apache.tuweni.bytes.Bytes32;
import org.ethereum.beacon.discovery.packet.StaticHeader.Flag;
import org.ethereum.beacon.discovery.packet.impl.OrdinaryMessageImpl.AuthDataImpl;
import org.ethereum.beacon.discovery.type.Bytes12;

public interface AuthData extends BytesSerializable {

  static AuthData create(Bytes12 gcmNonce) {
    return new AuthDataImpl(gcmNonce);
  }

  static Header<AuthData> createHeader(Bytes32 srcNodeId, Bytes12 gcmNonce) {
    AuthData authData = create(gcmNonce);
    return Header.create(srcNodeId, Flag.MESSAGE, authData);
  }

  Bytes12 getAesGcmNonce();

  default boolean isEqual(AuthData other) {
    return getAesGcmNonce().equals(other.getAesGcmNonce());
  }
}
