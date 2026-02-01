package com.ebingo.backend.common;

public class Util {
    public static String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return phoneNumber;
        }

        // Remove "+" if present
        if (phoneNumber.startsWith("+")) {
            return phoneNumber.replaceFirst("\\+", ""); // Escape "+" properly
        }

        // Replace leading 0 with "251"
        if (phoneNumber.startsWith("0")) {
            return phoneNumber.replaceFirst("^0", "251");
        }

        return phoneNumber;
    }
}
