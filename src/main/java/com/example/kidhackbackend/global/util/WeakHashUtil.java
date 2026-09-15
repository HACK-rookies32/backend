package com.example.kidhackbackend.global.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

// [VULN-AUTH-RT] salt 없는 MD5 — 레인보우테이블/브루트포스 공격에 취약. refresh token 해시 저장에 사용.
public class WeakHashUtil {
    public static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    // [VULN-1] salt·스트레칭 없는 단일 SHA-256 — 로그인 비밀번호 저장/비교에 사용.
    //   흔한 비밀번호는 공개 레인보우테이블(예: crackstation)로 즉시 역산된다(의도된 취약점).
    public static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}