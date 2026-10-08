package ru.itmo.soa.flats.web;

import ru.itmo.soa.flats.exception.ApiException;
import ru.itmo.soa.flats.model.Furnish;
import ru.itmo.soa.flats.model.Transport;
import ru.itmo.soa.flats.model.View;
import ru.itmo.soa.flats.service.FlatFilter;
import ru.itmo.soa.flats.service.FlatInput;
import ru.itmo.soa.flats.service.HouseInput;

/**
 * Преобразование query-параметров в объекты сервисного слоя (имена параметров — как в спецификации).
 */
final class FlatParams {

    private FlatParams() {
    }

    /** Параметры POST и PUT: обязательные поля квартиры + необязательный дом (все обязательные поля дома или ничего). */
    static FlatInput fullInput(QueryParams p) {
        FlatInput input = new FlatInput(
                p.requiredString("name"),
                p.requiredDouble("coordinatesX"),
                p.requiredFloat("coordinatesY"),
                p.requiredLong("area"),
                p.requiredLong("price"),
                p.requiredBoolean("balcony"),
                p.requiredLong("numberOfRooms"),
                p.enumValue("furnish", Furnish.class),
                p.enumValue("view", View.class),
                p.requiredEnum("transport", Transport.class),
                optionalHouse(p));
        HouseInput house = input.house();
        if (!house.isEmpty() && !house.isComplete()) {
            throw ApiException.badRequest("Parameters " + HouseInput.REQUIRED_PARAMS + " must be specified together");
        }
        return input;
    }

    /** Параметры PATCH: всё необязательно, но хотя бы один параметр должен быть передан. */
    static FlatInput partialInput(QueryParams p) {
        FlatInput input = new FlatInput(
                p.string("name"),
                p.doubleValue("coordinatesX"),
                p.floatValue("coordinatesY"),
                p.longValue("area"),
                p.longValue("price"),
                p.booleanValue("balcony"),
                p.longValue("numberOfRooms"),
                p.enumValue("furnish", Furnish.class),
                p.enumValue("view", View.class),
                p.enumValue("transport", Transport.class),
                optionalHouse(p));
        if (input.isEmpty()) {
            throw ApiException.badRequest("At least one field parameter must be specified");
        }
        return input;
    }

    /** Параметры операции count-by-house-greater-than (numberOfFloors необязателен). */
    static HouseInput requiredHouse(QueryParams p) {
        return new HouseInput(
                p.requiredString("houseName"),
                p.requiredLong("houseYear"),
                p.longValue("houseNumberOfFloors"),
                p.requiredLong("houseNumberOfFlatsOnFloor"),
                p.requiredInt("houseNumberOfLifts"));
    }

    static FlatFilter filter(QueryParams p) {
        return new FlatFilter(
                p.longValue("id"),
                p.string("name"),
                p.doubleValue("coordinatesX"),
                p.floatValue("coordinatesY"),
                p.dateTimeValue("creationDateFrom"),
                p.dateTimeValue("creationDateTo"),
                p.longValue("areaMin"),
                p.longValue("areaMax"),
                p.longValue("priceMin"),
                p.longValue("priceMax"),
                p.booleanValue("balcony"),
                p.longValue("numberOfRoomsMin"),
                p.longValue("numberOfRoomsMax"),
                p.enumValue("furnish", Furnish.class),
                p.enumValue("view", View.class),
                p.enumValue("transport", Transport.class),
                p.string("houseName"),
                p.longValue("houseYearMin"),
                p.longValue("houseYearMax"),
                p.longValue("houseNumberOfFloors"),
                p.longValue("houseNumberOfFlatsOnFloor"),
                p.intValue("houseNumberOfLifts"));
    }

    private static HouseInput optionalHouse(QueryParams p) {
        return new HouseInput(
                p.string("houseName"),
                p.longValue("houseYear"),
                p.longValue("houseNumberOfFloors"),
                p.longValue("houseNumberOfFlatsOnFloor"),
                p.intValue("houseNumberOfLifts"));
    }
}
