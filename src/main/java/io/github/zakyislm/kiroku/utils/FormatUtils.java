package io.github.zakyislm.kiroku.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class FormatUtils {
    private static final byte[] MAGIC = new byte[]{0x4B, 0x49, 0x52, 0x4F}; // "KIRO"
    private static final byte FORMAT_VERSION = 1;
    private static final byte[] XOR_KEY = "KirokuObfuscationKey2026!".getBytes(StandardCharsets.UTF_8);

    private static byte[] xorCipher(byte[] data) {
        byte[] result = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = (byte) (data[i] ^ XOR_KEY[i % XOR_KEY.length]);
        }
        return result;
    }

    private static final String SYSTEM_HEADER = 
        "// DO NOT REMOVE THIS FILE, UNLESS YOU UNDERSTAND\n" +
        "// written by Kiroku";

    private static final String CONFIG_HEADER = 
        "// DO NOT REMOVE THIS FILE, UNLESS YOU UNDERSTAND\n" +
        "// written by Kiroku\n" +
        "/?\n" +
        "  user configuration\n" +
        "  format: KIRO v1\n" +
        "?/";

    // â”€â”€ System File (ID Counter) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    public static int parseSystemFile(byte[] bytes) {
        try {
            int offset = findSystemPayloadOffset(bytes);
            if (offset + 4 > bytes.length) {
                return 0;
            }
            ByteBuffer buffer = ByteBuffer.wrap(bytes, offset, 4);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            return buffer.getInt();
        } catch (Exception e) {
            return 0;
        }
    }

    public static byte[] writeSystemFile(int id) {
        byte[] headerBytes = (SYSTEM_HEADER + "\n").getBytes(StandardCharsets.UTF_8);
        byte[] idBytes = new byte[4];
        ByteBuffer.wrap(idBytes).order(ByteOrder.LITTLE_ENDIAN).putInt(id);

        byte[] combined = new byte[headerBytes.length + idBytes.length];
        System.arraycopy(headerBytes, 0, combined, 0, headerBytes.length);
        System.arraycopy(idBytes, 0, combined, headerBytes.length, idBytes.length);
        return combined;
    }

    private static int findSystemPayloadOffset(byte[] bytes) {
        int i = 0;
        while (i < bytes.length) {
            // Skip whitespace and newlines
            while (i < bytes.length && (bytes[i] == 0x20 || bytes[i] == 0x09 || bytes[i] == 0x0D || bytes[i] == 0x0A)) {
                i++;
            }
            if (i >= bytes.length) break;

            // Check if line starts with '//'
            if (i + 1 < bytes.length && bytes[i] == 0x2F && bytes[i + 1] == 0x2F) {
                i += 2;
                while (i < bytes.length && bytes[i] != 0x0A) {
                    i++;
                }
                if (i < bytes.length) i++; // Skip the newline
            } else {
                return i; // Found start of non-comment payload
            }
        }
        return bytes.length;
    }

    // â”€â”€ Config File â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    public static String parseConfigFile(byte[] bytes) {
        try {
            int magicIndex = findMagicIndex(bytes);
            if (magicIndex == -1) {
                return null;
            }

            // Read version and data length
            byte version = bytes[magicIndex + 4];
            ByteBuffer lenBuf = ByteBuffer.wrap(bytes, magicIndex + 5, 4);
            lenBuf.order(ByteOrder.LITTLE_ENDIAN);
            int dataLength = lenBuf.getInt();

            if (magicIndex + 9 + dataLength > bytes.length) {
                return null;
            }

            byte[] encryptedBytes = new byte[dataLength];
            System.arraycopy(bytes, magicIndex + 9, encryptedBytes, 0, dataLength);
            byte[] decryptedBytes = xorCipher(encryptedBytes);

            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    public static String parseConfigFilePlain(byte[] bytes) {
        try {
            int magicIndex = findMagicIndex(bytes);
            if (magicIndex == -1) {
                return null;
            }

            // Read version and data length
            ByteBuffer lenBuf = ByteBuffer.wrap(bytes, magicIndex + 5, 4);
            lenBuf.order(ByteOrder.LITTLE_ENDIAN);
            int dataLength = lenBuf.getInt();

            if (magicIndex + 9 + dataLength > bytes.length) {
                return null;
            }

            byte[] rawBytes = new byte[dataLength];
            System.arraycopy(bytes, magicIndex + 9, rawBytes, 0, dataLength);

            return new String(rawBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }


    public static byte[] writeConfigFile(String json) {
        byte[] headerBytes = (CONFIG_HEADER + "\n").getBytes(StandardCharsets.UTF_8);
        byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
        byte[] encryptedJsonBytes = xorCipher(jsonBytes);

        byte[] payloadHeader = new byte[9];
        System.arraycopy(MAGIC, 0, payloadHeader, 0, MAGIC.length);
        payloadHeader[4] = FORMAT_VERSION;
        ByteBuffer.wrap(payloadHeader, 5, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(encryptedJsonBytes.length);

        byte[] combined = new byte[headerBytes.length + payloadHeader.length + encryptedJsonBytes.length];
        System.arraycopy(headerBytes, 0, combined, 0, headerBytes.length);
        System.arraycopy(payloadHeader, 0, combined, headerBytes.length, payloadHeader.length);
        System.arraycopy(encryptedJsonBytes, 0, combined, headerBytes.length + payloadHeader.length, encryptedJsonBytes.length);
        
        return combined;
    }

    private static int findMagicIndex(byte[] bytes) {
        int startIndex = findCommentEndIndex(bytes);
        for (int i = startIndex; i <= bytes.length - 4; i++) {
            if (bytes[i] == MAGIC[0] &&
                bytes[i + 1] == MAGIC[1] &&
                bytes[i + 2] == MAGIC[2] &&
                bytes[i + 3] == MAGIC[3]) {
                return i;
            }
        }
        return -1;
    }

    private static int findCommentEndIndex(byte[] bytes) {
        for (int i = 0; i <= bytes.length - 2; i++) {
            // Find "?/" (0x3F, 0x2F)
            if (bytes[i] == 0x3F && bytes[i + 1] == 0x2F) {
                return i + 2;
            }
        }
        return 0;
    }
}
