package ru.itmo.soa.flats.service;

import ru.itmo.soa.flats.exception.ApiException;

import java.util.ArrayList;
import java.util.List;

/**
 * Проверка ограничений целостности класса Flat. Нарушения приводят к ответу 422.
 * Проверяются только переданные (не {@code null}) значения.
 */
public final class FlatValidator {

    public static final long MAX_AREA = 982;
    public static final double MAX_X = 607;
    public static final float MIN_Y_EXCLUSIVE = -560;

    private FlatValidator() {
    }

    public static void validateId(long id) {
        if (id < 1) {
            throw ApiException.unprocessable("Field 'id' must be greater than 0");
        }
    }

    public static void validate(FlatInput input) {
        List<String> errors = new ArrayList<>();
        if (input.name() != null && input.name().isBlank()) {
            errors.add("Field 'name' must not be empty");
        }
        if (input.coordinatesX() != null && input.coordinatesX() > MAX_X) {
            errors.add("Field 'coordinatesX' must not be greater than 607");
        }
        if (input.coordinatesY() != null && input.coordinatesY() <= MIN_Y_EXCLUSIVE) {
            errors.add("Field 'coordinatesY' must be greater than -560");
        }
        if (input.area() != null && (input.area() < 1 || input.area() > MAX_AREA)) {
            errors.add("Field 'area' must be between 1 and 982");
        }
        positive(errors, "price", input.price());
        positive(errors, "numberOfRooms", input.numberOfRooms());
        collectHouseErrors(errors, input.house());
        throwIfAny(errors);
    }

    public static void validate(HouseInput house) {
        List<String> errors = new ArrayList<>();
        collectHouseErrors(errors, house);
        throwIfAny(errors);
    }

    private static void collectHouseErrors(List<String> errors, HouseInput house) {
        if (house.name() != null && house.name().isBlank()) {
            errors.add("Field 'houseName' must not be empty");
        }
        positive(errors, "houseYear", house.year());
        positive(errors, "houseNumberOfFloors", house.numberOfFloors());
        positive(errors, "houseNumberOfFlatsOnFloor", house.numberOfFlatsOnFloor());
        positive(errors, "houseNumberOfLifts", house.numberOfLifts() == null ? null : house.numberOfLifts().longValue());
    }

    private static void positive(List<String> errors, String field, Long value) {
        if (value != null && value < 1) {
            errors.add("Field '" + field + "' must be greater than 0");
        }
    }

    private static void throwIfAny(List<String> errors) {
        if (!errors.isEmpty()) {
            throw ApiException.unprocessable(String.join("; ", errors));
        }
    }
}
