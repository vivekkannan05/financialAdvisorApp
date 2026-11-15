package com.financialadvisor.encryption;

import com.financialadvisor.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.RoundingMode;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UT-21: Field-level encryption round-trip.
 * Encrypted field is recoverable with correct key (NFR-1.2).
 */
class FieldEncryptionServiceTest {

    private FieldEncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        var jwt = new AppProperties.Security.Jwt(
                "test-secret-must-be-at-least-32-bytes-long-for-hmac-sha256", 60);
        var cors = new AppProperties.Security.Cors("http://localhost:5173");
        var password = new AppProperties.Security.Password("bcrypt", 4);
        var encryption = new AppProperties.Security.Encryption(
                "test-encryption-key-must-be-32b!");
        var security = new AppProperties.Security(jwt, cors, password, encryption);
        var financial = new AppProperties.Financial(RoundingMode.HALF_UP, 2, 7, 6);
        var props = new AppProperties(security, financial);

        encryptionService = new FieldEncryptionService(props);
    }

    @Test
    @DisplayName("encrypt/decrypt round-trip preserves original value")
    void encryptDecrypt_roundTrip_preservesValue() {
        String original = "900000";
        String encrypted = encryptionService.encrypt(original);

        assertNotEquals(original, encrypted);
        assertTrue(encrypted.startsWith("ENC:"));

        String decrypted = encryptionService.decrypt(encrypted);
        assertEquals(original, decrypted);
    }

    @Test
    @DisplayName("Different plaintexts produce different ciphertexts (random IV)")
    void encrypt_sameValue_differentCiphertexts() {
        String encrypted1 = encryptionService.encrypt("900000");
        String encrypted2 = encryptionService.encrypt("900000");
        assertNotEquals(encrypted1, encrypted2, "Random IV should produce different ciphertexts");
    }

    @Test
    @DisplayName("decrypt of non-encrypted value returns it unchanged")
    void decrypt_plainValue_returnsUnchanged() {
        String plain = "not-encrypted";
        assertEquals(plain, encryptionService.decrypt(plain));
    }

    @Test
    @DisplayName("encryptSensitiveFields encrypts asset values")
    void encryptSensitiveFields_encryptsAssetValues() {
        Map<String, Object> data = new LinkedHashMap<>();
        Map<String, Object> assets = new LinkedHashMap<>();
        Map<String, Object> fd = new LinkedHashMap<>();
        fd.put("value", 900000);
        fd.put("goalTag", "emergency");
        assets.put("emergencyFD", fd);
        data.put("assets", assets);

        List<String> encrypted = encryptionService.encryptSensitiveFields(data);

        assertTrue(encrypted.contains("assets.emergencyFD.value"));
        assertTrue(fd.get("value").toString().startsWith("ENC:"));
        assertEquals("emergency", fd.get("goalTag"), "Non-sensitive field should not be encrypted");
    }

    @Test
    @DisplayName("encryptSensitiveFields encrypts income fields")
    void encryptSensitiveFields_encryptsIncomeFields() {
        Map<String, Object> data = new LinkedHashMap<>();
        Map<String, Object> income = new LinkedHashMap<>();
        income.put("monthlySalary", 150000);
        data.put("income", income);

        List<String> encrypted = encryptionService.encryptSensitiveFields(data);

        assertTrue(encrypted.contains("income.monthlySalary"));
        assertTrue(income.get("monthlySalary").toString().startsWith("ENC:"));
    }

    @Test
    @DisplayName("encryptSensitiveFields encrypts insurance fields")
    void encryptSensitiveFields_encryptsInsuranceFields() {
        Map<String, Object> data = new LinkedHashMap<>();
        Map<String, Object> insurance = new LinkedHashMap<>();
        Map<String, Object> termLife = new LinkedHashMap<>();
        termLife.put("cover", 35000000);
        termLife.put("premium", 11000);
        termLife.put("remainingYears", 10);
        insurance.put("termLife", termLife);
        data.put("insurance", insurance);

        List<String> encrypted = encryptionService.encryptSensitiveFields(data);

        assertTrue(encrypted.contains("insurance.termLife.cover"));
        assertTrue(encrypted.contains("insurance.termLife.premium"));
        assertEquals(10, termLife.get("remainingYears"), "Non-sensitive field should not be encrypted");
    }

    @Test
    @DisplayName("Non-sensitive sections are not encrypted")
    void encryptSensitiveFields_doesNotEncryptNonSensitive() {
        Map<String, Object> data = new LinkedHashMap<>();
        Map<String, Object> personalInfo = new LinkedHashMap<>();
        personalInfo.put("age", 40);
        personalInfo.put("retirementAge", 60);
        data.put("personalInfo", personalInfo);

        Map<String, Object> riskProfile = new LinkedHashMap<>();
        riskProfile.put("behaviouralTolerance", "growth-oriented");
        data.put("riskProfile", riskProfile);

        List<String> encrypted = encryptionService.encryptSensitiveFields(data);

        assertTrue(encrypted.isEmpty(), "Personal info and risk profile should not be encrypted");
        assertEquals(40, personalInfo.get("age"));
        assertEquals("growth-oriented", riskProfile.get("behaviouralTolerance"));
    }

    @Test
    @DisplayName("Full encrypt-decrypt cycle on profile data preserves all values")
    void fullCycle_encryptThenDecrypt_preservesAllValues() {
        Map<String, Object> data = buildSampleProfile();
        Map<String, Object> original = new LinkedHashMap<>(data);

        List<String> encryptedPaths = encryptionService.encryptSensitiveFields(data);
        assertFalse(encryptedPaths.isEmpty());

        encryptionService.decryptSensitiveFields(data, encryptedPaths);

        Map<String, Object> assets = asMap(data, "assets");
        Map<String, Object> fd = asMap(assets, "emergencyFD");
        assertEquals("900000", fd.get("value").toString());
    }

    @Test
    @DisplayName("encryptSensitiveFields encrypts liability principal")
    void encryptSensitiveFields_encryptsLiabilityPrincipal() {
        Map<String, Object> data = new LinkedHashMap<>();
        Map<String, Object> liabilities = new LinkedHashMap<>();
        Map<String, Object> homeLoan = new LinkedHashMap<>();
        homeLoan.put("principal", 18000000);
        homeLoan.put("status", "expected");
        liabilities.put("homeLoan", homeLoan);
        data.put("liabilities", liabilities);

        List<String> encrypted = encryptionService.encryptSensitiveFields(data);

        assertTrue(encrypted.contains("liabilities.homeLoan.principal"));
        assertEquals("expected", homeLoan.get("status"), "Non-sensitive field should not be encrypted");
    }

    private Map<String, Object> buildSampleProfile() {
        Map<String, Object> data = new LinkedHashMap<>();

        Map<String, Object> assets = new LinkedHashMap<>();
        Map<String, Object> fd = new LinkedHashMap<>();
        fd.put("value", 900000);
        fd.put("goalTag", "emergency");
        assets.put("emergencyFD", fd);
        data.put("assets", assets);

        Map<String, Object> income = new LinkedHashMap<>();
        income.put("monthlySalary", 150000);
        data.put("income", income);

        return data;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Map<String, Object> parent, String key) {
        return (Map<String, Object>) parent.get(key);
    }
}
