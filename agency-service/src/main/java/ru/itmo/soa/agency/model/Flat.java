package ru.itmo.soa.agency.model;

import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Схема {@code Flat} из flats-service.yaml. Агентство получает квартиры от первого сервиса и отдаёт их
 * клиенту без изменений, поэтому дата создания и перечисления хранятся как строки (значения не меняются).
 */
@XmlRootElement(name = "flat")
@XmlType(propOrder = {"id", "name", "coordinates", "creationDate", "area", "price", "balcony",
        "numberOfRooms", "furnish", "view", "transport", "house"})
public class Flat {
    private Long id;
    private String name;
    private Coordinates coordinates;
    private String creationDate;
    private Long area;
    private Long price;
    private Boolean balcony;
    private Long numberOfRooms;
    private String furnish;
    private String view;
    private String transport;
    private House house;

    public Flat() {
    }

    /** Для тестов. */
    public Flat(long id, long price) {
        this.id = id;
        this.price = price;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Coordinates getCoordinates() {
        return coordinates;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public Long getArea() {
        return area;
    }

    public Long getPrice() {
        return price;
    }

    public Boolean getBalcony() {
        return balcony;
    }

    public Long getNumberOfRooms() {
        return numberOfRooms;
    }

    public String getFurnish() {
        return furnish;
    }

    public String getView() {
        return view;
    }

    public String getTransport() {
        return transport;
    }

    public House getHouse() {
        return house;
    }
}
