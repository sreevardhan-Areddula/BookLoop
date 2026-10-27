package com.bookloop.account;

final class HexFormatSupport {

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private HexFormatSupport() {
    }

    static String toHex(byte[] bytes) {
        char[] output = new char[bytes.length * 2];
        for (int index = 0; index < bytes.length; index++) {
            int value = bytes[index] & 0xff;
            output[index * 2] = HEX[value >>> 4];
            output[index * 2 + 1] = HEX[value & 0x0f];
        }
        return new String(output);
    }
}
