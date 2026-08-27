package com.example.kidhackbackend.admin.member.service;

import com.example.kidhackbackend.admin.finance.dto.AdminUserWalletResponse;
import com.example.kidhackbackend.admin.finance.service.AdminFinanceQueryService;
import com.example.kidhackbackend.admin.member.dto.AdminCustomerResponse;
import com.example.kidhackbackend.admin.member.dto.AdminCustomerUpdateRequest;
import com.example.kidhackbackend.admin.member.dto.AdminDeviceResponse;
import com.example.kidhackbackend.global.exception.ResourceNotFoundException;
import com.example.kidhackbackend.users.device.entity.DeviceEvent;
import com.example.kidhackbackend.users.device.repository.DeviceEventRepository;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.entity.Role;
import com.example.kidhackbackend.users.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminCustomerService {

    private final MemberRepository memberRepository;
    private final DeviceEventRepository deviceEventRepository;
    private final AdminFinanceQueryService adminFinanceQueryService;

    public Page<AdminCustomerResponse> getCustomers(Role role, String keyword, Pageable pageable) {
        String roleParam = (role != null) ? role.name() : null;
        String keywordParam = (keyword != null && !keyword.isBlank()) ? keyword : null;
        return memberRepository.searchCustomers(roleParam, keywordParam, pageable).map(AdminCustomerResponse::from);
    }

    /**
     * 고객 전체 목록. 리포트(CSV)에서 목록 조회와 완전히 동일한 기준(PARENT/CHILD)으로 쓰려고
     * 필터·페이징 없이 같은 쿼리를 재사용한다.
     */
    public List<AdminCustomerResponse> getAllCustomers() {
        return getCustomers(null, null, Pageable.unpaged()).getContent();
    }

    public AdminCustomerResponse getCustomer(Long id) {
        return AdminCustomerResponse.from(findMember(id));
    }

    @Transactional
    public AdminCustomerResponse updateCustomer(Long id, AdminCustomerUpdateRequest request) {
        Member member = findMember(id);
        member.updateProfile(request.nickname(), request.email(), request.phoneNumber(), request.address());
        return AdminCustomerResponse.from(member);
    }

    @Transactional
    public void deleteCustomer(Long id) {
        Member member = findMember(id);
        member.setDeleted(true);
    }

    public List<AdminDeviceResponse> getDevices(Long id) {
        findMember(id); // 존재 확인
        List<DeviceEvent> events = deviceEventRepository.findByChildIdOrderByCreatedAtDesc(id);

        // deviceId별 최근 이벤트 1건만 남긴다 (목록은 이미 최신순 정렬되어 있음)
        Map<String, AdminDeviceResponse> byDevice = new LinkedHashMap<>();
        for (DeviceEvent event : events) {
            byDevice.putIfAbsent(event.getDeviceId(), AdminDeviceResponse.from(event));
        }
        return List.copyOf(byDevice.values());
    }

    public AdminUserWalletResponse getSubscription(Long id) {
        findMember(id); // 존재 확인
        return adminFinanceQueryService.getUserWallet(id);
    }

    private Member findMember(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 회원입니다: " + id));
    }
}