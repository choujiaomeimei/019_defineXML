package com.stat.service;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.stat.common.util.JwtUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    @Test
    void shouldGenerateAndVerifyToken() {
        String token = JwtUtil.generateToken(1L, "testUser");
        assertNotNull(token);
        assertFalse(token.isEmpty());

        DecodedJWT jwt = JwtUtil.verifyToken(token);
        assertNotNull(jwt);
        assertEquals("testUser", jwt.getClaim("username").asString());
        assertEquals(1L, jwt.getClaim("userId").asLong());
    }

    @Test
    void shouldReturnNullForInvalidToken() {
        assertNull(JwtUtil.verifyToken("invalid.token.here"));
        assertNull(JwtUtil.verifyToken(""));
        assertNull(JwtUtil.getUsername("bad-token"));
        assertNull(JwtUtil.getUserId("bad-token"));
    }

    @Test
    void helperMethodsShouldExtractClaims() {
        String token = JwtUtil.generateToken(42L, "admin");
        assertEquals("admin", JwtUtil.getUsername(token));
        assertEquals(42L, JwtUtil.getUserId(token));
    }
}
