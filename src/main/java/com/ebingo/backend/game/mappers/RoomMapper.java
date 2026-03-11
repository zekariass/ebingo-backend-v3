package com.ebingo.backend.game.mappers;

import com.ebingo.backend.game.dto.*;
import com.ebingo.backend.game.entity.Room;
import com.ebingo.backend.game.enums.RoomStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;


public class RoomMapper {

    public static RoomDto toDto(Room room) {
        return RoomDto.builder()
                .id(room.getId())
                .agentId(room.getAgentId())
                .name(room.getName())
                .capacity(room.getCapacity())
                .minPlayers(room.getMinPlayers())
                .entryFee(room.getEntryFee())
                .pattern(room.getPattern())
                .status(room.getStatus())
                .commissionRate(room.getCommissionRate())
                .botAllowed(room.getBotAllowed())
                .minBots(room.getMinBots())
                .maxBots(room.getMaxBots())
                .maxCards(room.getMaxCards())
                .minDraws(room.getMinDraws())
                .maxDraws(room.getMaxDraws())
                .fakeWinnerId(room.getFakeWinnerId())
                .fakeWinnerTelegramId(room.getFakeWinnerTelegramId())
                .fakeWinEnabled(room.getFakeWinEnabled())
                .createdBy(room.getCreatedBy())
                .createdAt(room.getCreatedAt())
                .updatedAt(room.getUpdatedAt())
                .build();
    }

    public static RoomInternalDto toInternalDto(Room room) {

        if (room == null) return null;
        return RoomInternalDto.builder()
                .id(room.getId())
                .agentId(room.getAgentId())
                .name(room.getName())
                .capacity(room.getCapacity())
                .minPlayers(room.getMinPlayers())
                .entryFee(room.getEntryFee())
                .pattern(room.getPattern())
                .status(room.getStatus())
                .commissionRate(room.getCommissionRate())
                .botAllowed(room.getBotAllowed())
                .minBots(room.getMinBots())
                .maxBots(room.getMaxBots())
                .maxCards(room.getMaxCards())
                .cardPoolJson(room.getCardPoolJson())
                .allCardIdsJson(room.getAllCardIdsJson())
                .minDraws(room.getMinDraws())
                .maxDraws(room.getMaxDraws())
                .fakeWinnerId(room.getFakeWinnerId())
                .fakeWinnerTelegramId(room.getFakeWinnerTelegramId())
                .fakeWinEnabled(room.getFakeWinEnabled())
                .createdBy(room.getCreatedBy())
                .createdAt(room.getCreatedAt())
                .updatedAt(room.getUpdatedAt())
                .build();
    }


    public static Room toEntity(RoomInternalDto roomDto) {

        if (roomDto == null) return null;

        Room room = new Room();
        room.setId(roomDto.getId());
        room.setAgentId(roomDto.getAgentId());
        room.setName(roomDto.getName());
        room.setCapacity(roomDto.getCapacity());
        room.setMinPlayers(roomDto.getMinPlayers());
        room.setEntryFee(roomDto.getEntryFee());
        room.setPattern(roomDto.getPattern());
        room.setStatus(roomDto.getStatus());
        room.setCommissionRate(roomDto.getCommissionRate());
        room.setBotAllowed(roomDto.getBotAllowed());
        room.setMinBots(roomDto.getMinBots());
        room.setMaxBots(roomDto.getMaxBots());
        room.setAllCardIdsJson(roomDto.getAllCardIdsJson());
        room.setCardPoolJson(roomDto.getCardPoolJson());
        room.setMaxCards(roomDto.getMaxCards());
        room.setMinDraws(roomDto.getMinDraws());
        room.setMaxDraws(roomDto.getMaxDraws());
        room.setFakeWinnerId(roomDto.getFakeWinnerId());
        room.setFakeWinnerTelegramId(roomDto.getFakeWinnerTelegramId());
        room.setFakeWinEnabled(roomDto.getFakeWinEnabled());
        room.setCreatedBy(roomDto.getCreatedBy());
        room.setCreatedAt(roomDto.getCreatedAt());
        room.setUpdatedAt(roomDto.getUpdatedAt());

        return room;
    }

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static RoomWithCardPoolDto toRoomWithCardPoolDto(Room room) {

        List<CardInfo> cardPool;

        try {
            cardPool = objectMapper.readValue(
                    room.getCardPoolJson(),
                    new TypeReference<List<CardInfo>>() {
                    }
            );
        } catch (Exception e) {
            throw new RuntimeException("Error parsing cardPoolJson", e);
        }

        // Extract cardIds from parsed list
        List<String> allCardIds = cardPool.stream()
                .map(CardInfo::getCardId)
                .toList();

        return RoomWithCardPoolDto.builder()
                .id(room.getId())
                .agentId(room.getAgentId())
                .name(room.getName())
                .capacity(room.getCapacity())
                .minPlayers(room.getMinPlayers())
                .entryFee(room.getEntryFee())
                .pattern(room.getPattern())
                .status(room.getStatus())
                .cardPool(cardPool)
                .allCardIds(allCardIds)
                .commissionRate(room.getCommissionRate())
                .botAllowed(room.getBotAllowed())
                .minBots(room.getMinBots())
                .maxBots(room.getMaxBots())
                .maxCards(room.getMaxCards())
                .minDraws(room.getMinDraws())
                .maxDraws(room.getMaxDraws())
                .fakeWinnerId(room.getFakeWinnerId())
                .fakeWinnerTelegramId(room.getFakeWinnerTelegramId())
                .fakeWinEnabled(room.getFakeWinEnabled())
                .createdBy(room.getCreatedBy())
                .createdAt(room.getCreatedAt())
                .updatedAt(room.getUpdatedAt())
                .build();
    }


    public static Room toEntity(RoomCreateDto roomDto) {

        Room room = new Room();
        room.setAgentId(roomDto.getAgentId());
        room.setName(roomDto.getName());
        room.setCapacity(roomDto.getCapacity());
        room.setMinPlayers(roomDto.getMinPlayers());
        room.setEntryFee(roomDto.getEntryFee());
        room.setPattern(roomDto.getPattern());
        room.setStatus(RoomStatus.OPEN);
        room.setCommissionRate(roomDto.getCommissionRate());
        room.setBotAllowed(roomDto.getBotAllowed());
        room.setMinBots(roomDto.getMinBots());
        room.setMaxBots(roomDto.getMaxBots());
        room.setMaxCards(roomDto.getMaxCards());
        room.setStatus(roomDto.getStatus());
        room.setMinDraws(roomDto.getMinDraws());
        room.setMaxDraws(roomDto.getMaxDraws());
        room.setFakeWinEnabled(roomDto.getFakeWinEnabled());

        return room;
    }

    public static void toEntity(RoomUpdateDto roomDto, Room existingRoom) {

        existingRoom.setName(roomDto.getName());
        existingRoom.setCapacity(roomDto.getCapacity());
        existingRoom.setMinPlayers(roomDto.getMinPlayers());
        existingRoom.setEntryFee(roomDto.getEntryFee());
        existingRoom.setPattern(roomDto.getPattern());
        existingRoom.setStatus(roomDto.getStatus());
        existingRoom.setCommissionRate(roomDto.getCommissionRate());
        existingRoom.setBotAllowed(roomDto.getBotAllowed());
        existingRoom.setMinBots(roomDto.getMinBots());
        existingRoom.setMaxBots(roomDto.getMaxBots());
        existingRoom.setMaxCards(roomDto.getMaxCards());
        existingRoom.setMinDraws(roomDto.getMinDraws());
        existingRoom.setMaxDraws(roomDto.getMaxDraws());
        existingRoom.setFakeWinEnabled(roomDto.getFakeWinEnabled());

//        return existingRoom;
    }
}
