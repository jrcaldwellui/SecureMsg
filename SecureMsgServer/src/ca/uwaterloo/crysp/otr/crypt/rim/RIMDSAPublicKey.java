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

import ca.uwaterloo.crysp.otr.crypt.*;
import net.rim.device.api.crypto.DSAPublicKey;
import net.rim.device.api.crypto.DSACryptoSystem;
import java.io.ByteArrayOutputStream;

/**
 * Wrapper class for the DSA verification (public) key using the RIM provider.
 * 
 */
public class RIMDSAPublicKey extends RIMDSAKey implements
        ca.uwaterloo.crysp.otr.crypt.DSAPublicKey {

    private DSAPublicKey pk;

    // Serialize buffer
    private byte[] ser;

    /**
     * Returns the RIM instance of the DSA public key.
     * 
     * @return the RIM instance of the DSA public key.
     */
    public DSAPublicKey getDSAPublicKey() {
        return pk;
    }

    public DSACryptoSystem getDSAParams() {
        return pk.getDSACryptoSystem();
    }

    public byte[] getY() {
        try {
            return RIMMPI.toBytes(pk.getPublicKeyData());
        } catch (Exception e) {
        }
        return null;
    }

    public String getValue() {
        return pk.toString();
    }

    public byte[] serialize() {
        return ser;
    }

    private void compute_serialization() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(RIMMPI.toBytes(pk.getDSACryptoSystem().getP()));
        baos.write(RIMMPI.toBytes(pk.getDSACryptoSystem().getQ()));
        baos.write(RIMMPI.toBytes(pk.getDSACryptoSystem().getG()));
        baos.write(RIMMPI.toBytes(pk.getPublicKeyData()));
        ser = baos.toByteArray();
    }

    public RIMDSAPublicKey(DSAPublicKey pk) throws Exception {
        this.pk = pk;
        compute_serialization();
    }

    public RIMDSAPublicKey(MPI p, MPI q, MPI g, MPI y) throws Exception {
        byte[] pp = RIMMPI.fromTrimmedBytes(p.getValue());
        byte[] qq = RIMMPI.fromTrimmedBytes(q.getValue());
        byte[] gg = RIMMPI.fromTrimmedBytes(g.getValue());
        byte[] yy = RIMMPI.fromTrimmedBytes(y.getValue());
        DSACryptoSystem dsacs = new DSACryptoSystem(pp, qq, gg);
        pk = new DSAPublicKey(dsacs, yy);
        compute_serialization();
    }
}
