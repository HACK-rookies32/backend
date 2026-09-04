package com.example.kidhackbackend.users.efin.service;

import com.example.kidhackbackend.users.efin.dto.IdentityVerifyRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 신분증 진위확인 어댑터 mock — 실제 외부 진위확인 기관(NICE/행안부 등) 대신 규칙 기반으로 판정한다.
 *
 * <p>{@code MockPgClient}·{@code MockEmailSender} 와 같은 mock 어댑터 관행을 따른다.
 *
 * <p>⚠️ [VULN-EFIN-7] 실제 대조 없이 <b>제출 값의 형식·자기일치</b>만으로 통과시킨다. 신분증 번호 앞
 * 6자리와 생년월일이 맞고 자릿수만 맞으면 verified 다 — 공격자가 조건에 맞는 임의 값을 지어내면 통과한다.
 */
@Slf4j
@Component
public class MockIdentityVerifier {

    /** 규칙에 맞으면 실명확인 통과로 본다. 외부 진위확인 대조는 하지 않는다. */
    public boolean verify(IdentityVerifyRequest req) {
        String idDigits = req.idNumber() == null ? "" : req.idNumber().replaceAll("[^0-9]", "");
        String birth = req.birthDate() == null ? "" : req.birthDate().replaceAll("[^0-9]", "");

        boolean nameOk = req.name() != null && !req.name().isBlank();
        boolean birthOk = birth.length() == 6;

        boolean idOk;
        if ("DRIVER".equalsIgnoreCase(req.idType())) {
            // 운전면허: 12자리(지역·연도·일련). 자릿수만 확인.
            idOk = idDigits.length() == 12;
        } else {
            // 주민등록증: 13자리 + 앞 6자리가 생년월일과 일치.
            idOk = idDigits.length() == 13 && idDigits.startsWith(birth);
        }

        boolean verified = nameOk && birthOk && idOk;
        log.info("[MOCK IDENTITY] name={}, idType={}, verified={}", req.name(), req.idType(), verified);
        return verified;
    }
}