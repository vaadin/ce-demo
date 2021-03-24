package com.jensjansson.ce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;

/**
 * The entry point of the Spring Boot application.
 */
@SpringBootApplication
@PWA(name = "CE Demo", shortName = "CE Demo")
@Push
@Theme("ce-demo")
public class Application extends SpringBootServletInitializer
        implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
