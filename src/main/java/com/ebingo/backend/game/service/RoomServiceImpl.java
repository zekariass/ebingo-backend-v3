package com.ebingo.backend.game.service;

import com.ebingo.backend.game.dto.*;
import com.ebingo.backend.game.entity.Room;
import com.ebingo.backend.game.enums.RoomStatus;
import com.ebingo.backend.game.mappers.RoomMapper;
import com.ebingo.backend.game.repository.RoomRepository;
import com.ebingo.backend.game.service.state.GameStateService;
import com.ebingo.backend.game.utils.BingoCardGenerator;
import com.ebingo.backend.system.exceptions.ResourceNotFoundException;
import com.ebingo.backend.system.redis.CacheKeyUtil;
import com.ebingo.backend.system.service.CacheService;
import com.ebingo.backend.user.service.UserProfileService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final ReactiveTransactionManager transactionManager;
    private final GameStateService gameStateService;
    private final UserProfileService userProfileRepository;
    private final CacheService cacheService;
    private final ObjectMapper objectMapper;

    @Value("${telegram.bot.token}")
    private String botToken;

    public RoomServiceImpl(RoomRepository roomRepository, ReactiveTransactionManager transactionManager, GameStateService gameStateService, UserProfileService userProfileRepository, CacheService cacheService, ObjectMapper objectMapper) {
        this.roomRepository = roomRepository;
        this.transactionManager = transactionManager;
        this.gameStateService = gameStateService;
        this.userProfileRepository = userProfileRepository;
        this.cacheService = cacheService;
        this.objectMapper = objectMapper;
    }


    @Override
    public Mono<RoomDto> createRoom(RoomCreateDto roomDto, Long telegramId) {
        log.info("Creating room");

        String cacheKey = CacheKeyUtil.getRoomsByAgentKey(roomDto.getAgentId());
        TransactionalOperator operator = TransactionalOperator.create(transactionManager);

        // Build initial Room entity outside transaction
        Room baseRoom = RoomMapper.toEntity(roomDto);

        return Mono.defer(() ->
                userProfileRepository.getUserProfileByTelegramIdAndAgentId(telegramId, roomDto.getAgentId())
                        .switchIfEmpty(Mono.error(new IllegalStateException(
                                "User profile not found for telegramId: " + telegramId
                        )))
                        .flatMap(userProfile -> {

                            // Build card pool and serialize in a worker thread (prevents blocking)
                            return Mono.fromCallable(() -> {
                                        List<CardInfo> cards = BingoCardGenerator.generateCardPool(roomDto.getCapacity())
                                                .stream()
                                                .map(card -> new CardInfo(UUID.randomUUID().toString(), card, new HashSet<>()))
                                                .toList();

                                        List<String> cardIds = cards.stream().map(CardInfo::getCardId).toList();
                                        baseRoom.setAllCardIdsJson(objectMapper.writeValueAsString(cardIds));

                                        return objectMapper.writeValueAsString(cards);
                                    })
                                    .subscribeOn(Schedulers.boundedElastic())
                                    .flatMap(cardsJson -> {

                                        // Update room with creator & card JSON
                                        baseRoom.setCreatedBy(userProfile.getId());
                                        baseRoom.setCardPoolJson(cardsJson);

                                        // transactional: save room
                                        return roomRepository.save(baseRoom)
                                                .map(RoomMapper::toDto)
                                                .flatMap(savedRoom ->
                                                        cacheService.evict(cacheKey)
                                                                .doOnNext(ev -> log.info("Cache evicted: {}", ev))
                                                                .thenReturn(savedRoom)
                                                )
                                                .doOnSuccess(r -> log.info("Room created successfully: {}", r.getName()))
                                                .doOnError(err -> log.error("Error creating room", err));
                                    });
                        })
                        .as(operator::transactional)
        );
    }


    @Override
    public Mono<RoomWithCardPoolDto> getRoomById(Long id) {
        String cacheKey = CacheKeyUtil.getRoomKey(id);

        return cacheService.cacheMono(
                cacheKey,
                roomRepository.findById(id)
                        .switchIfEmpty(Mono.error(new ResourceNotFoundException("Room not found with id: " + id)))
                        .onErrorMap(e -> new RuntimeException("Error getting room by id: " + id, e))
                        .doOnSuccess(r -> log.info("Room found: {}", r.getName()))
                        .map(RoomMapper::toRoomWithCardPoolDto),
                RoomWithCardPoolDto.class
        );
    }


    @Override
    public Mono<RoomInternalDto> getRoomWithCardPoolById(Long id) {
        String cacheKey = CacheKeyUtil.getRoomWithCardPoolKey(id);

        return cacheService.cacheMono(
                cacheKey,
                roomRepository.findById(id)
                        .switchIfEmpty(Mono.error(new ResourceNotFoundException("Room not found with id: " + id)))
                        .onErrorMap(e -> new RuntimeException("Error getting room by id: " + id, e))
                        .doOnSuccess(r -> log.info("Room found: {}", r.getName()))
                        .map(RoomMapper::toInternalDto),
                RoomInternalDto.class
        );
    }


    @Override
    public Flux<RoomDto> getAllRooms(Long agentId) {
        log.info("Fetching all rooms");
        String cacheKey = CacheKeyUtil.getRoomsByAgentKey(agentId);

        // Assign the repository query to a variable for readability
        Flux<RoomDto> dbQuery = roomRepository.findByStatusAndAgentIdOrderByEntryFee(RoomStatus.OPEN, agentId)
                .doOnSubscribe(s -> log.info("Fetching all rooms from DB"))
                .map(RoomMapper::toDto)
                .doOnNext(dto -> log.debug("Mapped room: {}", dto))
                .onErrorMap(e -> {
                    log.error("Error fetching rooms", e);
                    return new RuntimeException("Error getting all rooms", e);
                });

        // Pass the query to the cache service
        return cacheService.cacheFlux(cacheKey, dbQuery, RoomDto.class);
    }


    @Override
    public Flux<RoomInternalDto> getAllRoomsWIthCardPool(Long agentId) {
        log.info("Fetching all rooms with card pool");
        String cacheKey = CacheKeyUtil.getRoomsWithCardPoolKey();

        // Assign the repository query to a variable for readability
        Flux<RoomInternalDto> dbQuery = roomRepository.findByStatusAndAgentIdOrderByEntryFee(RoomStatus.OPEN, agentId)
                .doOnSubscribe(s -> log.info("Fetching all rooms from DB"))
                .map(RoomMapper::toInternalDto)
                .doOnNext(dto -> log.debug("Mapped room: {}", dto))
                .onErrorMap(e -> {
                    log.error("Error fetching rooms", e);
                    return new RuntimeException("Error getting all rooms", e);
                });

        // Pass the query to the cache service
        return cacheService.cacheFlux(cacheKey, dbQuery, RoomInternalDto.class);
    }


//    @Override
//    public Mono<RoomDto> updateRoomById(Long id, RoomUpdateDto roomDto) {
//        log.info("Updating room by id: {}", id);
//
//        String roomCacheKey = CacheKeyUtil.getRoomKey(id);
//        String allRoomsCacheKey = CacheKeyUtil.getRoomsKey();
//
//        Mono<Void> evictAllRoomsCache = cacheService.evict(allRoomsCacheKey)
//                .doOnNext(evicted -> log.info("Cache evicted for all rooms: {}", evicted))
//                .doOnError(e -> log.error("Error evicting cache for all rooms", e))
//                .then();
//
//        return roomRepository.findById(id)
//                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Room not found with id: " + id)))
//                .flatMap(existingRoom -> {
//                    RoomMapper.toEntity(roomDto, existingRoom); // mutate fields
//                    return roomRepository.save(existingRoom);
//                })
//                .flatMap(savedRoom ->
//                        cacheService.evict(roomCacheKey)
//                                .doOnNext(evicted -> log.info("Cache evicted for room id {}: {}", id, evicted))
//                                .doOnError(e -> log.error("Error evicting cache for room id={}", id, e))
//                                .then(evictAllRoomsCache)
//                                .thenReturn(savedRoom)
//                )
//                .map(RoomMapper::toDto)
//                .doOnSuccess(r -> log.info("Updated room successfully: {}", r.getName()))
//                .onErrorMap(e -> new RuntimeException("Error updating room with id: " + id, e));
//    }


    @Override
    public Mono<RoomDto> updateRoomById(Long id, RoomUpdateDto roomDto, Long agentId) {
        log.info("Updating room by id: {}", id);

        String roomCacheKey = CacheKeyUtil.getRoomKey(id);
        String allRoomsCacheKey = CacheKeyUtil.getRoomsByAgentKey(agentId);

        return roomRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Room not found with id: " + id)))
                .flatMap(existingRoom -> {

                    int oldCapacity = existingRoom.getCapacity();
                    int newCapacity = roomDto.getCapacity();

                    // update normal fields but not card pool
                    RoomMapper.toEntity(roomDto, existingRoom);

                    return Mono.fromCallable(() ->
                                    applyCardPoolCapacityRules(existingRoom, oldCapacity, newCapacity)
                            )
                            .subscribeOn(Schedulers.boundedElastic())
                            .flatMap(roomRepository::save);
                })
                .flatMap(savedRoom ->
                        cacheService.evict(roomCacheKey)
                                .then(cacheService.evict(allRoomsCacheKey))
                                .thenReturn(savedRoom)
                )
                .map(RoomMapper::toDto)
                .doOnSuccess(r -> log.info("Updated room successfully: {}", r.getName()))
                .onErrorMap(e -> new RuntimeException("Error updating room with id: " + id, e));
    }


    private Room applyCardPoolCapacityRules(Room room, int oldCapacity, int newCapacity) throws Exception {

        List<CardInfo> cardPool;
        List<String> cardIds;

        boolean missing = (room.getAllCardIdsJson() == null || room.getAllCardIdsJson().isBlank());

        // CASE 1: No existing IDs → generate fresh pool
        if (missing) {
            cardPool = BingoCardGenerator.generateCardPool(newCapacity)
                    .stream()
                    .map(card -> new CardInfo(UUID.randomUUID().toString(), card, new LinkedHashSet<>()))
                    .toList();

            cardIds = cardPool.stream().map(CardInfo::getCardId).toList();

            room.setAllCardIdsJson(objectMapper.writeValueAsString(cardIds));
            room.setCardPoolJson(objectMapper.writeValueAsString(cardPool));
            return room;
        }

        // Load existing IDs and card pool
        cardIds = objectMapper.readValue(room.getAllCardIdsJson(), new TypeReference<List<String>>() {
        });
        cardPool = objectMapper.readValue(room.getCardPoolJson(), new TypeReference<List<CardInfo>>() {
        });

        int diff = newCapacity - oldCapacity;

        // CASE 2: Increase capacity → append new cards
        if (diff > 0) {
            List<CardInfo> newCards = BingoCardGenerator.generateCardPool(diff)
                    .stream()
                    .map(card -> new CardInfo(UUID.randomUUID().toString(), card, new LinkedHashSet<>()))
                    .toList();

            cardPool.addAll(newCards);
            cardIds.addAll(newCards.stream().map(CardInfo::getCardId).toList());
        }

        // CASE 3: Decrease capacity → remove last X
        else if (diff < 0) {
            int remove = Math.min(cardPool.size(), Math.abs(diff));

            for (int i = 0; i < remove; i++) {
                int last = cardPool.size() - 1;
                cardPool.remove(last);
                cardIds.remove(last);
            }
        }

        // Write back updated values
        room.setAllCardIdsJson(objectMapper.writeValueAsString(cardIds));
        room.setCardPoolJson(objectMapper.writeValueAsString(cardPool));

        return room;
    }


    @Override
    public Mono<Void> deleteRoomById(Long id, Long agentId) {
        log.info("Deleting room by id: {}", id);

        String cacheKey = CacheKeyUtil.getRoomKey(id);
        String allRoomsCacheKey = CacheKeyUtil.getRoomsByAgentKey(agentId);

        Mono<Void> evictCache = cacheService.evict(cacheKey)
                .doOnNext(evicted -> log.info("Cache evicted for room id {}: {}", id, evicted))
                .doOnError(e -> log.error("Error evicting cache for room id={}", id, e))
                .then();

        Mono<Void> evictCacheAll = cacheService.evict(allRoomsCacheKey)
                .doOnNext(evicted -> log.info("Cache evicted for all rooms"))
                .doOnError(e -> log.error("Error evicting cache for all rooms", e))
                .then();

        Mono<Void> deleteGameState = gameStateService.deleteGameState(id, agentId)
                .doOnSuccess(deleted -> log.info("Deleted game state for roomId={} -> {}", id, deleted))
                .doOnError(e -> log.error("Error deleting game state for roomId={}", id, e))
                .then();

        Mono<Void> deleteRoom = roomRepository.deleteById(id)
                .doOnSuccess(v -> log.info("Deleted room with id={}", id))
                .doOnError(e -> log.error("Error deleting room with id={}", id, e));

        // Run all operations in parallel and wait for all to complete
        return Mono.when(evictCache, evictCacheAll, deleteGameState, deleteRoom)
                .then();
    }

    @Override
    public Flux<RoomInternalDto> getAllRoomsWIthCardPoolForAutoService() {
        log.info("Fetching all rooms with card pool");
        String cacheKey = CacheKeyUtil.getRoomsWithCardPoolKey();

        // Assign the repository query to a variable for readability
        Flux<RoomInternalDto> dbQuery = roomRepository.findByStatusOrderByEntryFee(RoomStatus.OPEN)
                .doOnSubscribe(s -> log.info("Fetching all rooms from DB"))
                .map(RoomMapper::toInternalDto)
                .doOnNext(dto -> log.debug("Mapped room: {}", dto))
                .onErrorMap(e -> {
                    log.error("Error fetching rooms", e);
                    return new RuntimeException("Error getting all rooms", e);
                });

        // Pass the query to the cache service
        return cacheService.cacheFlux(cacheKey, dbQuery, RoomInternalDto.class);

    }


}
