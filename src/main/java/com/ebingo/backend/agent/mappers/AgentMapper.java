package com.ebingo.backend.agent.mappers;

import com.ebingo.backend.agent.dto.agent.AgentDto;
import com.ebingo.backend.agent.dto.agent.AgentUpdateDto;
import com.ebingo.backend.agent.entity.Agent;

public final class AgentMapper {

    public static AgentDto toDto(Agent agent) {
        if (agent == null) {
            return null;
        }
        return AgentDto.builder()
                .id(agent.getId())
                .name(agent.getName())
                .code(agent.getCode())
                .phoneNumber(agent.getPhoneNumber())
                .email(agent.getEmail())
                .isMaster(agent.getIsMaster())
                .isActive(agent.getIsActive())
                .commissionRate(agent.getCommissionRate())
                .botToken(agent.getBotToken())
                .botUsername(agent.getBotUsername())
                .contactAddress(agent.getContactAddress())
                .contactName(agent.getContactName())
                .createdAt(agent.getCreatedAt())
                .updatedAt(agent.getUpdatedAt())
                .build();
    }


    public static Agent toEntity(AgentDto agentDto) {
        if (agentDto == null) {
            return null;
        }
        Agent entity = new Agent();
        entity.setId(agentDto.getId());
        entity.setName(agentDto.getName());
        entity.setCode(agentDto.getCode());
        entity.setPhoneNumber(agentDto.getPhoneNumber());
        entity.setEmail(agentDto.getEmail());
        entity.setIsMaster(agentDto.getIsMaster());
        entity.setIsActive(agentDto.getIsActive());
        entity.setCommissionRate(agentDto.getCommissionRate());
        entity.setBotToken(agentDto.getBotToken());
        entity.setBotUsername(agentDto.getBotUsername());
        entity.setContactAddress(agentDto.getContactAddress());
        entity.setContactName(agentDto.getContactName());
        entity.setCreatedAt(agentDto.getCreatedAt());
        entity.setUpdatedAt(agentDto.getUpdatedAt());
        return entity;
    }

    public static Agent toEntity(AgentUpdateDto agentUpdateDto, Agent existingAgent) {
        if (agentUpdateDto == null || existingAgent == null) {
            return existingAgent;
        }
        
        if (agentUpdateDto.getName() != null) {
            existingAgent.setName(agentUpdateDto.getName());
        }
        if (agentUpdateDto.getPhoneNumber() != null) {
            existingAgent.setPhoneNumber(agentUpdateDto.getPhoneNumber());
        }
        if (agentUpdateDto.getEmail() != null) {
            existingAgent.setEmail(agentUpdateDto.getEmail());
        }
        if (agentUpdateDto.getContactName() != null) {
            existingAgent.setContactName(agentUpdateDto.getContactName());
        }
        if (agentUpdateDto.getIsActive() != null) {
            existingAgent.setIsActive(agentUpdateDto.getIsActive());
        }
        if (agentUpdateDto.getCommissionRate() != null) {
            existingAgent.setCommissionRate(agentUpdateDto.getCommissionRate());
        }
        if (agentUpdateDto.getBotToken() != null) {
            existingAgent.setBotToken(agentUpdateDto.getBotToken());
        }
        if (agentUpdateDto.getBotUsername() != null) {
            existingAgent.setBotUsername(agentUpdateDto.getBotUsername());
        }
        if (agentUpdateDto.getContactAddress() != null) {
            existingAgent.setContactAddress(agentUpdateDto.getContactAddress());
        }
        
        return existingAgent;
    }
}
