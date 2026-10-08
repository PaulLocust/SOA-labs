package ru.itmo.soa.flats.service;

import ru.itmo.soa.flats.model.House;

/**
 * Параметры дома из запроса (houseName, houseYear, ...). Любое поле может отсутствовать ({@code null}).
 */
public record HouseInput(String name, Long year, Long numberOfFloors, Long numberOfFlatsOnFloor,
                         Integer numberOfLifts) {

    public static final String REQUIRED_PARAMS =
            "houseName, houseYear, houseNumberOfFlatsOnFloor and houseNumberOfLifts";

    public boolean isEmpty() {
        return name == null && year == null && numberOfFloors == null
                && numberOfFlatsOnFloor == null && numberOfLifts == null;
    }

    /** Переданы все обязательные поля дома (numberOfFloors может отсутствовать). */
    public boolean isComplete() {
        return name != null && year != null && numberOfFlatsOnFloor != null && numberOfLifts != null;
    }

    public House toHouse() {
        return new House(name, year, numberOfFloors, numberOfFlatsOnFloor, numberOfLifts);
    }

    /** Переносит в существующий дом только переданные поля. */
    public void applyTo(House house) {
        if (name != null) {
            house.setName(name);
        }
        if (year != null) {
            house.setYear(year);
        }
        if (numberOfFloors != null) {
            house.setNumberOfFloors(numberOfFloors);
        }
        if (numberOfFlatsOnFloor != null) {
            house.setNumberOfFlatsOnFloor(numberOfFlatsOnFloor);
        }
        if (numberOfLifts != null) {
            house.setNumberOfLifts(numberOfLifts);
        }
    }
}
