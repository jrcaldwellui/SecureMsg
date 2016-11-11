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

import net.rim.device.api.crypto.DHCryptoSystem;

/**
 * Abstract DH key using the RIM provider. The g, l and p parameters as well as
 * the actual key values are represented using DHCryptoSystem.
 */
public abstract class RIMDHKey extends ca.uwaterloo.crysp.otr.crypt.DHKey {
    private DHCryptoSystem dhcs;

    public RIMDHKey(byte[] g, byte[] p) {
        try {
            dhcs = new DHCryptoSystem(p, g, 320);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public byte[] getG() {
        try {
            return RIMMPI.toBytes(dhcs.getG());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public byte[] getL() {
        try {
            return RIMMPI.toBytes(dhcs.getQ());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public byte[] getP() {
        try {
            return RIMMPI.toBytes(dhcs.getP());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public DHCryptoSystem getDHCS() {
        return dhcs;
    }
}
