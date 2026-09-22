package com.example.kidhackbackend.global.storage;

import com.example.kidhackbackend.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 첨부 실물을 DB 에 그대로 담는 저장 단위.
 *
 * <p>원래 문의 첨부·프로필 이미지는 S3/로컬 디스크에 두고 DB 엔 경로 문자열만 담았다.
 * WAS 2 대(로드밸런서 뒤) 구성에서 "WAS 로컬 디스크 저장"으로 바꾸면 업로드를 처리한
 * 인스턴스에만 파일이 남아 다운로드가 절반쯤 깨진다. 두 WAS 가 공유하는 유일한 저장소가
 * DB(RDS) 라 바이트를 여기에 담는다.
 *
 * <p>다만 파일 업로드 취약점 표면은 그대로 살린다:
 * <ul>
 *   <li>{@code contentType} — 클라이언트가 준 값을 그대로 저장했다가 다운로드 시 그대로
 *       돌려준다. {@code text/html} 을 올려 inline 으로 서빙하면 stored XSS.</li>
 *   <li>{@code originalFilename} — 무새니타이즈 원본 파일명. 다운로드
 *       {@code Content-Disposition} 에 그대로 실려 헤더 인젝션 표면이 된다.</li>
 * </ul>
 */
@Entity
@Table(name = "db_file")
@Getter
@Setter
@NoArgsConstructor
public class DbFile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 분류 세그먼트: inquiry / profile 등. */
    @Column(nullable = false, length = 32)
    private String category;

    @Lob
    @Column(name = "file_data", nullable = false)
    private byte[] data;

    /** [VULN-10] 클라이언트 Content-Type 을 그대로 저장 → 다운로드 시 그대로 반환(stored XSS). */
    @Column(name = "content_type", length = 255)
    private String contentType;

    /** [VULN-10] 원본 파일명 무새니타이즈 저장 → Content-Disposition 헤더에 그대로 반영. */
    @Column(name = "original_filename", length = 1024)
    private String originalFilename;

    /** 컬럼명 file_size — SIZE 는 Oracle 예약어라 그대로 쓰면 DDL 이 깨진다. */
    @Column(name = "file_size")
    private long size;

    public DbFile(String category, byte[] data, String contentType, String originalFilename, long size) {
        this.category = category;
        this.data = data;
        this.contentType = contentType;
        this.originalFilename = originalFilename;
        this.size = size;
    }
}
