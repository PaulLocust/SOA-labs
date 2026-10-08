package ru.itmo.soa.agency.client;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import ru.itmo.soa.agency.model.ApiError;
import ru.itmo.soa.agency.model.Flat;
import ru.itmo.soa.agency.model.FlatPage;
import ru.itmo.soa.agency.service.AgencyException;
import ru.itmo.soa.agency.service.FlatsGateway;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Клиент REST API первого сервиса по HTTPS (JAX-RS Client API).
 * <p>
 * Настройки (системные свойства WildFly или переменные окружения):
 * <ul>
 *     <li>{@code flats.service.url} / {@code FLATS_SERVICE_URL} — базовый URL, например {@code https://localhost:8443/api};</li>
 *     <li>{@code flats.service.truststore} / {@code FLATS_SERVICE_TRUSTSTORE} — PKCS12-хранилище с самоподписанным
 *     сертификатом первого сервиса;</li>
 *     <li>{@code flats.service.truststore.password} / {@code FLATS_SERVICE_TRUSTSTORE_PASSWORD} — пароль хранилища.</li>
 * </ul>
 */
@ApplicationScoped
public class FlatsServiceClient implements FlatsGateway {

    private static final Logger log = Logger.getLogger(FlatsServiceClient.class.getName());

    private static final String DEFAULT_URL = "https://localhost:8443/api";
    private static final int CONNECT_TIMEOUT_SECONDS = 5;
    private static final int READ_TIMEOUT_SECONDS = 15;

    private String baseUrl;
    private SSLContext sslContext;

    @PostConstruct
    void init() {
        baseUrl = setting("flats.service.url", "FLATS_SERVICE_URL", DEFAULT_URL).replaceAll("/+$", "");
        sslContext = createSslContext(
                setting("flats.service.truststore", "FLATS_SERVICE_TRUSTSTORE", null),
                setting("flats.service.truststore.password", "FLATS_SERVICE_TRUSTSTORE_PASSWORD", ""));
        log.info("Flats service base URL: " + baseUrl);
    }

    @Override
    public Optional<Flat> findFirstByBalconyOrderedByPrice(boolean balcony, boolean ascending) {
        return call(target -> target.path("flats")
                        .queryParam("balcony", balcony)
                        .queryParam("sort", "price," + (ascending ? "asc" : "desc"))
                        .queryParam("pageNumber", 0)
                        .queryParam("pageSize", 1),
                response -> {
                    expectStatus(response, Response.Status.OK);
                    FlatPage page = response.readEntity(FlatPage.class);
                    return page.getItems().stream().findFirst();
                });
    }

    @Override
    public Optional<Flat> findById(long id) {
        return call(target -> target.path("flats").path(Long.toString(id)),
                response -> {
                    if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
                        return Optional.empty();
                    }
                    expectStatus(response, Response.Status.OK);
                    return Optional.of(response.readEntity(Flat.class));
                });
    }

    private <T> T call(Function<WebTarget, WebTarget> request, Function<Response, T> handler) {
        Client client = ClientBuilder.newBuilder()
                .sslContext(sslContext)
                .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();
        try (Response response = request.apply(client.target(baseUrl))
                .request(MediaType.APPLICATION_XML_TYPE)
                .get()) {
            return handler.apply(response);
        } catch (ProcessingException e) {
            if (hasCause(e, SocketTimeoutException.class)) {
                throw new AgencyException(504, "Flats service did not respond in time", e);
            }
            log.log(Level.WARNING, "Flats service call failed", e);
            throw new AgencyException(503, "Flats service is unavailable", e);
        } finally {
            client.close();
        }
    }

    /** Неожиданный ответ первого сервиса — ошибка на его стороне (502). */
    private static void expectStatus(Response response, Response.Status expected) {
        if (response.getStatus() == expected.getStatusCode()) {
            return;
        }
        String details = "";
        try {
            ApiError error = response.readEntity(ApiError.class);
            if (error != null && error.getMessage() != null) {
                details = ": " + error.getMessage();
            }
        } catch (RuntimeException ignored) {
            // тело ответа не в формате Error
        }
        throw new AgencyException(502, "Flats service responded with status " + response.getStatus() + details);
    }

    private static boolean hasCause(Throwable e, Class<? extends Throwable> type) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (type.isInstance(t)) {
                return true;
            }
        }
        return false;
    }

    private static String setting(String property, String env, String defaultValue) {
        String value = System.getProperty(property);
        if (value == null || value.isBlank()) {
            value = System.getenv(env);
        }
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static SSLContext createSslContext(String trustStorePath, String password) {
        try {
            if (trustStorePath == null) {
                log.warning("flats.service.truststore is not set: the default JVM trust store will be used");
                return SSLContext.getDefault();
            }
            KeyStore trustStore = KeyStore.getInstance("PKCS12");
            try (InputStream in = Files.newInputStream(Path.of(trustStorePath))) {
                trustStore.load(in, password.toCharArray());
            }
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, tmf.getTrustManagers(), null);
            return context;
        } catch (Exception e) {
            throw new IllegalStateException("Cannot initialize SSL context for flats service client", e);
        }
    }
}
