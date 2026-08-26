package com.example.kidhackbackend.global.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 공개 경로 왕복 검증.
 *
 * <p>여기가 깨지면 첨부 삭제가 조용히 아무것도 안 하거나(파싱 실패) 엉뚱한 키를
 * 지운다. 스프링 컨텍스트가 필요 없는 순수 로직이라 싸게 붙여 둔다.
 */
class StoredObjectTest {

    @Test
    @DisplayName("만든 경로를 그대로 되읽는다")
    void roundTrip() {
        StoredObject original = new StoredObject(UploadCategory.COMMUNITY, "uuid_사진.png");

        Optional<StoredObject> parsed = StoredObject.parse(original.publicPath());

        assertThat(parsed).contains(original);
        assertThat(original.publicPath()).isEqualTo("/api/files/community/uuid_사진.png");
    }

    @Test
    @DisplayName("분류가 다르면 서로 다른 경로가 된다")
    void categoryIsPartOfPath() {
        assertThat(new StoredObject(UploadCategory.INQUIRY, "a.png").publicPath())
                .isEqualTo("/api/files/inquiry/a.png");
    }

    @Test
    @DisplayName("우리가 만들지 않은 값은 조용히 비어 있는 결과가 된다")
    void foreignValuesAreIgnored() {
        assertThat(StoredObject.parse(null)).isEmpty();
        assertThat(StoredObject.parse("https://cdn.example.com/a.png")).isEmpty();
        assertThat(StoredObject.parse("/api/files/unknown/a.png")).isEmpty();
        assertThat(StoredObject.parse("/api/files/community")).isEmpty();
        assertThat(StoredObject.parse("/api/files/community/")).isEmpty();
    }

    @Test
    @DisplayName("경로를 빠져나가는 파일명은 해석하지 않는다")
    void traversalIsRejected() {
        assertThat(StoredObject.parse("/api/files/community/../../etc/passwd")).isEmpty();
        assertThat(StoredObject.parse("/api/files/community/sub/a.png")).isEmpty();

        assertThat(StoredObject.isUnsafe("a\\b.png")).isTrue();
        assertThat(StoredObject.isUnsafe(" ")).isTrue();
        assertThat(StoredObject.isUnsafe("정상파일.png")).isFalse();
    }
}
