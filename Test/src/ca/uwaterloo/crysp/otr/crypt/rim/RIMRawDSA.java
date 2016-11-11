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
import net.rim.device.api.crypto.DSASignatureSigner;
import net.rim.device.api.crypto.DSASignatureVerifier;
import net.rim.device.api.crypto.CryptoByteArrayArithmetic;

/**
 * The DSA digital signature algorithm, as implemented by RIM
 * 
 */
public class RIMRawDSA extends ca.uwaterloo.crysp.otr.crypt.RawDSA {

    public RIMRawDSA() {
        super();
    }

    public byte[] sign(PrivateKey priv, byte[] data) throws OTRCryptException {
        try {
            Sig ss = new Sig();
            ss.initSign((RIMDSAPrivateKey) priv);
            byte[] ret = ss.sign(data);
            return ret;
        } catch (Exception e) {
            // System.out.println("Sign failed: " + e.getMessage());
            throw new OTRCryptException(e);
        }
    }

    public boolean verify(PublicKey pub, byte[] signature, byte[] data)
            throws OTRCryptException {
        try {
            Sig sv = new Sig();
            sv.initVerify((RIMDSAPublicKey) pub);
            return sv.verify(signature, data);
        } catch (Exception e) {
            throw new OTRCryptException(e);
        }
    }

    class Sig {
        RIMDSAPrivateKey priv;
        RIMDSAPublicKey pub;

        void initSign(RIMDSAPrivateKey p) {
            priv = p;
        }

        void initVerify(RIMDSAPublicKey p) {
            pub = p;
        }

        byte[] sign(byte[] data) throws OTRCryptException {
            byte[] p = RIMMPI.fromBytes(priv.getP());
            byte[] q = RIMMPI.fromBytes(priv.getQ());
            byte[] g = RIMMPI.fromBytes(priv.getG());
            byte[] x = RIMMPI.fromBytes(priv.getX());

            byte[] m = data;
            try {
                byte[] k = new byte[q.length];
                new RIMSecureRandom().nextBytes(k);
                while (CryptoByteArrayArithmetic.compare(k, q) > 0) {
                    new RIMSecureRandom().nextBytes(k);
                }
                byte[] res = new byte[p.length];
                CryptoByteArrayArithmetic.exponent(g, k, p, res);
                byte[] r = new byte[q.length];
                CryptoByteArrayArithmetic.mod(res, q, r);

                byte[] inv = new byte[q.length];
                CryptoByteArrayArithmetic.invert(k, q, inv);
                byte[] prod = new byte[q.length];
                CryptoByteArrayArithmetic.multiply(x, r, q, prod);
                byte[] sum = new byte[q.length];
                CryptoByteArrayArithmetic.add(m, prod, q, sum);

                byte[] s = new byte[q.length];
                CryptoByteArrayArithmetic.multiply(inv, sum, q, s);

                int rstart = 0, sstart = 0;
                while (r[rstart] == 0) {
                    rstart++;
                }
                while (s[sstart] == 0) {
                    sstart++;
                }
                byte[] ret = new byte[40];

                System.arraycopy(r, rstart, ret, 20 - (r.length - rstart),
                        (r.length - rstart));
                System.arraycopy(s, sstart, ret, 40 - (s.length - sstart),
                        (s.length - sstart));

                return ret;

            } catch (Exception e) {
                throw new OTRCryptException(e);
            }

        }

        boolean verify(byte[] signature, byte[] data) throws OTRCryptException {
            byte[] p = RIMMPI.fromBytes(pub.getP());
            byte[] q = RIMMPI.fromBytes(pub.getQ());
            byte[] g = RIMMPI.fromBytes(pub.getG());
            byte[] y = RIMMPI.fromBytes(pub.getY());

            byte[] m = data;
            byte[] r = new byte[20];
            byte[] s = new byte[20];
            System.arraycopy(signature, 0, r, 0, 20);
            System.arraycopy(signature, 20, s, 0, 20);

            byte[] zero = { 0 };
            if (CryptoByteArrayArithmetic.compare(r, q) >= 0
                    || CryptoByteArrayArithmetic.compare(s, q) >= 0
                    || CryptoByteArrayArithmetic.compare(r, zero) == 0
                    || CryptoByteArrayArithmetic.compare(s, zero) == 0)
                return false;

            byte[] w = new byte[q.length];
            CryptoByteArrayArithmetic.invert(s, q, w);

            byte[] u1 = new byte[q.length];
            CryptoByteArrayArithmetic.multiply(m, w, q, u1);
            byte[] u2 = new byte[q.length];
            CryptoByteArrayArithmetic.multiply(r, w, q, u2);

            byte[] p1 = new byte[p.length];
            byte[] p2 = new byte[p.length];
            CryptoByteArrayArithmetic.exponent(g, u1, p, p1);
            CryptoByteArrayArithmetic.exponent(y, u2, p, p2);

            byte[] m1 = new byte[p.length];
            byte[] m2 = new byte[q.length];
            CryptoByteArrayArithmetic.multiply(p1, p2, p, m1);
            CryptoByteArrayArithmetic.mod(m1, q, m2);

            return CryptoByteArrayArithmetic.compare(m2, r) == 0;
        }

    }

}
