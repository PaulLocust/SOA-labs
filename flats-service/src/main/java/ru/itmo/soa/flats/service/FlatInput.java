package ru.itmo.soa.flats.service;

import ru.itmo.soa.flats.model.Furnish;
import ru.itmo.soa.flats.model.Transport;
import ru.itmo.soa.flats.model.View;

/**
 * Значения полей квартиры, переданные в запросе на создание/обновление.
 * Для PATCH любое поле может отсутствовать ({@code null}).
 */
public record FlatInput(String name, Double coordinatesX, Float coordinatesY, Long area, Long price,
                        Boolean balcony, Long numberOfRooms, Furnish furnish, View view, Transport transport,
                        HouseInput house) {

    public boolean isEmpty() {
        return name == null && coordinatesX == null && coordinatesY == null && area == null && price == null
                && balcony == null && numberOfRooms == null && furnish == null && view == null
                && transport == null && house.isEmpty();
    }
}
