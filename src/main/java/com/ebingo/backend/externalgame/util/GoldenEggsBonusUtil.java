package com.ebingo.backend.externalgame.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

/**
 * Utility class for Golden Eggs Bonus operations
 */
public class GoldenEggsBonusUtil {

    /**
     * Generate subOperatorId from aggregatorId and subId
     * Algorithm: UUID v4 from MD5 hash of "${aggregatorId}:${subId}"
     *
     * @param aggregatorId The aggregator ID
     * @param subId        The sub ID
     * @return UUID subOperatorId
     */
    public static UUID generateSubOperatorId(String aggregatorId, String subId) {
        String input = aggregatorId + ":" + subId;
        return generateUUIDv4FromString(input);
    }

    /**
     * Generate UUID v4 from string using MD5 hash as seed
     *
     * @param inputString Input string
     * @return UUID v4
     */
    public static UUID generateUUIDv4FromString(String inputString) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] seed = md.digest(inputString.getBytes(StandardCharsets.UTF_8));

            // UUID v4 format: xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx
            // where y is 8, 9, A, or B
            seed[6] &= 0x0f;  // clear version
            seed[6] |= 0x40;  // set to version 4
            seed[8] &= 0x3f;  // clear variant
            seed[8] |= (byte) 0x80;  // set to IETF variant

            long msb = 0;
            long lsb = 0;
            for (int i = 0; i < 8; i++) {
                msb = (msb << 8) | (seed[i] & 0xff);
            }
            for (int i = 8; i < 16; i++) {
                lsb = (lsb << 8) | (seed[i] & 0xff);
            }

            return new UUID(msb, lsb);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate UUID from string", e);
        }
    }

    /**
     * Generate X-REQUEST-SIGN header value
     * Algorithm: HMAC-SHA256(operatorId, signatureKey) in hex lowercase
     *
     * @param operatorId   The operator ID (subOperatorId)
     * @param signatureKey The signature secret key
     * @return Hex lowercase signature
     */
    public static String generateRequestSignature(String operatorId, String signatureKey) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    signatureKey.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(operatorId.getBytes(StandardCharsets.UTF_8));

            // Convert to hex lowercase
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate request signature", e);
        }
    }
}
