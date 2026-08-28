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
    void null_은_그대로_통과한다() {
        EncryptedStringConverter converter = new EncryptedStringConverter();

        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }
}
