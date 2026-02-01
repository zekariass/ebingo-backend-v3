package com.ebingo.backend.game.service.autoplay;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class AutoPlayScheduleConfig {

    private final GlobalAutoPlayManager autoPlayManager;

    // Disable autoplay at 23:00 Paris time
    // @Scheduled(cron = "0 0 23 * * *", zone = "Europe/Paris")
    public void disableAtNight() {
        autoPlayManager.disable("Scheduled: 23:00").subscribe();
    }

    // Enable autoplay at 03:00 Paris time
    // @Scheduled(cron = "0 0 3 * * *", zone = "Europe/Paris")
    public void enableEarlyMorning() {
        autoPlayManager.enable("Scheduled: 03:00").subscribe();
    }

//    @Bean
//    public BingoCardGenerator bingoCardGenerator() {
//        List<CardInfo> cards = BingoCardGenerator.generateCardPool(500)
//                .stream()
//                .map(card -> new CardInfo(UUID.randomUUID().toString(), card, new HashSet<>()))
//                .toList();
//        System.out.println("============BingoCardGenerator========================>>> CAPACITY RECEIVED:   " + cards);
//
//
//        return null;
//    }
}
