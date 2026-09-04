package com.zeroreel.app;

import android.net.Uri;

import java.nio.ByteBuffer;
import java.security.SecureRandom;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

final class Totp {
    private static final String BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final int PERIOD_SECONDS = 30;
    private static final int DIGITS = 6;

    /** Same on every install. Used only to reveal the unique lock secret. */
    static final String UNIVERSAL_REVEAL_SECRET = "K7N2WQXM4TYP5HRCZVLB6G3FJEASUD2I";

    private Totp() {}

    static String generateSecret() {
        byte[] raw = new byte[20];
        new SecureRandom().nextBytes(raw);
        return encodeBase32(raw);
    }

    static String otpAuthUri(String secret) {
        return otpAuthUri(secret, "lock");
    }

    static String otpAuthUri(String secret, String account) {
        return "otpauth://totp/" + Uri.encode("Zero Reel:" + account)
                + "?secret=" + secret
                + "&issuer=" + Uri.encode("Zero Reel")
                + "&algorithm=SHA1&digits=" + DIGITS
                + "&period=" + PERIOD_SECONDS;
    }

    static String universalRevealUri() {
        return otpAuthUri(UNIVERSAL_REVEAL_SECRET, "reveal");
    }

    static boolean verify(String secret, String code) {
        if (secret == null || secret.isEmpty() || code == null) return false;
        String normalized = code.replaceAll("\\s+", "");
        if (normalized.length() != DIGITS || !normalized.matches("\\d{6}")) return false;
        long timestep = System.currentTimeMillis() / (PERIOD_SECONDS * 1000L);
        for (int drift = -1; drift <= 1; drift++) {
            if (normalized.equals(codeAt(secret, timestep + drift))) return true;
        }
        return false;
    }

    static String currentCode(String secret) {
        long timestep = System.currentTimeMillis() / (PERIOD_SECONDS * 1000L);
        return codeAt(secret, timestep);
    }

    private static String codeAt(String secret, long timestep) {
        try {
            byte[] key = decodeBase32(secret);
            byte[] data = ByteBuffer.allocate(8).putLong(timestep).array();
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(data);
            int offset = hash[hash.length - 1] & 0x0f;
            int binary = ((hash[offset] & 0x7f) << 24)
                    | ((hash[offset + 1] & 0xff) << 16)
                    | ((hash[offset + 2] & 0xff) << 8)
                    | (hash[offset + 3] & 0xff);
            int otp = binary % 1_000_000;
            return String.format("%06d", otp);
        } catch (Exception e) {
            return "";
        }
    }

    private static String encodeBase32(byte[] data) {
        StringBuilder out = new StringBuilder();
        int buffer = 0;
        int bitsLeft = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xff);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                out.append(BASE32.charAt((buffer >> (bitsLeft - 5)) & 31));
                bitsLeft -= 5;
            }
        }
        if (bitsLeft > 0) {
            out.append(BASE32.charAt((buffer << (5 - bitsLeft)) & 31));
        }
        return out.toString();
    }

    private static byte[] decodeBase32(String encoded) {
        String clean = encoded.toUpperCase().replace("=", "").replace(" ", "");
        int buffer = 0;
        int bitsLeft = 0;
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        for (int i = 0; i < clean.length(); i++) {
            int val = BASE32.indexOf(clean.charAt(i));
            if (val < 0) continue;
            buffer = (buffer << 5) | val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                out.write((buffer >> (bitsLeft - 8)) & 0xff);
                bitsLeft -= 8;
            }
        }
        return out.toByteArray();
    }
}
