package ru.itmo.soa.agency.service;

import ru.itmo.soa.agency.model.Flat;

import java.util.Optional;

/**
 * Доступ к REST API первого сервиса (Flat Collection Service).
 */
public interface FlatsGateway {

    /**
     * Первая квартира из {@code GET /flats?balcony=...&sort=price,asc|desc&pageSize=1}.
     */
    Optional<Flat> findFirstByBalconyOrderedByPrice(boolean balcony, boolean ascending);

    /**
     * Квартира из {@code GET /flats/{id}} или пусто, если первый сервис ответил 404.
     */
    Optional<Flat> findById(long id);
}
