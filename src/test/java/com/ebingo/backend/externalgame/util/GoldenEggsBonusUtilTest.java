package com.ebingo.backend.externalgame.util;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for GoldenEggsBonusUtil
 * Validates SubOperatorId generation and request signing
 */
class GoldenEggsBonusUtilTest {

    @Test
    void testGenerateSubOperatorId_Deterministic() {
        // Given
        String aggregatorId = "ee2013ed-e1f0-4d6e-97d2-f36619e2eb88";
        String subId = "brand-123";
        String expectedSubOperatorId = "d48ce2b5-2a56-4de7-952f-12254f0d2dd2";

        // When
        UUID result = GoldenEggsBonusUtil.generateSubOperatorId(aggregatorId, subId);

        // Then
        assertEquals(expectedSubOperatorId, result.toString());
    }

    @Test
    void testGenerateSubOperatorId_SameInputProducesSameOutput() {
        // Given
        String aggregatorId = "ee2013ed-e1f0-4d6e-97d2-f36619e2eb88";
        String subId = "brand-123";

        // When
        UUID result1 = GoldenEggsBonusUtil.generateSubOperatorId(aggregatorId, subId);
        UUID result2 = GoldenEggsBonusUtil.generateSubOperatorId(aggregatorId, subId);

        // Then
        assertEquals(result1, result2, "Same input should produce same subOperatorId");
    }

    @Test
    void testGenerateSubOperatorId_DifferentSubIdProducesDifferentOutput() {
        // Given
        String aggregatorId = "ee2013ed-e1f0-4d6e-97d2-f36619e2eb88";
        String subId1 = "brand-123";
        String subId2 = "brand-456";

        // When
        UUID result1 = GoldenEggsBonusUtil.generateSubOperatorId(aggregatorId, subId1);
        UUID result2 = GoldenEggsBonusUtil.generateSubOperatorId(aggregatorId, subId2);

        // Then
        assertNotEquals(result1, result2, "Different subId should produce different subOperatorId");
    }

    @Test
    void testGenerateSubOperatorId_IsValidUUIDv4() {
        // Given
        String aggregatorId = "ee2013ed-e1f0-4d6e-97d2-f36619e2eb88";
        String subId = "brand-123";

        // When
        UUID result = GoldenEggsBonusUtil.generateSubOperatorId(aggregatorId, subId);

        // Then
        assertNotNull(result);
        // UUID v4 has version bits set to 0100 (4)
        assertEquals(4, result.version(), "Should be UUID version 4");
        // UUID v4 has variant bits set to 10xx
        assertEquals(2, result.variant(), "Should be IETF variant");
    }

    @Test
    void testGenerateRequestSignature_MatchesExpected() {
        // Given - from specification example
        String operatorId = "6295c404-e633-48cc-ae14-8ca0880d55d4";
        String signatureKey = "7A2DB2F4FE86998735835F538826B262E834662EA297AB8B4286DBFE315A2467521D791A94E3D12A942427A29F";
        String expectedSignature = "6548c3439d482c6b330d421aca1e9947bfd80da15286a6b8791ef39579d3fcae";

        // When
        String result = GoldenEggsBonusUtil.generateRequestSignature(operatorId, signatureKey);

        // Then
        assertEquals(expectedSignature, result);
    }

    @Test
    void testGenerateRequestSignature_IsHexLowercase() {
        // Given
        String operatorId = "6295c404-e633-48cc-ae14-8ca0880d55d4";
        String signatureKey = "test-secret-key";

        // When
        String result = GoldenEggsBonusUtil.generateRequestSignature(operatorId, signatureKey);

        // Then
        assertNotNull(result);
        assertTrue(result.matches("^[a-f0-9]+$"), "Signature should be hex lowercase");
        assertEquals(64, result.length(), "HMAC-SHA256 should produce 64 hex characters");
    }

    @Test
    void testGenerateRequestSignature_Deterministic() {
        // Given
        String operatorId = "test-operator-id";
        String signatureKey = "test-secret-key";

        // When
        String result1 = GoldenEggsBonusUtil.generateRequestSignature(operatorId, signatureKey);
        String result2 = GoldenEggsBonusUtil.generateRequestSignature(operatorId, signatureKey);

        // Then
        assertEquals(result1, result2, "Same input should produce same signature");
    }

    @Test
    void testGenerateRequestSignature_DifferentOperatorIdProducesDifferentSignature() {
        // Given
        String operatorId1 = "operator-1";
        String operatorId2 = "operator-2";
        String signatureKey = "test-secret-key";

        // When
        String result1 = GoldenEggsBonusUtil.generateRequestSignature(operatorId1, signatureKey);
        String result2 = GoldenEggsBonusUtil.generateRequestSignature(operatorId2, signatureKey);

        // Then
        assertNotEquals(result1, result2, "Different operatorId should produce different signature");
    }

    @Test
    void testGenerateRequestSignature_DifferentKeyProducesDifferentSignature() {
        // Given
        String operatorId = "test-operator-id";
        String signatureKey1 = "secret-key-1";
        String signatureKey2 = "secret-key-2";

        // When
        String result1 = GoldenEggsBonusUtil.generateRequestSignature(operatorId, signatureKey1);
        String result2 = GoldenEggsBonusUtil.generateRequestSignature(operatorId, signatureKey2);

        // Then
        assertNotEquals(result1, result2, "Different signatureKey should produce different signature");
    }

    @Test
    void testGenerateUUIDv4FromString_Deterministic() {
        // Given
        String input = "test-input-string";

        // When
        UUID result1 = GoldenEggsBonusUtil.generateUUIDv4FromString(input);
        UUID result2 = GoldenEggsBonusUtil.generateUUIDv4FromString(input);

        // Then
        assertEquals(result1, result2, "Same input should produce same UUID");
    }

    @Test
    void testGenerateUUIDv4FromString_DifferentInputProducesDifferentUUID() {
        // Given
        String input1 = "input-1";
        String input2 = "input-2";

        // When
        UUID result1 = GoldenEggsBonusUtil.generateUUIDv4FromString(input1);
        UUID result2 = GoldenEggsBonusUtil.generateUUIDv4FromString(input2);

        // Then
        assertNotEquals(result1, result2, "Different input should produce different UUID");
    }
}
