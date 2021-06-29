package com.jensjansson.ce.bot;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.collaborationengine.CollaborationEngine;
import com.vaadin.collaborationengine.ConnectionContext;
import com.vaadin.collaborationengine.PresenceManager;
import com.vaadin.collaborationengine.UserInfo;
import com.vaadin.flow.component.Component;

import com.jensjansson.ce.data.entity.Person;
import com.jensjansson.ce.data.service.PersonService;

public class PresenceBot implements Runnable {
    private static final Random random = new Random();

    private HashMap<String, PresenceData> managers = new HashMap<>();
    private PersonService personService;
    private CollaborationEngine ce;
    private List<Integer> ids = Collections.emptyList();
    private List<UserInfo> bots;

    private List<PresenceManager> presenceManagers = Collections.emptyList();


    public PresenceBot(PersonService personService, CollaborationEngine ce) {
        this.personService = personService;
        this.ce = ce;
        bots = Stream
            .generate(BotUserGenerator::generateBotUser).limit(5)
            .collect(Collectors.toList());
    }

    @Override
    public void run() {
        while (true) {
            createPresence();
            try {
                Thread.sleep(random.nextInt(5000) + 5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void createPresence() {

        if (ids.isEmpty()) {
            ids = personService.findByQuery(null).stream()
                .map(Person::getId).collect(Collectors.toList());
            if (ids.isEmpty()) {
                return;
            }
        }
        presenceManagers.forEach(PresenceManager::close);
        Constructor<PresenceManager> constructor;
        try {
            constructor = PresenceManager.class
                .getDeclaredConstructor(ConnectionContext.class,
                    UserInfo.class, String.class,
                    CollaborationEngine.class);
            constructor.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        presenceManagers = bots.stream().map(bot -> {
            String topic = String
                .format("person/%d", ids.get(random.nextInt(25) + 1));
            try {
                PresenceManager manager = constructor
                    .newInstance(new EagerConnectionContext(), bot, topic,
                        ce);
                manager.markAsPresent(true);
                return manager;
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());
    }

    public synchronized void add(String topic, Component component) {

        PresenceData data = managers.computeIfAbsent(topic, t -> {
            UserInfo bot = BotUserGenerator.generateBotUser();
            return new PresenceData(bot, t);
        });
        data.add(component);
        component.addDetachListener(e -> {
            synchronized (PresenceBot.this) {
                PresenceData dataToRemove = managers.get(topic);
                dataToRemove.remove(component);
                if (!data.hasComponents()) {
                    managers.remove(topic);
                }
            }
        });
    }

}

class PresenceData {
    private UserInfo user;
    private String topic;
    private Set<Component> components = new HashSet<>();
    private PresenceManager manager;
    private Component current;

    public PresenceData(UserInfo user, String topic) {
        this.user = user;
        this.topic = topic;
    }

    void add(Component c) {
        components.add(c);
        createPresenceManager();
    }

    void remove(Component c) {
        components.remove(c);
        if (c.equals(this.current) && this.manager != null) {
            this.manager.close();
            this.manager = null;
            this.current = null;
            createPresenceManager();
        }
    }

    boolean hasComponents() {
        return components.isEmpty();
    }

    void createPresenceManager() {
        if (this.manager == null) {
            Optional<Component> componentOptional = components.stream()
                .findAny();
            if (componentOptional.isPresent()) {
                this.current = componentOptional.get();
                this.manager = new PresenceManager(componentOptional.get(),
                    user, topic);
                this.manager.markAsPresent(true);
            }
        }
    }
}