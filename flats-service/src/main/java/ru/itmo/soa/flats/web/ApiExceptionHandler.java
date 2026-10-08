package ru.itmo.soa.flats.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.itmo.soa.flats.dto.ApiError;
import ru.itmo.soa.flats.exception.ApiException;
import ru.itmo.soa.flats.model.DateAdapter;

import java.io.IOException;
import java.util.Date;
import java.util.stream.Collectors;

/**
 * Преобразует ошибки в XML-ответ со схемой {@code Error}.
 * Тело пишется напрямую в ответ, минуя согласование формата: иначе при {@code Accept: application/json}
 * (ответ 406) Spring не смог бы записать XML-тело ошибки.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Log log = LogFactory.getLog(ApiExceptionHandler.class);
    private static final JAXBContext ERROR_CONTEXT = createContext();

    @ExceptionHandler(ApiException.class)
    public void handleApiException(ApiException e, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        write(response, request, e.getStatus().value(), e.getMessage());
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public void handleNotAcceptable(HttpServletRequest request, HttpServletResponse response) throws IOException {
        write(response, request, HttpStatus.NOT_ACCEPTABLE.value(), "Only application/xml is supported");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public void handleMethodNotSupported(HttpRequestMethodNotSupportedException e, HttpServletRequest request,
                                         HttpServletResponse response) throws IOException {
        if (e.getSupportedHttpMethods() != null) {
            response.setHeader(HttpHeaders.ALLOW, e.getSupportedHttpMethods().stream()
                    .map(HttpMethod::name).collect(Collectors.joining(", ")));
        }
        write(response, request, HttpStatus.METHOD_NOT_ALLOWED.value(),
                "Method " + e.getMethod() + " is not supported for this URL");
    }

    @ExceptionHandler(Exception.class)
    public void handleOther(Exception e, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        // Остальные ошибки Spring MVC (например, 404 для неизвестного URL)
        if (e instanceof ErrorResponse error) {
            int status = error.getStatusCode().value();
            String message = status == HttpStatus.NOT_FOUND.value()
                    ? "Resource " + request.getRequestURI() + " not found"
                    : error.getBody().getDetail();
            write(response, request, status, message);
            return;
        }
        log.error("Unexpected error while processing " + request.getMethod() + " " + request.getRequestURI(), e);
        write(response, request, HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal server error");
    }

    private static void write(HttpServletResponse response, HttpServletRequest request, int status, String message)
            throws IOException {
        ApiError error = new ApiError(DateAdapter.format(new Date()), status, reasonPhrase(status), message,
                request.getRequestURI());
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_XML_VALUE);
        response.setCharacterEncoding("UTF-8");
        try {
            Marshaller marshaller = ERROR_CONTEXT.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            marshaller.marshal(error, response.getOutputStream());
        } catch (JAXBException e) {
            throw new IOException("Failed to write error response", e);
        }
    }

    private static String reasonPhrase(int status) {
        if (status == 422) {
            return "Unprocessable Entity";
        }
        HttpStatus httpStatus = HttpStatus.resolve(status);
        return httpStatus == null ? "Error" : httpStatus.getReasonPhrase();
    }

    private static JAXBContext createContext() {
        try {
            return JAXBContext.newInstance(ApiError.class);
        } catch (JAXBException e) {
            throw new IllegalStateException(e);
        }
    }
}
