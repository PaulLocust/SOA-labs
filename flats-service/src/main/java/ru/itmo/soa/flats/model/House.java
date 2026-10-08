package ru.itmo.soa.flats.model;

import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlRootElement(name = "house")
@XmlType(propOrder = {"name", "year", "numberOfFloors", "numberOfFlatsOnFloor", "numberOfLifts"})
public class House {
    private String name; //Поле не может быть null
    private Long year; //Значение поля должно быть больше 0
    private Long numberOfFloors; //Поле может быть null, Значение поля должно быть больше 0
    private Long numberOfFlatsOnFloor; //Значение поля должно быть больше 0
    private int numberOfLifts; //Значение поля должно быть больше 0

    public House() {
    }

    public House(String name, Long year, Long numberOfFloors, Long numberOfFlatsOnFloor, int numberOfLifts) {
        this.name = name;
        this.year = year;
        this.numberOfFloors = numberOfFloors;
        this.numberOfFlatsOnFloor = numberOfFlatsOnFloor;
        this.numberOfLifts = numberOfLifts;
    }

    public House copy() {
        return new House(name, year, numberOfFloors, numberOfFlatsOnFloor, numberOfLifts);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getYear() {
        return year;
    }

    public void setYear(Long year) {
        this.year = year;
    }

    public Long getNumberOfFloors() {
        return numberOfFloors;
    }

    public void setNumberOfFloors(Long numberOfFloors) {
        this.numberOfFloors = numberOfFloors;
    }

    public Long getNumberOfFlatsOnFloor() {
        return numberOfFlatsOnFloor;
    }

    public void setNumberOfFlatsOnFloor(Long numberOfFlatsOnFloor) {
        this.numberOfFlatsOnFloor = numberOfFlatsOnFloor;
    }

    public int getNumberOfLifts() {
        return numberOfLifts;
    }

    public void setNumberOfLifts(int numberOfLifts) {
        this.numberOfLifts = numberOfLifts;
    }
}
