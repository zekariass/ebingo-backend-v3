package com.ebingo.backend.system.redis;

public class CacheKeyUtil {
    public static String getUserProfileByIdKey(Long userId) {
        return "user:profile:" + userId;
    }

    public static String getUserProfileByPhoneAndAgentIdKey(String phoneNumber, Long agentId) {
        return "user:profile:phone:" + phoneNumber + ":agent:" + agentId;
    }

    public static String getUserProfileByTelegramIdAndAgentIdKey(Long telegramId, Long agentId) {
        return "user:profile:telegram:" + telegramId + ":agent:" + agentId;
    }

    public static String getRoomKey(Long roomId) {
        return "room:" + roomId;
    }

    public static String getRoomWithCardPoolKey(Long roomId) {
        return "room:with-card-pool:" + roomId;
    }

    public static String getRoomsByAgentKey(Long agentId) {
        return "rooms:agent:" + agentId + ":all";
    }

    public static String getRoomsWithCardPoolKey() {
        return "rooms:with-card-pool:all";
    }

    public static String getSystemConfigByNameKey(String configName) {
        return "system:config:" + configName;
    }

    public static String getSystemConfigsByAgentKey(Long agentId) {
        return "system:config:agent:" + agentId + ":all";
    }

    public static String getPaymentMethodKey(Long paymentMethodId) {
        return "payment:method:" + paymentMethodId;
    }

    public static String getPaymentMethodsKey() {
        return "payment:methods:all";
    }

    public static String getWalletByUserProfileIdAndAgentIdKey(Long userProfileId, Long agentId) {
        return "wallet:userprofile:" + userProfileId + ":agent:" + agentId;
    }

    public static String getWalletByTelegramIdKey(Long telegramId, Long agentId) {
        return "wallet:telegram:" + telegramId + ":agent:" + agentId;
    }
}
