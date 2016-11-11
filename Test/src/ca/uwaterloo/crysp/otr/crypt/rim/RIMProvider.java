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
import ca.uwaterloo.crysp.otr.OTRException;
import net.rim.device.api.crypto.CryptoByteArrayArithmetic;

/**
 * The RIM crypto provider.
 * 
 * @author Can Tang <c24ang@cs.uwaterloo.ca>
 */
public class RIMProvider extends Provider {
    public RawDSA getRawDSA() {
        return new RIMRawDSA();
    }

    public DSAKeyPairGenerator getDSAKeyPairGenerator() {
        return new RIMDSAKeyPairGenerator();
    }

    public SHA1 getSHA1() {
        return new RIMSHA1();
    }

    public SHA256 getSHA256() {
        return new RIMSHA256();
    }

    public AESCTR getAESCounterMode(SecretKey key, byte[] ctrHigh)
            throws OTRCryptException {
        return new RIMAESCTR(key, ctrHigh);
    }

    public SecureRandom getSecureRandom() {
        return new RIMSecureRandom();
    }

    public DHKeyPairGenerator getDHKeyPairGenerator() {
        return new RIMDHKeyPairGenerator();
    }

    public AESKey getAESKey(byte[] r) {
        return new RIMAESKey(r);
    }

    public HMACKey getHMACKey(byte[] encoded) {
        return new RIMHMACKey(encoded);
    }

    public HMACKeyGenerator getHMACKeyGenerator() {
        return new RIMHMACKeyGenerator();
    }

    public HMAC getHMACSHA1() {
        return new RIMHMACSHA1();
    }

    public HMAC getHMACSHA256() {
        return new RIMHMACSHA256();
    }

    public DSAPublicKey getDSAPublicKey(MPI p, MPI q, MPI g, MPI y)
            throws Exception {
        return new RIMDSAPublicKey(p, q, g, y);
    }

    public DHPublicKey getDHPublicKey(MPI val) {
        return new RIMDHPublicKey(val);
    }

    public DHKeyAgreement getDHKeyAgreement() {
        return new RIMDHKeyAgreement();
    }

    public int compareMPI(MPI ours, MPI theirs) {
        byte[] ourdata = ours.getValue();
        byte[] theirdata = theirs.getValue();
        return net.rim.device.api.crypto.CryptoByteArrayArithmetic.compare(
                ourdata, theirdata);
    }
    
    public MPI powm(MPI base, MPI exp, MPI mod) throws OTRException { 
        byte[] res = new byte[mod.getValue().length];
        CryptoByteArrayArithmetic.exponent(base.getValue(), exp.getValue(),mod.getValue(), res);
        return new MPI(res);
    }
    
    public MPI mulm(MPI a, MPI b, MPI mod) throws OTRException {
        byte[] res = new byte[mod.getValue().length];
        CryptoByteArrayArithmetic.multiply(a.getValue(), b.getValue(),mod.getValue(), res);
        return new MPI(res);
    }
    
    public MPI subm(MPI a, MPI b, MPI mod) throws OTRException {
        byte[] res = new byte[mod.getValue().length];
        CryptoByteArrayArithmetic.subtract(a.getValue(), b.getValue(),mod.getValue(), res);
        return new MPI(res);
        }
    
    public MPI invm(MPI a, MPI mod) throws OTRException {
        byte[] res = new byte[mod.getValue().length];
        CryptoByteArrayArithmetic.invert(a.getValue(), mod.getValue(), res);
        return new MPI(res);
        }
}
