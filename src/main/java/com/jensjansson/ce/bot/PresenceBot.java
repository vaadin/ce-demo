package com.jensjansson.ce.bot;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.vaadin.collaborationengine.CollaborationEngine;
import com.vaadin.collaborationengine.ConnectionContext;
import com.vaadin.collaborationengine.NewUserHandler;
import com.vaadin.collaborationengine.PresenceManager;
import com.vaadin.collaborationengine.TopicConnection;
import com.vaadin.collaborationengine.TopicConnectionRegistration;
import com.vaadin.collaborationengine.UserInfo;
import com.vaadin.flow.shared.Registration;

import com.jensjansson.ce.data.entity.Person;
import com.jensjansson.ce.data.service.PersonService;
import static com.jensjansson.ce.bot.BotRunner.sleepRandom;

public class PresenceBot implements Runnable {
    public static final String BOT_PREFIX = "pr-"+BotUserGenerator.BOT_ID_PREFIX;
    private static final Random random = new Random();

    private static final int botCount = 5;
    private static final int maxAvatarNumber = 8;
    private static PresenceBot instance;

    private PersonService personService;
    private CollaborationEngine ce;
    private List<Integer> ids = Collections.emptyList();
    private List<UserInfo> bots;

    private PresenceManager[] presenceManagers;
    private BiFunction<UserInfo, String, PresenceManager> presenceManagerCreator;
    private Map<Integer,PresenceManager> presenceObservers = new HashMap<>();
    private ConcurrentHashMap<Integer,Bot> botMap = new ConcurrentHashMap<>();

    PresenceBot(PersonService personService, CollaborationEngine ce) {
        this.personService = personService;
        this.ce = ce;
        this.bots = createBotUsers();
        this.presenceManagers = new PresenceManager[botCount];
        this.presenceManagerCreator = createPresenceManagerCreator();
    }

    public static void createInstance(PersonService personService,
        CollaborationEngine ce) {
        if (instance != null) {
            throw new IllegalStateException(
                "Only 1 instance should be created");
        }
        instance = new PresenceBot(personService, ce);
        Thread thread = new Thread(instance);
        thread.setDaemon(true);
        thread.setName("Bot-Thread");
        thread.start();
    }

    @Override
    public synchronized void run() {

        for (int i = 0; true; i = (i + 1) % botCount) {
            createPresence(i);
            try {
                wait(random.nextInt(1000) + 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("Running bots");
            botMap.values().stream().filter(b -> !b.shouldStop).collect(
                Collectors.toList()).forEach(bot -> {
                    try {
                        bot.run();
                    }catch (IllegalStateException e) {
                        e.printStackTrace();
                    }
            });
        }
    }

    private List<UserInfo> createBotUsers() {
        return IntStream.range(0, botCount).mapToObj(i -> {
            UserInfo bot = BotUserGenerator.generateBotUser(BOT_PREFIX);
            bot.setImage(
                String.format("images/avatars/%d.png", maxAvatarNumber - i));
            return bot;
        }).collect(Collectors.toList());
    }

    private void createPresence(int i) {
        if (ids.isEmpty()) {
            ids = personService.findByQuery(null).stream().map(Person::getId)
                .collect(Collectors.toList());
            if (ids.isEmpty()) {
                return;
            }
            fillPresenceObservers();
        }
        String topic = createTopic(random.nextInt(25) + 1);
        PresenceManager previousPresenceManager = presenceManagers[i];
        if (previousPresenceManager != null && topic
            .equals(previousPresenceManager.getTopicId())) {
            //Nothing to do
            return;
        }
        if (previousPresenceManager != null) {
            previousPresenceManager.close();
            presenceManagers[i] = null;
        }
        presenceManagers[i] = presenceManagerCreator.apply(bots.get(i), topic);
    }

    private BiFunction<UserInfo, String, PresenceManager> createPresenceManagerCreator() {

        Constructor<PresenceManager> constructor;
        try {
            constructor = PresenceManager.class
                .getDeclaredConstructor(ConnectionContext.class, UserInfo.class,
                    String.class, CollaborationEngine.class);
            constructor.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        return (bot, topic) -> {
            try {
                PresenceManager manager = constructor
                    .newInstance(new EagerConnectionContext(), bot, topic, ce);
                manager.markAsPresent(true);
                return manager;
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        };
    }

    private void fillPresenceObservers() {
        for(Integer id : ids) {
            UserInfo userInfo = bots.get(id % bots.size());
            String topic = createTopic(id);
            PresenceManager presenceManager = presenceManagerCreator.apply(userInfo,topic);
            presenceManager.markAsPresent(false);
            presenceManager.setNewUserHandler(new UserHandler(id, userInfo, topic, presenceManager));
        }
    }

    class UserHandler implements NewUserHandler {

        private Integer id;
        private UserInfo userInfo;
        private String topic;
        private PresenceManager presenceManager;

        private Set<UserInfo> users = new HashSet<>();
        private boolean hasUsers;
        private TopicConnectionRegistration topicConnectionRegistration;

        public UserHandler(Integer id, UserInfo userInfo, String topic,
            PresenceManager presenceManager) {
            this.id = id;
            this.userInfo = userInfo;
            this.topic = topic;
            this.presenceManager = presenceManager;
        }

        @Override
        public Registration handleNewUser(UserInfo user) {
            if(isRealUser(user)) {
                users.add(user);
                update();
            }
            return () -> {
                if(isRealUser(user)) {
                    users.remove(user);
                    update();
                }
            };
        }

        private boolean isRealUser(UserInfo user) {
            String id = user.getId();
            return !id.startsWith(BotUserGenerator.BOT_ID_PREFIX) && !id
                .startsWith(PresenceBot.BOT_PREFIX);
        }

        private void update() {
            boolean hadUsers = hasUsers;
            hasUsers = !users.isEmpty();
            if(hadUsers == hasUsers) {
                return;
            }

            if(hasUsers) {
                presenceManager.markAsPresent(true);
                Person person = personService.get(id).orElse(null);
                //this.topicConnectionRegistration = BotRunner.onUserJoined(topic,userInfo,person,personService);
                botMap.computeIfAbsent(id, id -> new Bot(topic,userInfo,person));
            } else {
                Bot bot = botMap.remove(id);
                if(bot != null) {
                    System.out.println("Bot removed for " + bot.person.getFirstName());
                    bot.shouldStop = true;
                    bot.topicRegistration.remove();
                    bot.topicRegistration = null;
                    presenceManager.markAsPresent(false);
                } else {
                    System.out.println("Bot not removed for id" + id);
                }
                //this.topicConnectionRegistration.remove();
                //this.topicConnectionRegistration = null;
            }
        }
    }

    class Bot implements Runnable {

        UserInfo user;
        volatile TopicConnection topic;
        volatile Registration topicRegistration;
        Person person;
        volatile boolean shouldStop;

        int saveAfter = BotRunner.generateNumberOfEditsBeforeSave();
        int editCounter = 0;

        Bot(String topicId, UserInfo user,Person person) {
            this.user = user;
            this.person = person;
            System.out.println("Created bot for " + person.getFirstName());
            this.topicRegistration = ce.openTopicConnection(new EagerConnectionContext(), topicId, user, topic -> {
                this.topic=topic;
                System.out.println("Topic connected for " + person.getFirstName());
                return () ->{
                    shouldStop = true;
                    this.topic = null;
                    System.out.println("Topic disconnected for " + person.getFirstName());
                };
            });
        }
        @Override
        public void run() {
            //sleepRandom(1, 3);
                if(topicRegistration == null || shouldStop) {
                    return; // Not connected yet or stopped
                }
                if (editCounter >= saveAfter) {
                    System.out.println("Called save for " + person.getFirstName());
                    //sleepRandom(1, 2);
                    BotSaver.save(ce, topic, person, personService, user);

                    editCounter = 0;
                    saveAfter = BotRunner.generateNumberOfEditsBeforeSave();
                } else {
                    System.out.println("Random edit for " + person.getFirstName());
                    BotFieldEditor.editRandomField(topic, user);
                    editCounter++;
                }
            }

    }

    private String createTopic(Integer id) {
       return String.format("person/%d", id);
    }

}
