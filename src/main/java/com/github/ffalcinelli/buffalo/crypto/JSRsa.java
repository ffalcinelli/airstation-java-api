package com.github.ffalcinelli.buffalo.crypto;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * This is a Java translation of Buffalo AirStation Javascript RSA implementation.
 * <p>
 * Created by fabio on 24/02/17.
 */
public class JSRsa implements Encryptor {
    private final static String b64map = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    private final static String b64pad = "=";
    private BigInteger exp;
    private BigInteger mod;
    private int len;

    /**
     * Build an AirStation RSA object. Modulus and Exponent must be read from login page, they could change from device to device.
     *
     * @param exp The exponent (Integer string representation).
     * @param mod The modulus (hex string).
     */
    public JSRsa(String exp, String mod) {
        this.exp = new BigInteger(Integer.toHexString(Integer.parseInt(exp)), 16);
        this.mod = new BigInteger(mod, 16);
        this.len = (this.mod.bitLength() + 7) >> 3;
    }


    String hex2b64(String h) {
        int i;
        int c;
        String ret = "";
        for (i = 0; i + 3 <= h.length(); i += 3) {
            c = Integer.parseInt(h.substring(i, i + 3), 16);
            ret += String.format("%c%c", b64map.charAt(c >> 6), b64map.charAt(c & 63));
        }
        if (i + 1 == h.length()) {
            c = Integer.parseInt(h.substring(i, i + 1), 16);
            ret += String.format("%c", b64map.charAt(c << 2));
        } else if (i + 2 == h.length()) {
            c = Integer.parseInt(h.substring(i, i + 2), 16);
            ret += String.format("%c%c", b64map.charAt(c >> 2), b64map.charAt((c & 3) << 4));
        }
        while ((ret.length() & 3) > 0) ret += b64pad;
        return ret;
    }

    String linebrk(String s, int n) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while ((i + n) < s.length()) {
            sb.append(s.substring(i, i + n)).append("\n");
            i += n;
        }
        return sb.append(s.substring(i, s.length())).toString();
    }

    // PKCS#1 (type 2, random) pad input string s to n bytes, and return a bigint
    BigInteger pkcs1pad2(String s) {
        int n = this.len;
        if (n < s.length() + 11) { // TODO: fix for utf-8
            throw new IllegalArgumentException("Message too long for RSA");
        }

        int i = s.length() - 1;
        byte[] ba = new byte[n];
        while (i >= 0 && n > 0) {
            int c = s.codePointAt(i--);
            if (c < 128) { // encode using utf-8
                ba[--n] = (byte) c;
            } else if ((c > 127) && (c < 2048)) {
                ba[--n] = (byte) ((c & 63) | 128);
                ba[--n] = (byte) ((c >> 6) | 192);
            } else {
                ba[--n] = (byte) ((c & 63) | 128);
                ba[--n] = (byte) (((c >> 6) & 63) | 128);
                ba[--n] = (byte) ((c >> 12) | 224);
            }
        }
        ba[--n] = 0x0;
        SecureRandom rng = new SecureRandom();
        byte[] x = new byte[1];
        while (n > 2) { // random non-zero pad
            x[0] = 0x0;
            while (x[0] == 0x0)
                rng.nextBytes(x);
            ba[--n] = x[0];
        }
        ba[--n] = 2;
        ba[--n] = 0x0;
        return new BigInteger(ba);
    }

    String hexToBase64(String data) {
        if (data != null && !data.equals("")) {
            return linebrk(hex2b64(data), 64);
        }
        return "";
    }

    /**
     * Encrypt the given String using AirStation's RSA implementation.
     *
     * @param plainText The text to encrypt.
     * @return The encrypted text.
     */
    public String encrypt(String plainText) {
        BigInteger m = pkcs1pad2(plainText);
        BigInteger c = m.modPow(exp, mod);
        String h = c.toString(16);
        if ((h.length() & 1) == 0) {
            return hexToBase64(h);
        }
        return hexToBase64("0" + h);
    }
}
