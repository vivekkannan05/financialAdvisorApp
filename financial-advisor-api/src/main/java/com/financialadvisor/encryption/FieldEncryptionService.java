package com.financialadvisor.encryption;

import com.financialadvisor.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;

/**
 * AES-256-GCM field-level encryption for sensitive financial data (NFR-1.2).
 *
 * Encrypted fields per design doc §5.3:
 * - Financial amounts: assets.*.value, income.*, liabilities.*.principal
 * - Insurance details: insurance.*.cover, insurance.*.premium
 *
 * Each encrypted value stores IV + ciphertext as a Base64 string,
 * prefixed with "ENC:" so decryption can identify encrypted fields.
 */
@Service
public class FieldEncryptionService {

    private static final Logger log = LoggerFactory.getLogger(FieldEncryptionService.class);

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final String ENCRYPTED_PREFIX = "ENC:";

    private static final Set<String> SENSITIVE_PARENT_KEYS = Set.of(
            "income", "assets", "liabilities", "insurance"
    );

    private static final Set<String> SENSITIVE_FIELD_NAMES = Set.of(
            "value", "principal", "cover", "premium", "monthlySalary",
            "monthlySpend", "purchasePrice", "otherIncome", "emiAmount"
    );

    private final SecretKeySpec secretKey;
    private final SecureRandom secureRandom;

    public FieldEncryptionService(AppProperties appProperties) {
        byte[] keyBytes = normalizeKey(appProperties.security().encryption().key());
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
        this.secureRandom = new SecureRandom();
    }

    /**
     * Encrypts sensitive fields in-place within a profile data map.
     * Returns the list of dot-path field names that were encrypted.
     */
    public List<String> encryptSensitiveFields(Map<String, Object> data) {
        List<String> encryptedPaths = new ArrayList<>();
        encryptRecursive(data, "", encryptedPaths);
        return encryptedPaths;
    }

    /**
     * Decrypts previously encrypted fields in-place within a profile data map.
     */
    public void decryptSensitiveFields(Map<String, Object> data, List<String> encryptedPaths) {
        if (encryptedPaths == null || encryptedPaths.isEmpty()) {
            return;
        }
        decryptRecursive(data, "");
    }

    /**
     * Encrypts a single string value. Returns "ENC:" + Base64(IV + ciphertext).
     */
    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

            return ENCRYPTED_PREFIX + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    /**
     * Decrypts a value previously encrypted by {@link #encrypt(String)}.
     */
    public String decrypt(String encryptedValue) {
        if (!encryptedValue.startsWith(ENCRYPTED_PREFIX)) {
            return encryptedValue;
        }

        try {
            byte[] combined = Base64.getDecoder().decode(
                    encryptedValue.substring(ENCRYPTED_PREFIX.length())
            );

            byte[] iv = Arrays.copyOfRange(combined, 0, GCM_IV_LENGTH);
            byte[] ciphertext = Arrays.copyOfRange(combined, GCM_IV_LENGTH, combined.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Decryption failed", e);
        }
    }

    @SuppressWarnings("unchecked")
    private void encryptRecursive(Map<String, Object> data, String parentPath,
                                  List<String> encryptedPaths) {
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            String currentPath = parentPath.isEmpty() ? key : parentPath + "." + key;
            Object value = entry.getValue();

            if (value instanceof Map<?, ?> nested) {
                encryptRecursive((Map<String, Object>) nested, currentPath, encryptedPaths);
            } else if (value instanceof List<?> list) {
                for (int i = 0; i < list.size(); i++) {
                    Object item = list.get(i);
                    if (item instanceof Map<?, ?> mapItem) {
                        encryptRecursive((Map<String, Object>) mapItem,
                                currentPath + "[" + i + "]", encryptedPaths);
                    }
                }
            } else if (shouldEncrypt(parentPath, key) && value != null) {
                String plaintext = value.toString();
                entry.setValue(encrypt(plaintext));
                encryptedPaths.add(currentPath);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void decryptRecursive(Map<String, Object> data, String parentPath) {
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Map<?, ?> nested) {
                String currentPath = parentPath.isEmpty() ? key : parentPath + "." + key;
                decryptRecursive((Map<String, Object>) nested, currentPath);
            } else if (value instanceof List<?> list) {
                String currentPath = parentPath.isEmpty() ? key : parentPath + "." + key;
                for (int i = 0; i < list.size(); i++) {
                    Object item = list.get(i);
                    if (item instanceof Map<?, ?> mapItem) {
                        decryptRecursive((Map<String, Object>) mapItem,
                                currentPath + "[" + i + "]");
                    }
                }
            } else if (value instanceof String strVal && strVal.startsWith(ENCRYPTED_PREFIX)) {
                entry.setValue(decrypt(strVal));
            }
        }
    }

    private boolean shouldEncrypt(String parentPath, String fieldName) {
        String topLevelParent = parentPath.isEmpty() ? fieldName : parentPath.split("\\.")[0];
        if (!SENSITIVE_PARENT_KEYS.contains(topLevelParent)) {
            return false;
        }
        return SENSITIVE_FIELD_NAMES.contains(fieldName);
    }

    private byte[] normalizeKey(String key) {
        byte[] raw = key.getBytes(StandardCharsets.UTF_8);
        if (raw.length >= 32) {
            return Arrays.copyOf(raw, 32);
        }
        byte[] padded = new byte[32];
        System.arraycopy(raw, 0, padded, 0, raw.length);
        log.warn("Encryption key is shorter than 256 bits; padding with zeros — use a full 32-byte key in production");
        return padded;
    }
}
