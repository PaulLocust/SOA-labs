package ru.itmo.soa.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Адреса сервисов и хранилище доверенных (самоподписанных) сертификатов.
 *
 * @param flatsOrigin        схема, хост и порт первого сервиса (Jetty), например {@code https://localhost:8443}
 * @param agencyOrigin       схема, хост и порт второго сервиса (WildFly), например {@code https://localhost:8444}
 * @param truststore         PKCS12-хранилище с сертификатами обоих сервисов (пусто — хранилище JVM по умолчанию)
 * @param truststorePassword пароль хранилища
 */
@ConfigurationProperties(prefix = "services")
public record ServicesProperties(String flatsOrigin, String agencyOrigin, String truststore,
                                 String truststorePassword) {
}
