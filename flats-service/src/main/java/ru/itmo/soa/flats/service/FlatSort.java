package ru.itmo.soa.flats.service;

import ru.itmo.soa.flats.exception.ApiException;
import ru.itmo.soa.flats.model.Flat;
import ru.itmo.soa.flats.model.House;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Сортировка списка квартир по параметрам {@code sort=поле,asc|desc} (может повторяться).
 * {@code null} считается меньше любого значения, перечисления сравниваются по имени константы.
 * Последним критерием всегда добавляется {@code id,asc}, чтобы порядок (и страницы) был стабильным.
 */
public final class FlatSort {

    private static final Map<String, Comparator<Flat>> FIELDS = new LinkedHashMap<>();

    static {
        register("id", Flat::getId);
        register("name", Flat::getName);
        register("coordinates.x", f -> f.getCoordinates().getX());
        register("coordinates.y", f -> f.getCoordinates().getY());
        register("creationDate", Flat::getCreationDate);
        register("area", Flat::getArea);
        register("price", Flat::getPrice);
        register("balcony", Flat::isBalcony);
        register("numberOfRooms", Flat::getNumberOfRooms);
        register("furnish", f -> f.getFurnish() == null ? null : f.getFurnish().name());
        register("view", f -> f.getView() == null ? null : f.getView().name());
        register("transport", f -> f.getTransport().name());
        registerHouse("house.name", House::getName);
        registerHouse("house.year", House::getYear);
        registerHouse("house.numberOfFloors", House::getNumberOfFloors);
        registerHouse("house.numberOfFlatsOnFloor", House::getNumberOfFlatsOnFloor);
        registerHouse("house.numberOfLifts", House::getNumberOfLifts);
    }

    private FlatSort() {
    }

    private static <T extends Comparable<? super T>> void register(String field, Function<Flat, T> key) {
        FIELDS.put(field, Comparator.comparing(key, Comparator.nullsFirst(Comparator.naturalOrder())));
    }

    private static <T extends Comparable<? super T>> void registerHouse(String field, Function<House, T> key) {
        register(field, f -> f.getHouse() == null ? null : key.apply(f.getHouse()));
    }

    /**
     * Строит компаратор по значениям параметра sort. Неизвестное поле или направление — 400.
     */
    public static Comparator<Flat> parse(List<String> sortParams) {
        List<Comparator<Flat>> comparators = new ArrayList<>();
        for (String raw : sortParams) {
            String[] parts = raw.split(",", -1);
            if (parts.length < 1 || parts.length > 2) {
                throw invalidFormat(raw);
            }
            String field = parts[0].trim();
            Comparator<Flat> comparator = FIELDS.get(field);
            if (comparator == null) {
                throw ApiException.badRequest("Unknown sort field '" + field + "'. Allowed fields: "
                        + String.join(", ", FIELDS.keySet()));
            }
            String direction = parts.length == 2 ? parts[1].trim().toLowerCase() : "asc";
            switch (direction) {
                case "asc" -> comparators.add(comparator);
                case "desc" -> comparators.add(comparator.reversed());
                default -> throw invalidFormat(raw);
            }
        }
        comparators.add(FIELDS.get("id"));
        return comparators.stream().reduce(Comparator::thenComparing).orElseThrow();
    }

    private static ApiException invalidFormat(String raw) {
        return ApiException.badRequest("Parameter 'sort' must be in format 'field,asc' or 'field,desc', got '"
                + raw + "'");
    }
}
