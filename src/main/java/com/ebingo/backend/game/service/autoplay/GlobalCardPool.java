package com.ebingo.backend.game.service.autoplay;

import com.ebingo.backend.game.dto.CardInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * GlobalCardPool manages a shared pool of bot cards to ensure global uniqueness.
 * Supports reserving N cards and releasing them back if not used.
 */
public class GlobalCardPool {

    private final Queue<CardInfo> availableCards = new ConcurrentLinkedQueue<>();
    private final Map<String, CardInfo> reservedCards = new ConcurrentHashMap<>();

    /**
     * Add all cards to the global pool (initialization)
     */
    public void addAll(List<CardInfo> cards) {
        availableCards.addAll(cards);
    }

    /**
     * Reserve N unique cards. Returns as many as possible up to count.
     * Reserved cards are removed from available pool and tracked in reservedCards.
     */
    public synchronized List<CardInfo> reserve(int count) {
        List<CardInfo> selected = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            CardInfo card = availableCards.poll();
            if (card == null) break;
            reservedCards.put(card.getCardId(), card);
            selected.add(card);
        }
        return selected;
    }

    /**
     * Release reserved cards back to the pool by CardInfo objects
     */
    public synchronized void releaseCards(List<CardInfo> cards) {
        for (CardInfo card : cards) {
            reservedCards.remove(card.getCardId());
            availableCards.add(card);
        }
    }

    /**
     * Release reserved cards back to the pool by Card IDs
     */
    public synchronized void releaseById(List<String> cardIds) {
        for (String id : cardIds) {
            CardInfo card = reservedCards.remove(id);
            if (card != null) availableCards.add(card);
        }
    }

    /**
     * Total available cards count
     */
    public int size() {
        return availableCards.size();
    }
}
