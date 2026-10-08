package ru.itmo.soa.flats.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Конфигурация Spring MVC. XML-ответы сериализуются через JAXB
 * (Jaxb2RootElementHttpMessageConverter регистрируется автоматически, т.к. JAXB есть в classpath).
 */
@Configuration
@EnableWebMvc
@ComponentScan("ru.itmo.soa.flats")
public class WebConfig implements WebMvcConfigurer {
}
