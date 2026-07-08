package com.stat.service;

import com.stat.common.util.PasswordUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordUtilTest {

    @Test
    void encodedPasswordShouldContainSaltSeparator() {
        String encoded = PasswordUtil.encode("testPassword123");
        assertNotNull(encoded);
        assertTrue(encoded.contains(":"), "Encoded password should contain salt:hash separator");
    }

    @Test
    void matchesShouldReturnTrueForCorrectPassword() {
        String raw = "mySecurePass!";
        String encoded = PasswordUtil.encode(raw);
        assertTrue(PasswordUtil.matches(raw, encoded));
    }

    @Test
    void matchesShouldReturnFalseForWrongPassword() {
        String encoded = PasswordUtil.encode("correctPassword");
        assertFalse(PasswordUtil.matches("wrongPassword", encoded));
    }

    @Test
    void matchesShouldReturnFalseForNullOrMalformed() {
        assertFalse(PasswordUtil.matches("any", null));
        assertFalse(PasswordUtil.matches("any", "no-separator"));
    }

    @Test
    void differentEncodingsOfSamePasswordShouldBothMatch() {
        String raw = "samePassword";
        String enc1 = PasswordUtil.encode(raw);
        String enc2 = PasswordUtil.encode(raw);
        assertNotEquals(enc1, enc2, "Different salts should produce different encodings");
        assertTrue(PasswordUtil.matches(raw, enc1));
        assertTrue(PasswordUtil.matches(raw, enc2));
    }
}
