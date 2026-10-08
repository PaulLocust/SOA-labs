package ru.itmo.soa.flats.dto;

import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "averageNumberOfRoomsResult")
public class AverageNumberOfRoomsResult {
    private double average;

    public AverageNumberOfRoomsResult() {
    }

    public AverageNumberOfRoomsResult(double average) {
        this.average = average;
    }

    public double getAverage() {
        return average;
    }
}
