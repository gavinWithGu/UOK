package com.bosch.iot.uok.common.desensitize;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Data masking utility for sensitive field desensitization.
 * Supports multiple masking strategies for different types of sensitive data.
 */
public class DataMasker {

    /**
     * Masking strategy enumeration.
     */
    public enum MaskStrategy {
        /** Keep first N and last N characters, mask the middle */
        KEEP_EDGES,
        /** Replace entire value with asterisks */
        FULL_MASK,
        /** Keep only the last N characters */
        KEEP_TAIL,
        /** Keep only the first N characters */
        KEEP_HEAD,
        /** No masking */
        NONE
    }

    private static final String DEFAULT_MASK_CHAR = "*";
    private static final int DEFAULT_VISIBLE_LENGTH = 2;

    private final Set<String> sensitiveFields;
    private final MaskStrategy defaultStrategy;
    private final int visibleLength;
    private final String maskChar;

    /**
     * Create a data masker with default settings.
     * Default sensitive fields: password, secret, token, apiKey, credential.
     */
    public DataMasker() {
        this.sensitiveFields = new HashSet<>(Arrays.asList(
                "password", "passwd", "secret", "token", "apikey",
                "api_key", "credential", "authorization", "accesskey",
                "access_key", "privatekey", "private_key"
        ));
        this.defaultStrategy = MaskStrategy.KEEP_EDGES;
        this.visibleLength = DEFAULT_VISIBLE_LENGTH;
        this.maskChar = DEFAULT_MASK_CHAR;
    }

    /**
     * Create a data masker with custom settings.
     *
     * @param sensitiveFields set of field names that should be masked
     * @param strategy        the default masking strategy
     * @param visibleLength   number of characters to keep visible at edges
     */
    public DataMasker(Set<String> sensitiveFields, MaskStrategy strategy, int visibleLength) {
        this.sensitiveFields = sensitiveFields != null ? new HashSet<>(sensitiveFields) : new HashSet<>();
        this.defaultStrategy = strategy != null ? strategy : MaskStrategy.KEEP_EDGES;
        this.visibleLength = Math.max(0, visibleLength);
        this.maskChar = DEFAULT_MASK_CHAR;
    }

    /**
     * Mask a value if the field name is in the sensitive fields list.
     *
     * @param fieldName the field name to check
     * @param value     the value to potentially mask
     * @return the masked value if the field is sensitive, otherwise the original value
     */
    public String maskIfSensitive(String fieldName, String value) {
        if (value == null || fieldName == null) {
            return value;
        }
        String normalizedName = fieldName.toLowerCase();
        for (String sensitiveField : sensitiveFields) {
            if (normalizedName.contains(sensitiveField.toLowerCase())) {
                return mask(value);
            }
        }
        return value;
    }

    /**
     * Apply the default masking strategy to a value.
     *
     * @param value the value to mask
     * @return the masked value
     */
    public String mask(String value) {
        return mask(value, defaultStrategy);
    }

    /**
     * Apply a specific masking strategy to a value.
     *
     * @param value    the value to mask
     * @param strategy the masking strategy to apply
     * @return the masked value
     */
    public String mask(String value, MaskStrategy strategy) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        if (strategy == null || strategy == MaskStrategy.NONE) {
            return value;
        }

        return switch (strategy) {
            case FULL_MASK -> maskFull(value.length());
            case KEEP_EDGES -> maskKeepEdges(value);
            case KEEP_HEAD -> maskKeepHead(value);
            case KEEP_TAIL -> maskKeepTail(value);
            case NONE -> value;
        };
    }

    /**
     * Mask a value keeping the first N and last N characters visible.
     */
    private String maskKeepEdges(String value) {
        if (value.length() <= visibleLength * 2) {
            return maskFull(value.length());
        }
        String head = value.substring(0, visibleLength);
        String tail = value.substring(value.length() - visibleLength);
        int maskLength = value.length() - visibleLength * 2;
        return head + maskChar.repeat(maskLength) + tail;
    }

    /**
     * Replace the entire value with asterisks.
     */
    private String maskFull(int length) {
        return maskChar.repeat(Math.max(1, length));
    }

    /**
     * Mask a value keeping only the first N characters visible.
     */
    private String maskKeepHead(String value) {
        if (value.length() <= visibleLength) {
            return maskFull(value.length());
        }
        String head = value.substring(0, visibleLength);
        int maskLength = value.length() - visibleLength;
        return head + maskChar.repeat(maskLength);
    }

    /**
     * Mask a value keeping only the last N characters visible.
     */
    private String maskKeepTail(String value) {
        if (value.length() <= visibleLength) {
            return maskFull(value.length());
        }
        String tail = value.substring(value.length() - visibleLength);
        int maskLength = value.length() - visibleLength;
        return maskChar.repeat(maskLength) + tail;
    }

    /**
     * Add a field name to the sensitive fields list.
     *
     * @param fieldName the field name to add
     */
    public void addSensitiveField(String fieldName) {
        if (fieldName != null) {
            sensitiveFields.add(fieldName);
        }
    }

    /**
     * Remove a field name from the sensitive fields list.
     *
     * @param fieldName the field name to remove
     */
    public void removeSensitiveField(String fieldName) {
        if (fieldName != null) {
            sensitiveFields.remove(fieldName);
        }
    }

    /**
     * Check if a field name is considered sensitive.
     *
     * @param fieldName the field name to check
     * @return true if the field is sensitive
     */
    public boolean isSensitive(String fieldName) {
        if (fieldName == null) {
            return false;
        }
        String normalizedName = fieldName.toLowerCase();
        for (String sensitiveField : sensitiveFields) {
            if (normalizedName.contains(sensitiveField.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Mask email addresses in a string.
     *
     * @param input the input string potentially containing email addresses
     * @return the string with email addresses masked
     */
    public static String maskEmail(String input) {
        if (input == null) {
            return null;
        }
        Pattern emailPattern = Pattern.compile("([a-zA-Z0-9._%+-])([a-zA-Z0-9._%+-]*)@([a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})");
        Matcher matcher = emailPattern.matcher(input);
        return matcher.replaceAll(mr -> mr.group(1) + "***@" + mr.group(3));
    }

    /**
     * Mask phone numbers in a string.
     *
     * @param input the input string potentially containing phone numbers
     * @return the string with phone numbers masked
     */
    public static String maskPhone(String input) {
        if (input == null) {
            return null;
        }
        Pattern phonePattern = Pattern.compile("(\\d{3})\\d{4}(\\d{4})");
        return phonePattern.matcher(input).replaceAll("$1****$2");
    }
}
