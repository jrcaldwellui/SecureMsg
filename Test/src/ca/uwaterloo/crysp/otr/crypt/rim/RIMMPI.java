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

/**
 * 
 */
class RIMMPI extends ca.uwaterloo.crysp.otr.crypt.MPI {
    RIMMPI(byte[] value) {
        super(value);
    }

    public static byte[] toBytes(byte[] data) {
        int len = data.length;
        byte[] n = new byte[len + 4];
        n[0] = (byte) ((len >> 24) & 0xff);
        n[1] = (byte) ((len >> 16) & 0xff);
        n[2] = (byte) ((len >> 8) & 0xff);
        n[3] = (byte) (len & 0xff);
        System.arraycopy(data, 0, n, 4, len);
        return n;
    }

    public static byte[] fromBytes(byte[] data) {
        int len = data.length;
        byte[] n = new byte[len - 4];
        System.arraycopy(data, 4, n, 0, len - 4);
        return n;
    }
    
    public static byte[] fromTrimmedBytes(byte[] data) {
        int len = data.length;
        byte[] n = new byte[len];
        System.arraycopy(data, 0, n, 0, len);
        return n;
    }


}
