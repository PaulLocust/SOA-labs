package ru.itmo.soa.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Клиентское приложение: отдаёт браузеру SPA (static/) и проксирует её запросы
 * {@code /api/**} и {@code /agency/**} в оба веб-сервиса по HTTPS.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class ClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClientApplication.class, args);
    }
}
