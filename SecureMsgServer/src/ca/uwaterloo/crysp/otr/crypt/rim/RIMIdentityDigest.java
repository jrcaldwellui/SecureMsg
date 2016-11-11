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

import java.io.ByteArrayOutputStream;
import net.rim.device.api.crypto.Digest;

/**
 * 
 */
class RIMIdentityDigest implements Digest {
    private int size;
    private ByteArrayOutputStream baos;

    RIMIdentityDigest(int size) {
        this.size = size;
        this.baos = new ByteArrayOutputStream(size);
    }

    public String getAlgorithm() {
        return "Identity";
    }

    public int getBlockLength() {
        return size;
    }

    public byte[] getDigest() {
        return getDigest(false);
    }

    public byte[] getDigest(boolean resetDigest) {
        byte[] res = baos.toByteArray();
        if (resetDigest) {
            reset();
        }
        return res;
    }

    public int getDigest(byte[] buffer, int offset) {
        return getDigest(buffer, offset, false);
    }

    public int getDigest(byte[] buffer, int offset, boolean resetDigest) {
        byte[] res = baos.toByteArray();
        System.arraycopy(res, 0, buffer, offset, size);
        if (resetDigest) {
            reset();
        }
        return size;
    }

    public int getDigestLength() {
        return size;
    }

    public void reset() {
        baos = new ByteArrayOutputStream(size);
    }

    public void update(byte[] data) {
        update(data, 0, data.length);
    }

    public void update(byte[] data, int offset, int length) {
        baos.write(data, offset, length);
    }

    public void update(int data) {
        baos.write(data);
    }
}
