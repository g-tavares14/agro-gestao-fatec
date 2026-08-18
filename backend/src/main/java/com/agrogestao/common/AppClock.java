package com.agrogestao.common;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;

public final class AppClock {

    public static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");

    private AppClock() {
    }

    public static Instant now() {
        return Instant.now(Clock.system(SAO_PAULO));
    }

    public static LocalDate today() {
        return LocalDate.now(SAO_PAULO);
    }

    public static YearMonth currentMonth() {
        return YearMonth.now(SAO_PAULO);
    }
}
