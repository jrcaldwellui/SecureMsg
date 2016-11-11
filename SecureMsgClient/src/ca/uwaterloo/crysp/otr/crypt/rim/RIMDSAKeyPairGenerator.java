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

import ca.uwaterloo.crysp.otr.crypt.KeyPair;
import ca.uwaterloo.crysp.otr.crypt.OTRCryptException;

import net.rim.device.api.crypto.DSACryptoSystem;
import net.rim.device.api.crypto.DSAKeyPair;

/**
 * Generates DSA KeyPairs via the RIM classes.
 * 
 */
public class RIMDSAKeyPairGenerator extends
        ca.uwaterloo.crysp.otr.crypt.DSAKeyPairGenerator {

    public KeyPair generateKeyPair() throws OTRCryptException {

        try {
            DSACryptoSystem dsacs = new DSACryptoSystem(DSACryptoSystem.SUN1024);
            DSAKeyPair dsakp = new DSAKeyPair(dsacs);
            // Wrap the RIM DSA Keys into the generic DSA keys
            return new KeyPair(new RIMDSAPrivateKey(dsakp.getDSAPrivateKey()),
                    new RIMDSAPublicKey(dsakp.getDSAPublicKey()));
        } catch (Exception e) {
            throw new OTRCryptException(e.getMessage());
        }
    }
}
