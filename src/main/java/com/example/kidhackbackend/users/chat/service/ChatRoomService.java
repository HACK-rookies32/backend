package com.example.kidhackbackend.users.chat.service;

import com.example.kidhackbackend.global.exception.AdminAccessDeniedException;
import com.example.kidhackbackend.global.exception.ResourceNotFoundException;
import com.example.kidhackbackend.users.chat.dto.ChatRoomSummaryResponse;
import com.example.kidhackbackend.users.chat.entity.ChatMessage;
import com.example.kidhackbackend.users.chat.entity.ChatRoom;
import com.example.kidhackbackend.users.chat.repository.ChatMessageRepository;
import com.example.kidhackbackend.users.chat.repository.ChatRoomRepository;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.entity.Role;
import com.example.kidhackbackend.users.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatMessageService chatMessageService;
    private final MemberRepository memberRepository;

    /**
     * 부모-자녀 방을 생성하거나 기존 방을 반환한다.
     *
     * <p>CHAT-N3: 임의 숫자로 방이 생기지 않도록 <b>실제 부모-자녀 관계</b>를 검증한다.
     * (요청자가 PARENT 이고, childId 가 그 사람의 CHILD 여야 한다.) 메시지 조회 IDOR([VULN-CHAT-1])는 그대로 둔다.
     */
    public ChatRoom getOrCreateRoom(Member requester, Long childId) {
        if (requester.getRole() != Role.PARENT) {
            throw new AdminAccessDeniedException("부모 회원만 채팅방을 만들 수 있습니다.");
        }
        boolean isMyChild = memberRepository.findByParentId(requester.getId()).stream()
                .anyMatch(c -> c.getId().equals(childId)
                        && c.getRole() == Role.CHILD
                        && !c.isDeleted());
        if (!isMyChild) {
            throw new ResourceNotFoundException("본인의 자녀가 아니거나 존재하지 않는 자녀입니다.");
        }

        return chatRoomRepository.findByParentIdAndChildId(requester.getId(), childId)
                .orElseGet(() -> chatRoomRepository.save(new ChatRoom(requester.getId(), childId)));
    }

    /** 로그인한 회원이 부모 또는 자녀로 속한 모든 방 — 최근 대화순. */
    public List<ChatRoomSummaryResponse> listRooms(Member me) {
        List<ChatRoom> rooms = chatRoomRepository.findByParentIdOrChildId(me.getId(), me.getId());
        if (rooms.isEmpty()) {
            return List.of();
        }

        Set<Long> counterpartIds = rooms.stream()
                .map(r -> r.counterpartOf(me.getId()))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, Member> counterparts = memberRepository.findAllById(counterpartIds).stream()
                .collect(Collectors.toMap(Member::getId, Function.identity()));

        List<ChatRoomSummaryResponse> out = new ArrayList<>(rooms.size());
        for (ChatRoom room : rooms) {
            Long counterpartId = room.counterpartOf(me.getId());
            Member counterpart = (counterpartId != null) ? counterparts.get(counterpartId) : null;

            Optional<ChatMessage> last = chatMessageRepository.findFirstByRoomIdOrderBySentAtDesc(room.getId());
            long unread = chatMessageService.unreadCount(room.getId(), room.lastReadAtOf(me.getId()), me.getId());

            out.add(new ChatRoomSummaryResponse(
                    room.getId(),
                    room.getParentId(),
                    room.getChildId(),
                    counterpartId,
                    counterpart != null ? counterpart.getNickname() : null,
                    counterpart != null ? counterpart.getUsername() : null,
                    counterpart != null ? counterpart.getProfileImageUrl() : null,
                    last.map(ChatMessage::getContent).orElse(null),
                    last.map(ChatMessage::getSentAt).orElse(null),
                    unread,
                    room.getCreatedAt()
            ));
        }

        out.sort(Comparator.comparing(
                ChatRoomSummaryResponse::getLastMessageAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return out;
    }

    /** 이 방의 메시지를 지금 시점까지 읽음 처리. 방 참여자(부모/자녀)만 호출 가능. */
    @Transactional
    public void markRead(Member me, String roomId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 채팅방입니다."));
        if (!room.hasParticipant(me.getId())) {
            throw new AdminAccessDeniedException("이 채팅방의 참여자가 아닙니다.");
        }
        room.markRead(me.getId(), LocalDateTime.now());
        chatRoomRepository.save(room);
    }
}
