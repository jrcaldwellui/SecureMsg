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

import net.rim.device.api.crypto.DSACryptoSystem;

/**
 * Abstract class for a DSA signing (private) or verification (public) key using
 * the RIM providers.
 * 
 */
public abstract class RIMDSAKey extends ca.uwaterloo.crysp.otr.crypt.DSAKey {
    /**
     * Returns the DSA parameters associated with the DSA key.
     * 
     * @return the DSA parameters associated with the DSA key.
     */
    public abstract DSACryptoSystem getDSAParams();

    /**
     * Returns the value of the DSA key.
     * 
     * @return the value of the DSA key.
     */
    public abstract String getValue();

    public byte[] getG() {
        try {
            byte[] result = getDSAParams().getG();
            return RIMMPI.toBytes(result);
        } catch (Exception e) {
        }
        return null;
    }

    public byte[] getP() {
        try {
            byte[] result = getDSAParams().getP();
            return RIMMPI.toBytes(result);
        } catch (Exception e) {
        }
        return null;
    }

    public byte[] getQ() {
        try {
            byte[] result = getDSAParams().getQ();
            return RIMMPI.toBytes(result);
        } catch (Exception e) {
        }
        return null;
    }

    public String toString() {
        return getValue();
    }
}
