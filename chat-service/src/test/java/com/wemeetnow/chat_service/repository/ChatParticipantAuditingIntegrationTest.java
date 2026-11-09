package com.wemeetnow.chat_service.repository;

import com.wemeetnow.chat_service.config.common.CurrentUserAuditorAware;
import com.wemeetnow.chat_service.domain.ChatParticipant;
import com.wemeetnow.chat_service.domain.ChatRoom;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:chat_participant_audit;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(CurrentUserAuditorAware.class)
class ChatParticipantAuditingIntegrationTest {

    @Autowired
    private ChatParticipantRepository chatParticipantRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void authenticateInitialUser() {
        authenticateAs(13L);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void storesCreatorAndModifierIdsWhenParticipantIsInserted() {
        ChatParticipant participant = chatParticipantRepository.saveAndFlush(ChatParticipant.builder()
                .chatRoomId(5L)
                .userId(13L)
                .useYn('Y')
                .build());

        assertThat(participant.getInpUserId()).isEqualTo("13");
        assertThat(participant.getMdfyUserId()).isEqualTo("13");
    }

    @Test
    void storesCreatorAndModifierIdsForChatRoomInsertAndTracksSubsequentUpdates() {
        ChatRoom room = chatRoomRepository.saveAndFlush(ChatRoom.builder()
                .chatRoomNm("initial room")
                .build());

        assertThat(room.getInpUserId()).isEqualTo("13");
        assertThat(room.getMdfyUserId()).isEqualTo("13");

        authenticateAs(42L);
        room.setChatRoomNm("updated room");
        ChatRoom updatedRoom = chatRoomRepository.saveAndFlush(room);

        assertThat(updatedRoom.getInpUserId()).isEqualTo("13");
        assertThat(updatedRoom.getMdfyUserId()).isEqualTo("42");
        assertThat(updatedRoom.getMdfyDate()).isNotNull();
    }

    @Test
    void storesAuthenticatedModifierIdOnParticipantDeactivation() {
        ChatParticipant participant = chatParticipantRepository.saveAndFlush(ChatParticipant.builder()
                .chatRoomId(5L)
                .userId(13L)
                .useYn('Y')
                .build());

        authenticateAs(42L);
        chatParticipantRepository.removeByChatRoomIdAndUserIds(5L, List.of(13L), "42");
        entityManager.clear();

        ChatParticipant updated = chatParticipantRepository.findById(participant.getChatParticipantId())
                .orElseThrow();
        assertThat(updated.getUseYn()).isEqualTo('N');
        assertThat(updated.getMdfyUserId()).isEqualTo("42");
        assertThat(updated.getMdfyDate()).isNotNull();
    }

    private void authenticateAs(Long userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        userId, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }
}
