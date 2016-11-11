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
import ca.uwaterloo.crysp.otr.crypt.SecretKey;
import net.rim.device.api.crypto.AESEncryptorEngine;
import net.rim.device.api.crypto.AESKey;

/**
 * RIM-specific implementations of AES in Counter Mode. In particular, RIM is
 * used to encrypt the control block that is XORd with the blocks of
 * plaintext/ciphertext.
 * 
 * 
 */
public class RIMAESCTR extends ca.uwaterloo.crysp.otr.crypt.AESCTR {
    private AESEncryptorEngine cipher;

    public RIMAESCTR(SecretKey key, byte[] inputHigh) throws OTRCryptException {
        super(key, inputHigh);

        try {
            cipher = new AESEncryptorEngine(new AESKey(key.getEncoded()));
        } catch (Exception e) {
            throw new OTRCryptException(e);
        }
    }

    public void setKey(Key key) throws OTRCryptException {
        if (key instanceof SecretKey) {
            secretKey = (SecretKey) key;
            // Update cipher with the secret key
            try {
                cipher = new AESEncryptorEngine(new AESKey(secretKey
                        .getEncoded()));
            } catch (Exception e) {
                throw new OTRCryptException(e);
            }
        } else {
            throw new OTRCryptException("Wrong key type!");
        }
    }

    /**
     * Runs the control block through AES with the secret key.
     * 
     * @throws OTRCryptException
     * 
     * @param controlBlock
     *            The control block to send through AES.
     * 
     * @returns AES_k(controlBlock)
     */
    protected byte[] aesControlBlock(byte[] controlBlock)
            throws OTRCryptException {
        try {
            // Return AES_secretkey(controlBlock);
            byte[] res = new byte[controlBlock.length];
            cipher.encrypt(controlBlock, 0, res, 0);
            return res;
        } catch (Exception e) {
            throw new OTRCryptException(e);
        }
    }
}
