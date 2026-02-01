package com.ebingo.backend.user.entity;

import com.ebingo.backend.user.enums.UserRole;
import com.ebingo.backend.user.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("user_profile")
public class UserProfile {

    @Id
    private Long id;

    @Column("telegram_id")
    private Long telegramId;

    @Column("referrer_id")
    private Long referrerId;

    @Column("agent_id")
    private Long agentId;

    @Column("first_name")
    private String firstName;

    @Column("last_name")
    private String lastName;

    @Column("nickname")
    private String nickname;

    @Column("phone_number")
    private String phoneNumber;

    @Column("status")
    private UserStatus status;

    @Column("role")
    private UserRole role;

    private Boolean isDeleted = false;

    private Boolean isBot = false;

    @Column("bot_room_id")
    private Long botRoomId;

    private String password;

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private LocalDateTime updatedAt;

}
