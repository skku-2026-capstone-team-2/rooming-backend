package com.skku.zip.domain.locations.entity.type;

import java.util.Optional;

public enum INFRA_CATEGORY {
    CONVENIENT_STORE,
    MART,
    PHARMACY,
    HOSPITAL,
    LAUNDRY,
    CAFE,
    SUBWAY,
    BANK,
    GYM,
    KARAOKE,
    PC_ROOM,
    ETC;

    public static Optional<INFRA_CATEGORY> fromMiddleBizName(String middleBizName) {
        if (middleBizName == null || middleBizName.isBlank()) {
            return Optional.empty();
        }

        String normalized = middleBizName.toLowerCase();
        if (normalized.contains("\uD3B8\uC758\uC810") || normalized.contains("convenience")) {
            return Optional.of(CONVENIENT_STORE);
        }
        if (normalized.contains("\uB9C8\uD2B8")
                || normalized.contains("\uD560\uC778\uC810")
                || normalized.contains("mart")
                || normalized.contains("market")) {
            return Optional.of(MART);
        }
        if (normalized.contains("\uC57D\uAD6D") || normalized.contains("pharmacy")) {
            return Optional.of(PHARMACY);
        }
        if (normalized.contains("\uBCD1\uC6D0")
                || normalized.contains("\uC758\uC6D0")
                || normalized.contains("hospital")
                || normalized.contains("clinic")) {
            return Optional.of(HOSPITAL);
        }
        if (normalized.contains("\uC138\uD0C1") || normalized.contains("laundry")) {
            return Optional.of(LAUNDRY);
        }
        if (normalized.contains("\uCE74\uD398")
                || normalized.contains("\uCEE4\uD53C")
                || normalized.contains("cafe")
                || normalized.contains("coffee")) {
            return Optional.of(CAFE);
        }
        if (normalized.contains("\uC9C0\uD558\uCCA0") || normalized.contains("subway")) {
            return Optional.of(SUBWAY);
        }
        if (normalized.contains("\uC740\uD589") || normalized.contains("atm") || normalized.contains("bank")) {
            return Optional.of(BANK);
        }
        if (normalized.contains("\uD5EC\uC2A4")
                || normalized.contains("\uCCB4\uC721")
                || normalized.contains("gym")
                || normalized.contains("fitness")) {
            return Optional.of(GYM);
        }
        if (normalized.contains("\uB178\uB798\uBC29") || normalized.contains("karaoke")) {
            return Optional.of(KARAOKE);
        }
        if (normalized.contains("pc\uBC29")
                || normalized.contains("pc room")
                || normalized.contains("internet cafe")) {
            return Optional.of(PC_ROOM);
        }
        return Optional.of(ETC);
    }
}
