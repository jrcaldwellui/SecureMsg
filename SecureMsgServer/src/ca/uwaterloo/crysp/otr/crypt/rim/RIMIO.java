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

import javax.microedition.io.*;
import java.io.*;

public class RIMIO {
        public static byte[] readLine(InputStream is) throws Exception {
                byte[] res = new byte[1024];
                int num_read = 0;
                byte b = (byte) is.read();
                int nblock = 0;
                byte[] tmp;
                while (b != '\n') {
                        if (b == '\r') {
                                b = (byte) is.read();
                                continue;
                        }
                        res[num_read] = b;
                        num_read++;
                        if (num_read % 1024 == 0) {
                                tmp = res;
                                res = new byte[num_read + 1024];
                                System.arraycopy(tmp, 0, res, 0, num_read);
                                nblock++;
                        }
                        b = (byte) is.read();
                }
                tmp = res;
                res = new byte[num_read];
                System.arraycopy(tmp, 0, res, 0, num_read);
                return res;
        }

        public static void writeLine(OutputStream os, byte[] content)
                        throws Exception {
                byte[] res = new byte[content.length+1];
                System.arraycopy(content,0,res,0,content.length);
                res[content.length]='\n';
                //os.write('\r');
                os.write(res);
        }
}
