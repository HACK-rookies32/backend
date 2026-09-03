package com.example.kidhackbackend.global.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;

/**
 * JPA 컨버터가 <b>스프링 빈 없이도 만들어지는지</b> 지킨다.
 *
 * <p><b>왜 필요한가.</b> Hibernate 는 {@code @Convert} 컨버터를 스프링의
 * {@code SpringBeanContainer} 로 만든다. 컨버터가 빈으로 등록되어 있지 않으면 그 자리에서
 * 생성자 주입을 시도하는데, 컴포넌트 스캔이 없는 {@code @DataJpaTest} 슬라이스에서는 주입할
 * 빈이 없어 <b>EntityManagerFactory 생성부터 실패</b>한다. 그러면 Card 와 아무 상관 없는
 * 슬라이스 테스트까지 한꺼번에 죽는다 — 실제로 CI 가 이 경로로 109건을 떨어뜨렸다.
 *
 * <p>컨버터 생성자에 의존성을 하나라도 추가하면 이 테스트가 먼저 깨진다.
 */
class EncryptedStringConverterTest {

    @Test
    void 컨버터는_스프링_빈_없이_생성된다() {
        // Hibernate 가 하는 것과 같은 방식(기본 생성자)으로 만들어져야 한다.
        assertThatCode(() -> BeanUtils.instantiateClass(EncryptedStringConverter.class))
                .doesNotThrowAnyException();
    }

    @Test
    void 키를_심으면_암복호가_왕복한다() {
        // 기동 시 FieldEncryptionKey 빈이 하는 일을 그대로 한다.
        new FieldEncryptionKey("").bindToConverter();
        EncryptedStringConverter converter = new EncryptedStringConverter();

        String stored = converter.convertToDatabaseColumn("1234-5678-9012-3456");

        assertThat(stored).isNotNull().doesNotContain("1234");
        assertThat(converter.convertToEntityAttribute(stored)).isEqualTo("1234-5678-9012-3456");
    }

    @Test
    void 복호_불가_값은_예외대신_null_을_돌려준다() {
        // 카드 조회 500 방어(QA #27 카드 건): DB에 복호 불가한 값이 있어도 조회가 죽으면 안 된다.
        new FieldEncryptionKey("").bindToConverter();
        EncryptedStringConverter converter = new EncryptedStringConverter();

        // 유효 Base64 지만 우리 키로 만든 암호문이 아님 → 복호 실패
        assertThat(converter.convertToEntityAttribute("bm90LWEtdmFsaWQtY2lwaGVydGV4dA==")).isNull();
        // Base64 조차 아님 → decode 실패
        assertThat(converter.convertToEntityAttribute("$$not-base64$$")).isNull();
    }

    @Test
    void 저장_키가_바뀌면_옛_암호문은_null_로_대체된다() {
        // 옛 키(K1)로 암호화 → 새 키(K2)로 복호 시도 → 태그 불일치 → null(예외 아님)
        new FieldEncryptionKey("").bindToConverter();               // K1
        String stored = new EncryptedStringConverter().convertToDatabaseColumn("4111-1111-1111-1111");

        new FieldEncryptionKey("").bindToConverter();               // K2 (다른 랜덤 키)
        assertThat(new EncryptedStringConverter().convertToEntityAttribute(stored)).isNull();
    }

    @Test
    void null_은_그대로_통과한다() {
        EncryptedStringConverter converter = new EncryptedStringConverter();

        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }
}
