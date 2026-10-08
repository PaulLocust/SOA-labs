package ru.itmo.soa.agency.web;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * JAX-RS-приложение. Контекстный путь WAR — {@code /agency}, поэтому операции доступны по
 * {@code /agency/find-with-balcony/...} и {@code /agency/get-most-expensive/...}.
 */
@ApplicationPath("/")
public class AgencyApplication extends Application {
}
