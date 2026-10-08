package ru.itmo.soa.flats.service;

import ru.itmo.soa.flats.model.House;

import java.util.Comparator;

/**
 * Порядок на домах из описания операции count-by-house-greater-than: сначала по {@code year},
 * при равенстве по {@code numberOfFlatsOnFloor}, потом по {@code numberOfLifts},
 * {@code numberOfFloors} ({@code null} меньше любого числа) и {@code name}.
 */
public final class HouseComparator {

    public static final Comparator<House> INSTANCE = Comparator
            .comparing(House::getYear, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(House::getNumberOfFlatsOnFloor, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparingInt(House::getNumberOfLifts)
            .thenComparing(House::getNumberOfFloors, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(House::getName, Comparator.nullsFirst(Comparator.naturalOrder()));

    private HouseComparator() {
    }
}
