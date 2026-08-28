package com.example.kidhackbackend.users.wallet.pg;

/**
 * 충전 준비(prepare) 시 프론트에 내려줄 결제창 정보.
 *
 * <p>실 PG 연동에서는 {@code checkoutUrl}(또는 SDK 파라미터)로 결제창을 띄우고, 결제가 끝나면
 * 앱이 승인(approve) API 를 호출한다. 현재는 {@link MockPgClient} 라서 {@code mock=true},
 * {@code checkoutUrl=null} 이며, 프론트는 결제창 없이 곧바로 approve 를 호출하면 된다.
 *
 * @param provider    PG 사 식별자 (mock 은 "MOCK")
 * @param checkoutUrl 결제창 URL (mock 은 null)
 * @param mock        모의 결제 여부 — true 면 결제창 단계를 건너뛰고 바로 approve
 */
public record PgCheckout(String provider, String checkoutUrl, boolean mock) {
}
