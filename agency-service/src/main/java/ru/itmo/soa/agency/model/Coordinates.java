package ru.itmo.soa.agency.model;

import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlRootElement(name = "coordinates")
@XmlType(propOrder = {"x", "y"})
public class Coordinates {
    private Double x;
    private Float y;

    public Double getX() {
        return x;
    }

    public Float getY() {
        return y;
    }
}
