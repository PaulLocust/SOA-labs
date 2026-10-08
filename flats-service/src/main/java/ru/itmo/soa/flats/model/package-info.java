@XmlAccessorType(XmlAccessType.FIELD)
@XmlJavaTypeAdapter(value = DateAdapter.class, type = Date.class)
package ru.itmo.soa.flats.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import java.util.Date;
