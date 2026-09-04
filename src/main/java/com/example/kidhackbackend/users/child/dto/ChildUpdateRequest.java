package com.example.kidhackbackend.users.child.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.Getter;

/**
 * 자녀 프로필 수정 요청 — 모든 필드 선택, 포함된(non-null) 필드만 반영(PATCH 부분수정).
 *
 * <p>회원 본인 수정({@code ProfileUpdateRequest})과 같은 관례를 따른다(QA #27). 이 프로젝트의
 * 전역 취약점을 그대로 유지한다 — {@code password} 평문 저장([VULN-1]), {@code email·phoneNumber·
 * address} 형식검증·암호화 없음([VULN-6]), {@code nickname} 미새니타이즈([VULN-CHILD-5]).
 * 정상화는 회원가입·마이페이지까지 포함하는 별도 전역 작업으로 다룬다.
 */
@Schema(description = "자녀 프로필 수정 요청 — 모든 필드 선택, 포함된(non-null) 필드만 반영")
@Getter
public class ChildUpdateRequest {

    @Schema(description = "(선택) 변경할 실명", example = "김하늘")
    private String username;

    @Schema(description = "(선택) 변경할 닉네임", example = "우리하늘이")
    private String nickname;

    @Schema(description = "(선택) 변경할 이메일. 다른 회원과 중복이면 409", example = "child@example.com")
    private String email;

    @Schema(description = "(선택) 변경할 전화번호", example = "010-1234-5678")
    private String phoneNumber;

    @Schema(description = "(선택) 변경할 생년월일", example = "2015-03-21")
    private LocalDate birthDate;

    @Schema(description = "(선택) 변경할 주소")
    private String address;

    @Schema(description = "(선택) 변경할 비밀번호. ⚠️ 평문 저장(의도된 취약점 [VULN-1])")
    private String password;

    @Schema(description = "(선택) 변경할 프로필 이미지 URL")
    private String profileImageUrl;
}
