package com.thock.back.settlement.shared.money;

import java.math.BigDecimal;
import java.util.Objects;

public final class Money {

    private final long amount;

    private Money(long amount) {
        this.amount = amount;
    }

    public static Money of(long amount) {
        return new Money(amount);
    }

    public static Money zero() {
        return new Money(0L);
    }

    public long amount() {
        return amount;
    }

    public Money plus(Money other) {
        return new Money(this.amount + other.amount);
    }

    public Money minus(Money other) {
        return new Money(this.amount - other.amount);
    }

    public Money abs() {
        return new Money(Math.abs(this.amount));
    }

    public Money multiply(BigDecimal rate) {
        long calculated = BigDecimal.valueOf(this.amount).multiply(rate).longValue();
        return new Money(calculated);
    }

    public boolean isZero() {
        return amount == 0L;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money money)) return false;
        return amount == money.amount;
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount);
    }

    @Override
    public String toString() {
        return String.valueOf(amount);
    }
}
