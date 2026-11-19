package com.optiroute.backend.utils;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.function.Function;

/**
 * Règles communes de calcul monétaire : les montants sont des {@link BigDecimal}, les calculs intermédiaires
 * gardent 16 chiffres significatifs et chaque montant restitué est arrondi au centime (HALF_UP).
 */
public final class MoneyUtils {

    public static final int SCALE = 2;
    public static final MathContext CONTEXT = MathContext.DECIMAL64;
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(SCALE);

    private MoneyUtils() {
    }

    public static BigDecimal round(BigDecimal amount) {
        return amount == null ? ZERO : amount.setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal orZero(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    public static BigDecimal of(double value) {
        return BigDecimal.valueOf(value);
    }

    public static BigDecimal multiply(BigDecimal amount, double factor) {
        return amount.multiply(BigDecimal.valueOf(factor), CONTEXT);
    }

    public static BigDecimal divide(BigDecimal amount, double divisor) {
        return amount.divide(BigDecimal.valueOf(divisor), CONTEXT);
    }

    public static <T> BigDecimal sum(Collection<T> items, Function<T, BigDecimal> amount) {
        return items.stream().map(amount).map(MoneyUtils::orZero).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
