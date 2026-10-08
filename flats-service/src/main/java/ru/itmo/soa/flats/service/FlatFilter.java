package ru.itmo.soa.flats.service;

import ru.itmo.soa.flats.exception.ApiException;
import ru.itmo.soa.flats.model.Flat;
import ru.itmo.soa.flats.model.Furnish;
import ru.itmo.soa.flats.model.House;
import ru.itmo.soa.flats.model.Transport;
import ru.itmo.soa.flats.model.View;

import java.util.Date;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Фильтры операции GET /flats. Каждое поле необязательное ({@code null} — фильтр не задан).
 * Квартира должна удовлетворять всем заданным фильтрам одновременно.
 */
public record FlatFilter(Long id, String name, Double coordinatesX, Float coordinatesY,
                         Date creationDateFrom, Date creationDateTo,
                         Long areaMin, Long areaMax, Long priceMin, Long priceMax, Boolean balcony,
                         Long numberOfRoomsMin, Long numberOfRoomsMax,
                         Furnish furnish, View view, Transport transport,
                         String houseName, Long houseYearMin, Long houseYearMax, Long houseNumberOfFloors,
                         Long houseNumberOfFlatsOnFloor, Integer houseNumberOfLifts) implements Predicate<Flat> {

    /** Проверка корректности диапазонов (min не больше max), иначе 422. */
    public void validateRanges() {
        checkRange("areaMin", areaMin, "areaMax", areaMax);
        checkRange("priceMin", priceMin, "priceMax", priceMax);
        checkRange("numberOfRoomsMin", numberOfRoomsMin, "numberOfRoomsMax", numberOfRoomsMax);
        checkRange("houseYearMin", houseYearMin, "houseYearMax", houseYearMax);
        checkRange("creationDateFrom", creationDateFrom, "creationDateTo", creationDateTo);
    }

    private static <T extends Comparable<T>> void checkRange(String minName, T min, String maxName, T max) {
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw ApiException.unprocessable(minName + " must not be greater than " + maxName);
        }
    }

    @Override
    public boolean test(Flat flat) {
        if (id != null && !id.equals(flat.getId())) {
            return false;
        }
        if (name != null && !name.equals(flat.getName())) {
            return false;
        }
        if (coordinatesX != null && Double.compare(coordinatesX, flat.getCoordinates().getX()) != 0) {
            return false;
        }
        if (coordinatesY != null && Float.compare(coordinatesY, flat.getCoordinates().getY()) != 0) {
            return false;
        }
        if (creationDateFrom != null && flat.getCreationDate().before(creationDateFrom)) {
            return false;
        }
        if (creationDateTo != null && flat.getCreationDate().after(creationDateTo)) {
            return false;
        }
        if (!inRange(flat.getArea(), areaMin, areaMax)
                || !inRange(flat.getPrice(), priceMin, priceMax)
                || !inRange(flat.getNumberOfRooms(), numberOfRoomsMin, numberOfRoomsMax)) {
            return false;
        }
        if (balcony != null && balcony != flat.isBalcony()) {
            return false;
        }
        if (furnish != null && furnish != flat.getFurnish()) {
            return false;
        }
        if (view != null && view != flat.getView()) {
            return false;
        }
        if (transport != null && transport != flat.getTransport()) {
            return false;
        }
        return testHouse(flat.getHouse());
    }

    private boolean testHouse(House house) {
        boolean houseFilterSet = houseName != null || houseYearMin != null || houseYearMax != null
                || houseNumberOfFloors != null || houseNumberOfFlatsOnFloor != null || houseNumberOfLifts != null;
        if (!houseFilterSet) {
            return true;
        }
        if (house == null) {
            return false;
        }
        return (houseName == null || houseName.equals(house.getName()))
                && inRange(house.getYear(), houseYearMin, houseYearMax)
                && (houseNumberOfFloors == null || Objects.equals(houseNumberOfFloors, house.getNumberOfFloors()))
                && (houseNumberOfFlatsOnFloor == null
                    || Objects.equals(houseNumberOfFlatsOnFloor, house.getNumberOfFlatsOnFloor()))
                && (houseNumberOfLifts == null || houseNumberOfLifts == house.getNumberOfLifts());
    }

    private static boolean inRange(Long value, Long min, Long max) {
        if (min == null && max == null) {
            return true;
        }
        if (value == null) {
            return false;
        }
        return (min == null || value >= min) && (max == null || value <= max);
    }
}
