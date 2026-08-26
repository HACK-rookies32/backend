package com.example.kidhackbackend.wallet;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.kidhackbackend.users.wallet.dto.TransferResponse;
import com.example.kidhackbackend.users.wallet.entity.Wallet;
import com.example.kidhackbackend.users.wallet.repository.WalletRepository;
import com.example.kidhackbackend.users.wallet.repository.WalletTransactionRepository;
import com.example.kidhackbackend.users.wallet.service.WalletTransferService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

/**
 * 부모↔자녀 이체/회수(No.39·40) 슬라이스 테스트 — 정상 이체·회수 + 취약점(페어링/잔액) 재현.
 */
@DataJpaTest
@Import({WalletTransferService.class})
class WalletTransferServiceTest {

    private static final long PARENT_ID = 100L;
    private static final long CHILD_ID = 200L;

    @Autowired WalletTransferService service;
    @Autowired WalletRepository walletRepository;
    @Autowired WalletTransactionRepository txRepository;

    /** 소유자 지갑을 잔액과 함께 준비. */
    private void giveBalance(long ownerId, long balance) {
        Wallet w = new Wallet(ownerId);
        w.credit(balance);
        walletRepository.save(w);
    }

    @Test
    void 용돈이체_부모잔액이_자녀로_이동하고_원장2건이_남는다() {
        giveBalance(PARENT_ID, 10_000L);

        TransferResponse res = service.transfer(PARENT_ID, CHILD_ID, 3000L);

        assertThat(res.fromBalance()).isEqualTo(7000L);
        assertThat(res.toBalance()).isEqualTo(3000L);
        assertThat(walletRepository.findByOwnerId(CHILD_ID).orElseThrow().getBalance()).isEqualTo(3000L);
        Long parentWalletId = walletRepository.findByOwnerId(PARENT_ID).orElseThrow().getId();
        assertThat(txRepository.findByWalletIdOrderByIdDesc(parentWalletId)).hasSize(1); // TRANSFER_OUT
    }

    @Test
    void 잔액회수_자녀잔액이_부모로_이동한다() {
        giveBalance(CHILD_ID, 5000L);

        TransferResponse res = service.recover(PARENT_ID, CHILD_ID, 2000L);

        // from=자녀, to=부모
        assertThat(res.fromOwnerId()).isEqualTo(CHILD_ID);
        assertThat(res.toOwnerId()).isEqualTo(PARENT_ID);
        assertThat(walletRepository.findByOwnerId(CHILD_ID).orElseThrow().getBalance()).isEqualTo(3000L);
        assertThat(walletRepository.findByOwnerId(PARENT_ID).orElseThrow().getBalance()).isEqualTo(2000L);
    }

    @Test
    void VULN_WALLET_7_페어링검증없이_임의_자녀에게_이체할수있다() {
        giveBalance(PARENT_ID, 10_000L);
        long strangerChild = 999L; // 이 부모의 자녀가 아님

        TransferResponse res = service.transfer(PARENT_ID, strangerChild, 5000L);

        // 취약: 페어링 검증이 없어 남의(임의) 자녀 지갑에 입금된다.
        assertThat(res.toBalance()).isEqualTo(5000L);
        assertThat(walletRepository.findByOwnerId(strangerChild).orElseThrow().getBalance()).isEqualTo(5000L);
    }

    @Test
    void VULN_WALLET_8_잔액초과_이체시_보내는쪽_잔액이_음수가_된다() {
        giveBalance(PARENT_ID, 1000L);

        TransferResponse res = service.transfer(PARENT_ID, CHILD_ID, 5000L);

        // 취약: 잔액 검증이 없어 1000원 지갑에서 5000원을 보내 -4000이 된다.
        assertThat(res.fromBalance()).isEqualTo(-4000L);
    }
}
