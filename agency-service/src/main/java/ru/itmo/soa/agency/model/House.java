package ru.itmo.soa.agency.model;

import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlRootElement(name = "house")
@XmlType(propOrder = {"name", "year", "numberOfFloors", "numberOfFlatsOnFloor", "numberOfLifts"})
public class House {
    private String name;
    private Long year;
    private Long numberOfFloors;
    private Long numberOfFlatsOnFloor;
    private Integer numberOfLifts;

    public String getName() {
        return name;
    }

    public Long getYear() {
        return year;
    }

    public Long getNumberOfFloors() {
        return numberOfFloors;
    }

    public Long getNumberOfFlatsOnFloor() {
        return numberOfFlatsOnFloor;
    }

    public Integer getNumberOfLifts() {
        return numberOfLifts;
    }
}
