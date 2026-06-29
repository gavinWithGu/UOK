package com.bosch.iot.uok.common.desensitize;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DataMasker}.
 */
class DataMaskerTest {

    @Test
    @DisplayName("Should mask sensitive fields by default")
    void shouldMaskSensitiveFields() {
        DataMasker masker = new DataMasker();
        assertThat(masker.maskIfSensitive("password", "mysecret123")).isNotEqualTo("mysecret123");
        assertThat(masker.maskIfSensitive("apiKey", "abc123def456")).isNotEqualTo("abc123def456");
        assertThat(masker.maskIfSensitive("token", "tok_abc123")).isNotEqualTo("tok_abc123");
    }

    @Test
    @DisplayName("Should not mask non-sensitive fields")
    void shouldNotMaskNonSensitiveFields() {
        DataMasker masker = new DataMasker();
        assertThat(masker.maskIfSensitive("username", "john")).isEqualTo("john");
        assertThat(masker.maskIfSensitive("email", "test@example.com")).isEqualTo("test@example.com");
        assertThat(masker.maskIfSensitive("deviceId", "dev-123")).isEqualTo("dev-123");
    }

    @Test
    @DisplayName("Should handle null values")
    void shouldHandleNullValues() {
        DataMasker masker = new DataMasker();
        assertThat(masker.maskIfSensitive("password", null)).isNull();
        assertThat(masker.maskIfSensitive(null, "value")).isEqualTo("value");
    }

    @Test
    @DisplayName("Should mask with KEEP_EDGES strategy")
    void shouldMaskWithKeepEdges() {
        DataMasker masker = new DataMasker();
        String result = masker.mask("abcdefgh", DataMasker.MaskStrategy.KEEP_EDGES);
        assertThat(result).startsWith("ab");
        assertThat(result).endsWith("gh");
        assertThat(result).contains("*");
    }

    @Test
    @DisplayName("Should mask with FULL_MASK strategy")
    void shouldMaskWithFullMask() {
        DataMasker masker = new DataMasker();
        String result = masker.mask("abcdefgh", DataMasker.MaskStrategy.FULL_MASK);
        assertThat(result).isEqualTo("********");
    }

    @Test
    @DisplayName("Should mask with KEEP_HEAD strategy")
    void shouldMaskWithKeepHead() {
        DataMasker masker = new DataMasker();
        String result = masker.mask("abcdefgh", DataMasker.MaskStrategy.KEEP_HEAD);
        assertThat(result).startsWith("ab");
        assertThat(result).contains("*");
        assertThat(result).doesNotEndWith("gh");
    }

    @Test
    @DisplayName("Should mask with KEEP_TAIL strategy")
    void shouldMaskWithKeepTail() {
        DataMasker masker = new DataMasker();
        String result = masker.mask("abcdefgh", DataMasker.MaskStrategy.KEEP_TAIL);
        assertThat(result).endsWith("gh");
        assertThat(result).contains("*");
        assertThat(result).doesNotStartWith("ab");
    }

    @Test
    @DisplayName("Should not mask with NONE strategy")
    void shouldNotMaskWithNone() {
        DataMasker masker = new DataMasker();
        String result = masker.mask("abcdefgh", DataMasker.MaskStrategy.NONE);
        assertThat(result).isEqualTo("abcdefgh");
    }

    @Test
    @DisplayName("Should handle short values")
    void shouldHandleShortValues() {
        DataMasker masker = new DataMasker();
        String result = masker.mask("ab", DataMasker.MaskStrategy.KEEP_EDGES);
        // Short values get full mask
        assertThat(result).contains("*");
    }

    @Test
    @DisplayName("Should handle empty values")
    void shouldHandleEmptyValues() {
        DataMasker masker = new DataMasker();
        assertThat(masker.mask("", DataMasker.MaskStrategy.KEEP_EDGES)).isEmpty();
        assertThat(masker.mask(null, DataMasker.MaskStrategy.KEEP_EDGES)).isNull();
    }

    @Test
    @DisplayName("Should mask email addresses")
    void shouldMaskEmailAddresses() {
        String result = DataMasker.maskEmail("Contact user@example.com for info");
        assertThat(result).contains("***@example.com");
        assertThat(result).doesNotContain("user@example.com");
    }

    @Test
    @DisplayName("Should mask phone numbers")
    void shouldMaskPhoneNumbers() {
        String result = DataMasker.maskPhone("Call 13812345678 for help");
        assertThat(result).contains("138****5678");
        assertThat(result).doesNotContain("1234");
    }

    @Test
    @DisplayName("Should handle null in static mask methods")
    void shouldHandleNullInStaticMethods() {
        assertThat(DataMasker.maskEmail(null)).isNull();
        assertThat(DataMasker.maskPhone(null)).isNull();
    }

    @Test
    @DisplayName("Should add and remove sensitive fields")
    void shouldAddAndRemoveSensitiveFields() {
        DataMasker masker = new DataMasker();
        masker.addSensitiveField("customDataField");
        assertThat(masker.isSensitive("customDataField")).isTrue();
        assertThat(masker.isSensitive("myCustomDataField")).isTrue();

        masker.removeSensitiveField("customDataField");
        assertThat(masker.isSensitive("customDataField")).isFalse();
    }

    @Test
    @DisplayName("Should check sensitivity case-insensitively")
    void shouldCheckSensitivityCaseInsensitively() {
        DataMasker masker = new DataMasker();
        assertThat(masker.isSensitive("PASSWORD")).isTrue();
        assertThat(masker.isSensitive("Password")).isTrue();
        assertThat(masker.isSensitive("userPassword")).isTrue();
    }

    @Test
    @DisplayName("Should handle null in sensitive field check")
    void shouldHandleNullInSensitiveCheck() {
        DataMasker masker = new DataMasker();
        assertThat(masker.isSensitive(null)).isFalse();
    }

    @Test
    @DisplayName("Should create with custom settings")
    void shouldCreateWithCustomSettings() {
        Set<String> fields = new HashSet<>();
        fields.add("customField");
        DataMasker masker = new DataMasker(fields, DataMasker.MaskStrategy.FULL_MASK, 3);

        assertThat(masker.isSensitive("customField")).isTrue();
        String result = masker.mask("abcdefgh");
        assertThat(result).isEqualTo("********");
    }

    @Test
    @DisplayName("Should handle null in custom constructor")
    void shouldHandleNullInCustomConstructor() {
        DataMasker masker = new DataMasker(null, null, 3);
        assertThat(masker.isSensitive("password")).isFalse();
    }
}
