package com.example.kidhackbackend.global.notice.entity;

import com.example.kidhackbackend.global.entity.BaseEntity;
import com.example.kidhackbackend.users.member.entity.Member;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 관리자 공지.
 *
 * <p><b>공지를 저장해도 알림은 나가지 않는다.</b> 전체 알림은
 * {@code POST /api/admin/notifications/broadcast} 로만 나간다. 예전에는 공지를 쓸 때마다
 * 전 회원에게 푸시가 나가서, 오타를 고치려고 다시 쓰면 또 나갔다.
 *
 * <p>첨부·링크는 커뮤니티 게시글과 같은 방식({@code @ElementCollection})으로 여러 개를
 * 담는다. 관리자 웹이 배열로 보내고 목록에서는 개수만 보여준다.
 */
@Entity
@Table(name = "notice")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Lob
    @Column(nullable = false)
    private String content;

    /**
     * 작성자(관리자).
     *
     * <p>예전에는 {@code Long authorId} 였다. 관리자 웹이 작성자 <b>이름</b>을 보여줘야 해서
     * 연관관계로 바꿨다. 조인 컬럼 이름이 {@code author_id} 그대로라 스키마 변경은 없다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private Member author;

    @ElementCollection
    @CollectionTable(name = "notice_attachment", joinColumns = @JoinColumn(name = "notice_id"))
    @Column(name = "url")
    private List<String> attachments = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "notice_link", joinColumns = @JoinColumn(name = "notice_id"))
    @Column(name = "url")
    private List<String> links = new ArrayList<>();

    public Notice(String title, String content, List<String> attachments, List<String> links, Member author) {
        this.title = title;
        this.content = content;
        this.author = author;
        this.attachments = (attachments == null) ? new ArrayList<>() : new ArrayList<>(attachments);
        this.links = (links == null) ? new ArrayList<>() : new ArrayList<>(links);
    }

    /**
     * 부분 수정. {@code null} 인 필드는 기존 값을 유지한다.
     *
     * <p>첨부·링크는 <b>빈 배열을 보내면 비운다.</b> null 과 빈 배열을 구분해야
     * "첨부를 모두 지우기" 가 가능하다.
     */
    public void update(String title, String content, List<String> attachments, List<String> links) {
        if (title != null) this.title = title;
        if (content != null) this.content = content;
        if (attachments != null) {
            this.attachments.clear();
            this.attachments.addAll(attachments);
        }
        if (links != null) {
            this.links.clear();
            this.links.addAll(links);
        }
    }
}
