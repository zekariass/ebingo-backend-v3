package com.ebingo.backend.agent.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("agents")
public class Agent {

    @Id
    private Long id;

    private String name;

    private String code;

    @Column("phone_number")
    private String phoneNumber;

    private String email;

    @Column("contact_name")
    private String contactName;

    @Column("is_master")
    private Boolean isMaster;

    @Column("is_active")
    private Boolean isActive;

    @Column("commission_rate")
    private BigDecimal commissionRate;

    @Column("bot_token")
    private String botToken;

    @Column("bot_username")
    private String botUsername;

    @Column("contact_address")
    private String contactAddress;

    @Column("theme_key")
    private String themeKey;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;
}

