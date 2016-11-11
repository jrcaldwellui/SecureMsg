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

import ca.uwaterloo.crysp.otr.crypt.OTRCryptException;

import net.rim.device.api.crypto.*;

/**
 * The SHA-1 hash algorithm, as implemented by RIM
 * 
 * @author Can Tang <c24tang@gmail.com>
 */
public class RIMSHA1 extends ca.uwaterloo.crysp.otr.crypt.SHA1 {
    SHA1Digest sha;

    public RIMSHA1() {
        super();
        sha = new SHA1Digest();

    }

    public byte[] hash() throws OTRCryptException {
        byte[] ret = new byte[sha.getDigestLength()];
        sha.getDigest(ret, 0, true);
        return ret;
    }

    public void update(byte[] data) {
        sha.update(data);
    }

    public void update(byte[] data, int offset, int length)
            throws OTRCryptException {
        sha.update(data, offset, length);
    }

    public byte[] hash(byte[] data) throws OTRCryptException {
        sha.update(data);
        byte[] ret = new byte[sha.getDigestLength()];
        sha.getDigest(ret, 0, true);
        return ret;
    }

    public byte[] hash(byte[] data, int offset, int length)
            throws OTRCryptException {
        sha.update(data, offset, length);
        byte[] ret = new byte[sha.getDigestLength()];
        sha.getDigest(ret, 0, true);
        return ret;
    }

    public boolean verify(byte[] digest, byte[] data) throws OTRCryptException {
        sha.update(data);
        byte[] ret = new byte[sha.getDigestLength()];
        sha.getDigest(ret, 0, true);
        return ca.uwaterloo.crysp.otr.Util.arrayEquals(ret, digest);
    }

    public boolean verify(byte[] digest, byte[] data, int offset, int length)
            throws OTRCryptException {
        sha.update(data, offset, length);
        byte[] ret = new byte[sha.getDigestLength()];
        sha.getDigest(ret, 0, true);
        return ca.uwaterloo.crysp.otr.Util.arrayEquals(ret, digest);
    }

    public String toString() {
        return sha.toString();
    }

    public static byte[] fromHex(byte[] msg) {
        byte[] ret = new byte[msg.length / 2];
        for (int i = 0; i < msg.length; i++) {
            if (msg[i] <= 57)
                msg[i] -= 48;
            else
                msg[i] -= 87;
            if (i % 2 == 0)
                ret[i / 2] += (msg[i] << 4);
            else
                ret[i / 2] += msg[i];
        }
        return ret;
    }

}
