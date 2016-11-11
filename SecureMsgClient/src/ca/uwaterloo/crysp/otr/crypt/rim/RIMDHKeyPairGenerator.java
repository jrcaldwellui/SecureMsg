/*
 *  Java OTR library
 *  Copyright (C) 2008-2009  Ian Goldberg, Muhaimeen Ashraf, Andrew Chung,
 *                           Can Tang
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of version 2.1 of the GNU Lesser General
 *  Public License as published by the Free Software Foundation.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package ca.uwaterloo.crysp.otr.crypt.rim;

import ca.uwaterloo.crysp.otr.OTRException;
import ca.uwaterloo.crysp.otr.crypt.KeyPair;
import ca.uwaterloo.crysp.otr.crypt.OTRCryptException;
import net.rim.device.api.crypto.DHKeyPair;
import net.rim.device.api.crypto.DHCryptoSystem;

/**
 * Generates a public/private keypair for the Diffie-Hellman key exchange. The
 * private key (x) is a randomly generated sequence of bits, and the public key
 * is computed using g<sup>x</sup> mod p. The parameters x, g, and p are the
 * constants DH_P, DH_G, DH_L defined in this class.
 * 
 */
public class RIMDHKeyPairGenerator extends
        ca.uwaterloo.crysp.otr.crypt.DHKeyPairGenerator {

    /**
     * 1536-bit MODP group
     */
    public static final byte[] DH_P = { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
            (byte) 0xC9, (byte) 0x0F, (byte) 0xDA, (byte) 0xA2, (byte) 0x21,
            (byte) 0x68, (byte) 0xC2, (byte) 0x34, (byte) 0xC4, (byte) 0xC6,
            (byte) 0x62, (byte) 0x8B, (byte) 0x80, (byte) 0xDC, (byte) 0x1C,
            (byte) 0xD1, (byte) 0x29, (byte) 0x02, (byte) 0x4E, (byte) 0x08,
            (byte) 0x8A, (byte) 0x67, (byte) 0xCC, (byte) 0x74, (byte) 0x02,
            (byte) 0x0B, (byte) 0xBE, (byte) 0xA6, (byte) 0x3B, (byte) 0x13,
            (byte) 0x9B, (byte) 0x22, (byte) 0x51, (byte) 0x4A, (byte) 0x08,
            (byte) 0x79, (byte) 0x8E, (byte) 0x34, (byte) 0x04, (byte) 0xDD,
            (byte) 0xEF, (byte) 0x95, (byte) 0x19, (byte) 0xB3, (byte) 0xCD,
            (byte) 0x3A, (byte) 0x43, (byte) 0x1B, (byte) 0x30, (byte) 0x2B,
            (byte) 0x0A, (byte) 0x6D, (byte) 0xF2, (byte) 0x5F, (byte) 0x14,
            (byte) 0x37, (byte) 0x4F, (byte) 0xE1, (byte) 0x35, (byte) 0x6D,
            (byte) 0x6D, (byte) 0x51, (byte) 0xC2, (byte) 0x45, (byte) 0xE4,
            (byte) 0x85, (byte) 0xB5, (byte) 0x76, (byte) 0x62, (byte) 0x5E,
            (byte) 0x7E, (byte) 0xC6, (byte) 0xF4, (byte) 0x4C, (byte) 0x42,
            (byte) 0xE9, (byte) 0xA6, (byte) 0x37, (byte) 0xED, (byte) 0x6B,
            (byte) 0x0B, (byte) 0xFF, (byte) 0x5C, (byte) 0xB6, (byte) 0xF4,
            (byte) 0x06, (byte) 0xB7, (byte) 0xED, (byte) 0xEE, (byte) 0x38,
            (byte) 0x6B, (byte) 0xFB, (byte) 0x5A, (byte) 0x89, (byte) 0x9F,
            (byte) 0xA5, (byte) 0xAE, (byte) 0x9F, (byte) 0x24, (byte) 0x11,
            (byte) 0x7C, (byte) 0x4B, (byte) 0x1F, (byte) 0xE6, (byte) 0x49,
            (byte) 0x28, (byte) 0x66, (byte) 0x51, (byte) 0xEC, (byte) 0xE4,
            (byte) 0x5B, (byte) 0x3D, (byte) 0xC2, (byte) 0x00, (byte) 0x7C,
            (byte) 0xB8, (byte) 0xA1, (byte) 0x63, (byte) 0xBF, (byte) 0x05,
            (byte) 0x98, (byte) 0xDA, (byte) 0x48, (byte) 0x36, (byte) 0x1C,
            (byte) 0x55, (byte) 0xD3, (byte) 0x9A, (byte) 0x69, (byte) 0x16,
            (byte) 0x3F, (byte) 0xA8, (byte) 0xFD, (byte) 0x24, (byte) 0xCF,
            (byte) 0x5F, (byte) 0x83, (byte) 0x65, (byte) 0x5D, (byte) 0x23,
            (byte) 0xDC, (byte) 0xA3, (byte) 0xAD, (byte) 0x96, (byte) 0x1C,
            (byte) 0x62, (byte) 0xF3, (byte) 0x56, (byte) 0x20, (byte) 0x85,
            (byte) 0x52, (byte) 0xBB, (byte) 0x9E, (byte) 0xD5, (byte) 0x29,
            (byte) 0x07, (byte) 0x70, (byte) 0x96, (byte) 0x96, (byte) 0x6D,
            (byte) 0x67, (byte) 0x0C, (byte) 0x35, (byte) 0x4E, (byte) 0x4A,
            (byte) 0xBC, (byte) 0x98, (byte) 0x04, (byte) 0xF1, (byte) 0x74,
            (byte) 0x6C, (byte) 0x08, (byte) 0xCA, (byte) 0x23, (byte) 0x73,
            (byte) 0x27, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF };

    /**
     * Generator for the DH_P group.
     */
    public static final byte[] DH_G = { (byte) 0x02 };

    public KeyPair generateKeyPair() throws OTRException {
        try {
            DHCryptoSystem dhcs = new DHCryptoSystem(DH_P, DH_G);
            DHKeyPair dhkp = new DHKeyPair(dhcs);
            // Wrap the RIM DH Keys into the generic DH keys
            RIMDHPrivateKey priv = new RIMDHPrivateKey(dhkp.getDHPrivateKey());
            RIMDHPublicKey pub = new RIMDHPublicKey(dhkp.getDHPublicKey());
            KeyPair kp = new KeyPair(priv, pub);
            return kp;
        } catch (Exception e) {
            throw new OTRException(e.toString());
        }
    }

}
