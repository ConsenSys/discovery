/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.ethereum.beacon.discovery.schema;

/**
 * Fields of Ethereum Node Record V4 as defined by <a
 * href="https://eips.ethereum.org/EIPS/eip-778">https://eips.ethereum.org/EIPS/eip-778</a>
 */
public class EnrFieldV4 extends EnrField {
  // Compressed secp256k1 public key, 33 bytes
  public static final String PKEY_SECP256K1 = "secp256k1";

  public EnrFieldV4(final String name, final Object value) {
    super(name, value);
  }
}
