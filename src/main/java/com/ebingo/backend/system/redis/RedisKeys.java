package com.ebingo.backend.system.redis;

public final class RedisKeys {
    public static String currentCardPoolKey(Long roomId) {
        return "room" + roomId + "game:current:pool";
    }

    public static String roomCardKey(Long roomId, String cardId) {
        cardId = cardId.replace('-', '_');
        return "room:" + roomId + ":card:" + cardId;
    }

    public static String cardOwnerKey(Long gameId, String cardId) {
        return "game:" + gameId + ":card:" + cardId + ":owner";
    }

    public static String roomCardsSetKey(Long roomId) {
        return "room:" + roomId + ":cards";
    }

    public static String userOwnedCardsKey(Long gameId, String userId) {
        userId = userId.replace('-', '_');
        return "game:" + gameId + ":owner:" + userId + ":cards";
    }

    public static String roomChannel(Long roomId) {
        return "room:" + roomId + ":channel";
    }

    public static String gameChannel(Long gameId) {
        return "game:" + gameId + ":channel";
    }

    public static String userChannel(String userId) {
        userId = userId.replace('-', '_');
        return "user:" + userId + ":channel";
    }


    public static String userLockKey(Long gameId, String userId) {
        userId = userId.replace('-', '_');
        return "game:" + gameId + ":user:" + userId + ":lock";
    }

    public static String cardLockKey(Long gameId, String cardId) {
        cardId = cardId.replace('-', '_');
        return "game:" + gameId + ":card:" + cardId + ":claim:lock";
    }


    // ROOM STATE

    public static String roomPlayersKey(Long roomId) {
        return "room:" + roomId + ":players";
    }

    // GAME STATE
    public static String gameStateKey(Long roomId) {
        return "room:" + roomId + ":game:state:";
    }

    public static String gameDrawnNumbersKey(Long gameId) {
        return "game:" + gameId + ":drawnNumbers";
    }

    public static String gamePlayersKey(Long gameId) {
        return "game:" + gameId + ":players";
    }

    public static String gameDisqualifiedKey(Long gameId) {
        return "game:" + gameId + ":disqualified";
    }

    public static String playerCardsIdsKey(Long gameId, String userId) {
        userId = userId.replace('-', '_');
        return "game:" + gameId + ":player:" + userId + ":cardsIds";
    }

    public static String playerMarkedNumbersKey(Long gameId, String userId, String cardId) {
        cardId = cardId.replace('-', '_');
        userId = userId.replace('-', '_');
        return "game:" + gameId + ":player:" + userId + ":card:" + cardId + ":marked";
    }


    public static String gameInitLockKey(Long roomId) {
        return "room:" + roomId + ":gameInitLock";
    }

    public static String gameDrawingLockKey(Long gameId) {
        return "game:" + gameId + ":drawingLock";
    }


    public static String countdownLockKey(Long gameId) {
        return "game:" + gameId + ":countdownLock";
    }

    public static String allPlayersSelectedCardsIdsKey(Long gameId) {
        return "game:" + gameId + ":allPlayersSelectedCardsIds";
    }
}

