package ru.itmo.soa.client;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Прозрачный прокси от SPA к веб-сервисам: запрос {@code /api/...} уходит в первый сервис,
 * {@code /agency/...} — во второй, с теми же методом, путём и query-строкой.
 * Код ответа, заголовки Content-Type/Location/Allow и XML-тело возвращаются браузеру без изменений,
 * поэтому SPA видит ровно то, что ответил сервис (в том числе ошибки 400/404/406/422).
 */
@RestController
public class ServiceProxyController {

    private static final Logger log = LoggerFactory.getLogger(ServiceProxyController.class);
    private static final List<String> PASSED_RESPONSE_HEADERS =
            List.of(HttpHeaders.CONTENT_TYPE, HttpHeaders.LOCATION, HttpHeaders.ALLOW);

    private final ServicesProperties properties;
    private final HttpClient http;

    public ServiceProxyController(ServicesProperties properties) throws IOException, GeneralSecurityException {
        this.properties = properties;
        this.http = HttpClient.newBuilder()
                .sslContext(sslContext(properties.truststore(), properties.truststorePassword()))
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @RequestMapping({"/api", "/api/**", "/agency", "/agency/**"})
    public ResponseEntity<byte[]> proxy(HttpServletRequest request) throws InterruptedException {
        String path = request.getRequestURI();
        boolean agency = path.equals("/agency") || path.startsWith("/agency/");
        String serviceName = agency ? "Agency Service" : "Flat Collection Service";
        String origin = (agency ? properties.agencyOrigin() : properties.flatsOrigin()).replaceAll("/+$", "");
        String query = request.getQueryString();
        URI target = URI.create(origin + path + (query == null ? "" : "?" + query));

        String accept = request.getHeader(HttpHeaders.ACCEPT);
        HttpRequest upstream = HttpRequest.newBuilder(target)
                .timeout(Duration.ofSeconds(30))
                .header(HttpHeaders.ACCEPT, accept == null ? MediaType.APPLICATION_XML_VALUE : accept)
                .method(request.getMethod(), HttpRequest.BodyPublishers.noBody())
                .build();
        try {
            HttpResponse<byte[]> response = http.send(upstream, HttpResponse.BodyHandlers.ofByteArray());
            ResponseEntity.BodyBuilder builder = ResponseEntity.status(response.statusCode());
            for (String header : PASSED_RESPONSE_HEADERS) {
                response.headers().firstValue(header).ifPresent(value -> builder.header(header, value));
            }
            return builder.body(response.body());
        } catch (HttpTimeoutException e) {
            log.warn("{} {} -> timeout", request.getMethod(), target);
            return error(504, "Gateway Timeout", serviceName + " did not respond in time", path);
        } catch (IOException e) {
            log.warn("{} {} -> {}", request.getMethod(), target, e.toString());
            return error(502, "Bad Gateway", serviceName + " is unavailable (" + target.getAuthority() + ")", path);
        }
    }

    /** Ошибка самого клиента (сервис недоступен) в том же формате, что и ошибки сервисов. */
    private static ResponseEntity<byte[]> error(int status, String error, String message, String path) {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><error>"
                + "<timestamp>" + Instant.now() + "</timestamp>"
                + "<status>" + status + "</status>"
                + "<error>" + escape(error) + "</error>"
                + "<message>" + escape(message) + "</message>"
                + "<path>" + escape(path) + "</path></error>";
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_XML)
                .body(xml.getBytes(StandardCharsets.UTF_8));
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static SSLContext sslContext(String truststore, String password)
            throws IOException, GeneralSecurityException {
        if (truststore == null || truststore.isBlank()) {
            log.warn("services.truststore is not set: the default JVM trust store will be used");
            return SSLContext.getDefault();
        }
        KeyStore store = KeyStore.getInstance("PKCS12");
        try (InputStream in = Files.newInputStream(Path.of(truststore))) {
            store.load(in, password == null ? new char[0] : password.toCharArray());
        }
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(store);
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, tmf.getTrustManagers(), null);
        return context;
    }
}
