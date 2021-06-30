package com.jensjansson.ce;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

import com.jensjansson.ce.bot.BotUserGenerator;
import com.jensjansson.ce.bot.PresenceBot;
import com.jensjansson.ce.data.service.PersonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.vaadin.collaborationengine.CollaborationEngine;
import com.vaadin.collaborationengine.CollaborationEngineConfiguration;
import com.vaadin.collaborationengine.LicenseEventHandler;
import com.vaadin.collaborationengine.UserInfo;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.theme.Theme;

/**
 * The entry point of the Spring Boot application.
 */
@SpringBootApplication
@PWA(name = "CE Demo", shortName = "CE Demo")
@Push
@Theme("ce-demo")
@EnableScheduling
public class Application extends SpringBootServletInitializer
        implements AppShellConfigurator, VaadinServiceInitListener {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(Application.class);

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Autowired
    PersonService personService;

    @Override
    public void serviceInit(ServiceInitEvent serviceInitEvent) {
        LicenseEventHandler licenseEventHandler = licenseEvent -> {
            switch (licenseEvent.getType()) {
            case GRACE_PERIOD_STARTED:
            case LICENSE_EXPIRES_SOON:

                LOGGER.warn(licenseEvent.getMessage());
                break;
            case GRACE_PERIOD_ENDED:
            case LICENSE_EXPIRED:
                LOGGER.error(licenseEvent.getMessage());
                break;
            default:
                LOGGER.error("Unknown error: " + licenseEvent.getMessage());
            }
        };
        CollaborationEngineConfiguration configuration = new CollaborationEngineConfiguration(
                licenseEventHandler);
        CollaborationEngine ce = CollaborationEngine.configure(serviceInitEvent.getSource(),
                configuration);

        PresenceBot.createInstance(personService, ce);
    }
}
