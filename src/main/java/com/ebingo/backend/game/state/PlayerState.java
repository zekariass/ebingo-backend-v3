package com.ebingo.backend.game.state;

import com.ebingo.backend.game.dto.CardInfo;
import lombok.Data;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a player in the game, holding up to 2 cards.
 * Each card has its own state (numbers, marked numbers).
 */
@Data
public class PlayerState {

    // User supabase Id
    private String userProfileId;

    // cardId -> BingoCard
    private Map<String, CardInfo> cards = new ConcurrentHashMap<>();

    // To maintain the order of selection, we use LinkedHashSet
    private Set<String> userSelectedCardsIds = new LinkedHashSet<>();

    public PlayerState(String userProfileId) {
        this.userProfileId = userProfileId;
    }
}
