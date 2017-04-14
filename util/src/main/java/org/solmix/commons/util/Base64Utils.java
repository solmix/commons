package org.solmix.commons.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Copyed from eclipse.
 * @author solmix.f@gmail.com
 * @version $Id$  2014年4月4日
 */
public class Base64Utils {

    private static final Logger LOG = LoggerFactory.getLogger(Base64Utils.class);
    // base 64 character set
    //
    private static final char[] BCS = {
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 
        'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 
        'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 
        'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 
        'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 
        'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', 
        '8', '9', '+', '/'
    };

    private static final char[] BCS_URL_SAFE = Arrays.copyOf(BCS, BCS.length);
    
    // base 64 padding
    private static final char PAD = '=';

    // size of base 64 decode table
    private static final int BDTSIZE = 128;

    // base 64 decode table  
    private static final byte[] BDT = new byte[128];

    
    private static final int PAD_SIZE0 = 1;
    private static final int PAD_SIZE4 = 2;
    private static final int PAD_SIZE8 = 3;
    
    private static final Charset CHARSET_UTF8 = Charset.forName("UTF-8");
    
    // class static initializer for building decode table
    static {
        for (int i = 0;  i < BDTSIZE;  i++) {
            BDT[i] = Byte.MAX_VALUE;
        }

        for (int i = 0;  i < BCS.length;  i++) {
            BDT[BCS[i]] = (byte)i;
        }
        
        BCS_URL_SAFE[62] = '-';
        BCS_URL_SAFE[63] = '_';
    }
    
    
    Base64Utils() {
    }
    
    
    
    /**
     * The <code>decode_chunk</code> routine decodes a chunk of data
     * into its native encoding.
     *
     * base64 encodes each 3 octets of data into 4 characters from a
     * limited 64 character set. The 3 octets are joined to form
     * 24 bits which are then split into 4 x 6bit values. Each 6 bit
     * value is then used as an index into the 64 character table of
     * base64 chars. If the total data length is not a 3 octet multiple
     * the '=' char is used as padding for the final 4 char group, 
     * either 1 octet + '==' or 2 octets + '='.
     *
     * @param   id  The input data to be processed
     * @param   o   The offset from which to begin processing
     * @param   l   The length (bound) at which processing is to end
     * @return  The decoded data   
     * @exception   Base64Exception Thrown is processing fails due to
     * formatting exceptions in the encoded data 
     */
    public static byte[] decodeChunk(char[] id,
                                     int o,
                                     int l) 
        throws Base64Exception {
        
        // Keep it simple - must be >= 4. Unpadded
        // base64 data contain < 3 octets is invalid.
        //
        if ((l - o) < 4) {
            return null;
        }

        char[] ib = new char[4];
        int ibcount = 0;

        // cryan. Calc the num of octets. Each 4 chars of base64 chars
        // (representing 24 bits) encodes 3 octets. 
        //
        int octetCount = 3 * (l / 4);

        // Final 4 chars may contain 3 octets or padded to contain
        // 1 or 2 octets.
        //
        if (id[l - 1] == PAD) {
            // TT== means last 4 chars encode 8 bits (ie subtract 2)
            // TTT= means last 4 chars encode 16 bits (ie subtract 1)
            octetCount -= (id[l - 2] == PAD) ? 2 : 1;
        }

        byte[] ob = new byte[octetCount];
        int obcount = 0;

        for (int i = o;  i < o + l && i < id.length;  i++) {
            if (id[i] == PAD
                || id[i] < BDT.length
                && BDT[id[i]] != Byte.MAX_VALUE) {
                
                ib[ibcount++] = id[i];

                // Decode each 4 char sequence.
                //
                if (ibcount == ib.length) {
                    ibcount = 0;
                    obcount += processEncodeme(ib, ob, obcount);
                }
            }
        }
        
        if (obcount != ob.length) {
            byte []tmp = new byte[obcount];
            System.arraycopy(ob, 0, tmp, 0, obcount);
            ob = tmp;
        }

        return ob;
    }

    public static byte[] decode(String id) throws Base64Exception {
        return decode(id, false);
    }
    
    public static byte[] decode(String id, boolean urlSafe) throws Base64Exception {
        if (urlSafe) {
            //TODO: optimize further
            id = id.replace("-", "+").replace('_', '/');
            switch (id.length() % 4) {
            case 0: 
                break; 
            case 2: 
                id += "=="; 
                break; 
            case 3: 
                id += "="; 
                break; 
            default: 
                throw new Base64Exception("base64 encode exception");
            }
        }
        try {
            char[] cd = id.toCharArray();
            return decodeChunk(cd, 0, cd.length);
        } catch (Exception e) {
            LOG.warn("Invalid base64 encoded string : " + id);
            throw new Base64Exception("base64 encode exception", e);
        }
    }

    public static void decode(char[] id,
                             int o,
                             int l,
                             OutputStream ostream) 
        throws Base64Exception {

        try {
            ostream.write(decodeChunk(id, o, l));
        } catch (Exception e) {
            LOG.warn("Invalid base64 encoded string : " + new String(id));
            throw new Base64Exception("base64 encode exception", e);
        }
    }

    public static void decode(String id,
                              OutputStream ostream) 
        throws Base64Exception {
        
        try {
            char[] cd = id.toCharArray();
            ostream.write(decodeChunk(cd, 0, cd.length));
        } catch (IOException ioe) {
            throw new Base64Exception("base64 encode exception", ioe);
        } catch (Exception e) {
            LOG.warn("Invalid base64 encoded string : " + id);
            throw new Base64Exception("base64 encode exception", e);
        }
    }

    // Returns base64 representation of specified byte array.
    //
    public static String encode(byte[] id) {
        return encode(id, false);
    }
    
    public static String encode(byte[] id, boolean urlSafe) {
        char[] cd = encodeChunk(id, 0, id.length,urlSafe);
        return new String(cd, 0, cd.length);
    }

    // Returns base64 representation of specified byte array.
    //
    public static char[] encodeChunk(byte[] id,
                                     int o,
                                     int l) {
        return encodeChunk(id, o, l, false);
    }
    
    public static char[] encodeChunk(byte[] id,
                                     int o,
                                     int l,
                                     boolean urlSafe) {
        if (l <= 0) {
            return null;
        }

        char[] out;

        // If not a multiple of 3 octets then a final padded 4 char
        // slot is needed.
        //
        if (l % 3 == 0) {
            out = new char[l / 3 * 4];
        } else {
            int finalLen = !urlSafe ? 4 : l % 3 == 1 ? 2 : 3;
            out = new char[l / 3 * 4 + finalLen];
        }

        int rindex = o;
        int windex = 0;
        int rest = l;

        final char[] base64Table = urlSafe ? BCS_URL_SAFE : BCS;
        while (rest >= 3) {
            int i = ((id[rindex] & 0xff) << 16)
                    + ((id[rindex + 1] & 0xff) << 8)
                    + (id[rindex + 2] & 0xff);

            out[windex++] = base64Table[i >> 18];
            out[windex++] = base64Table[(i >> 12) & 0x3f];
            out[windex++] = base64Table[(i >> 6) & 0x3f];
            out[windex++] = base64Table[i & 0x3f];
            rindex += 3;
            rest -= 3;
        }

        if (rest == 1) {
            int i = id[rindex] & 0xff;
            out[windex++] = base64Table[i >> 2];
            out[windex++] = base64Table[(i << 4) & 0x3f];
            if (!urlSafe) {
                out[windex++] = PAD;
                out[windex++] = PAD;
            }
        } else if (rest == 2) {
            int i = ((id[rindex] & 0xff) << 8) + (id[rindex + 1] & 0xff);
            out[windex++] = base64Table[i >> 10];
            out[windex++] = base64Table[(i >> 4) & 0x3f];
            out[windex++] = base64Table[(i << 2) & 0x3f];
            if (!urlSafe) {
                out[windex++] = PAD;
            }
        }
        return out;
    }
    
    public static void encodeAndStream(byte[] id,
                                       int o,
                                       int l,
                                       OutputStream os) throws IOException {
        encodeAndStream(id, o, l, false, os);
    }
    
    public static void encodeAndStream(byte[] id,
                                           int o,
                                           int l,
                                           boolean urlSafe,
                                           OutputStream os) throws IOException {
        if (l <= 0) {
            return;
        }

        int rindex = o;
        int rest = l;
        final char[] base64Table = urlSafe ? BCS_URL_SAFE : BCS;
        
        char[] chunk = new char[4];
        while (rest >= 3) {
            int i = ((id[rindex] & 0xff) << 16)
                    + ((id[rindex + 1] & 0xff) << 8)
                    + (id[rindex + 2] & 0xff);
            chunk[0] = base64Table[i >> 18]; 
            chunk[1] = base64Table[(i >> 12) & 0x3f];
            chunk[2] = base64Table[(i >> 6) & 0x3f];
            chunk[3] = base64Table[i & 0x3f];
            writeCharArrayToStream(chunk, 4, os);
            rindex += 3;
            rest -= 3;
        }
        if (rest == 0) {
            return;
        }
        if (rest == 1) {
            int i = id[rindex] & 0xff;
            chunk[0] = base64Table[i >> 2];
            chunk[1] = base64Table[(i << 4) & 0x3f];
            if (!urlSafe) {
                chunk[2] = PAD;
                chunk[3] = PAD;
            }
        } else if (rest == 2) {
            int i = ((id[rindex] & 0xff) << 8) + (id[rindex + 1] & 0xff);
            chunk[0] = base64Table[i >> 10];
            chunk[1] = base64Table[(i >> 4) & 0x3f];
            chunk[2] = base64Table[(i << 2) & 0x3f];
            if (!urlSafe) {
                chunk[3] = PAD;
            }
        }
        int finalLenToWrite = !urlSafe ? 4 : rest == 1 ? 2 : 3;
        writeCharArrayToStream(chunk, finalLenToWrite, os);
    }

    private static void writeCharArrayToStream(char[] chunk, int len, OutputStream os) throws IOException {
        // may be we can just cast to byte when creating chunk[] earlier on
        byte[] bytes = CHARSET_UTF8.encode(CharBuffer.wrap(chunk, 0, len)).array();
        os.write(bytes);
    }
    
    //
    // Outputs base64 representation of the specified byte array 
    // to a byte stream.
    //
    public static void encodeChunk(byte[] id,
                                   int o,
                                   int l,
                                   OutputStream ostream) throws Base64Exception {
        try {
            ostream.write(new String(encodeChunk(id, o, l)).getBytes());
        } catch (IOException e) {
            throw new Base64Exception("base64 encode exception", e);
        }
    }
    
    // Outputs base64 representation of the specified byte 
    // array to a character stream.
    //
    public static void encode(byte[] id,
                              int o,
                              int l,
                              Writer writer) throws Base64Exception {
        try {
            writer.write(encodeChunk(id, o, l));
        } catch (IOException e) {
            throw new Base64Exception("base64 encode exception", e);
        }
    }
    //---- Private static methods --------------------------------------

    /**
     * The <code>process</code> routine processes an atomic base64
     * unit of encoding (encodeme) into its native encoding. This class is
     * used by decode routines to do the grunt work of decoding
     * base64 encoded information
     *
     * @param   ib  Input character buffer of encoded bytes
     * @param   ob  Output byte buffer of decoded bytes
     * @param   p   Pointer to the encodeme of interest
     * @return  The decoded encodeme
     * @exception   Base64Exception Thrown is processing fails due to
     * formatting exceptions in the encoded data 
     */ 
    private static int processEncodeme(char[] ib,
                                       byte[] ob,
                                       int p) 
        throws Base64Exception {
        

        int spad = PAD_SIZE8;        
        if (ib[3] == PAD) {
            spad = PAD_SIZE4;
        }
        if (ib[2] == PAD) {
            spad = PAD_SIZE0;
        }

        int b0 = BDT[ib[0]];
        int b1 = BDT[ib[1]];
        int b2 = BDT[ib[2]];
        int b3 = BDT[ib[3]];

        switch (spad) {
        case PAD_SIZE0:
            ob[p] = (byte)(b0 << 2 & 0xfc | b1 >> 4 & 0x3);
            return PAD_SIZE0;
        case PAD_SIZE4:
            ob[p++] = (byte)(b0 << 2 & 0xfc | b1 >> 4 & 0x3);
            ob[p] = (byte)(b1 << 4 & 0xf0 | b2 >> 2 & 0xf);
            return PAD_SIZE4;
        case PAD_SIZE8:
            ob[p++] = (byte)(b0 << 2 & 0xfc | b1 >> 4 & 0x3);
            ob[p++] = (byte)(b1 << 4 & 0xf0 | b2 >> 2 & 0xf);
            ob[p] = (byte)(b2 << 6 & 0xc0 | b3 & 0x3f);
            return PAD_SIZE8;
        default:
            // We should never get here
            throw new IllegalStateException();
        } 
    }

    public static boolean isValidBase64(int ch) {
        return ch == PAD || BDT[ch] != Byte.MAX_VALUE;
    } 
}
