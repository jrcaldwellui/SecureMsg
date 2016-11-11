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

import ca.uwaterloo.crysp.otr.crypt.Key;
import ca.uwaterloo.crysp.otr.crypt.OTRCryptException;
import net.rim.device.api.crypto.HMAC;
import net.rim.device.api.crypto.SHA256Digest;

/**
 * The HMAC algorithm, as implemented by RIM
 * 
 * @author Can Tang <c24tang@gmail.com>
 */
public class RIMHMACSHA256 extends ca.uwaterloo.crysp.otr.crypt.HMAC {
    HMAC hmac;

    public RIMHMACSHA256() {
        super();
    }

    public void setKey(Key key) throws OTRCryptException {
        try {
            super.setKey(key);
            hmac = new HMAC(((RIMHMACKey) key).getHMACKey(), new SHA256Digest());
        } catch (net.rim.device.api.crypto.CryptoTokenException e) {
            throw new OTRCryptException(e.getMessage());
        } catch (net.rim.device.api.crypto.CryptoUnsupportedOperationException e) {
            throw new OTRCryptException(e.getMessage());
        }
    }

    public byte[] tag(byte[] data, int offset, int length)
            throws OTRCryptException {
        hmac.update(data, offset, length);
        return doFinal();
    }

    public void update(byte[] input) throws OTRCryptException {
        try {
            hmac.update(input);
        } catch (net.rim.device.api.crypto.CryptoTokenException e) {
            throw new OTRCryptException(e.getMessage());
        }

    }

    public void update(byte[] input, int inputOffset, int inputLen) {
        hmac.update(input, inputOffset, inputLen);
    }

    public byte[] doFinal() throws OTRCryptException {
        byte[] ret = new byte[hmac.getLength()];
        try {
            hmac.getMAC(ret, 0, true);
        } catch (net.rim.device.api.crypto.CryptoTokenException e) {
            throw new OTRCryptException(e.getMessage());
        }
        return ret;
    }

    public boolean verify(byte[] tag, byte[] data, int offset, int length)
            throws OTRCryptException {
        hmac.update(data, offset, length);

        byte[] realtag = doFinal();
        return ca.uwaterloo.crysp.otr.Util.arrayEquals(tag, realtag);
    }

}
