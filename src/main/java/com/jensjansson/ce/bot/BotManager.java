package com.jensjansson.ce.bot;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.vaadin.collaborationengine.CollaborationEngine;
import com.vaadin.collaborationengine.ConnectionContext;
import com.vaadin.collaborationengine.NewUserHandler;
import com.vaadin.collaborationengine.PresenceManager;
import com.vaadin.collaborationengine.TopicConnection;
import com.vaadin.collaborationengine.UserInfo;
import com.vaadin.flow.shared.Registration;

import com.jensjansson.ce.data.entity.Person;
import com.jensjansson.ce.data.service.PersonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread that controls the bots, both for presence and for editing forms.
 */
public class BotManager implements Runnable {
    public static final String BOT_PREFIX =
        "pr-" + BotUserGenerator.BOT_ID_PREFIX;
    private final Random random = new Random();
    private static final Logger logger = LoggerFactory
        .getLogger(BotManager.class);

    private static final int botCount = 20;
    private static final int maxAvatarNumber = 8;
    private static BotManager instance;

    private PersonService personService;
    private CollaborationEngine ce;
    private List<Integer> ids = Collections.emptyList();
    private List<UserInfo> bots;
    private List<ExtraBot> extraBots;

    private BiFunction<UserInfo, String, PresenceManager> presenceManagerCreator;
    private Map<Integer,UserHandler> handlerMap;
    private ConcurrentHashMap<Integer, Bot> botMap = new ConcurrentHashMap<>();

    BotManager(PersonService personService, CollaborationEngine ce) {
        this.personService = personService;
        this.ce = ce;
        this.bots = createBotUsers(botCount);
        this.extraBots = createBotUsers(5).stream().map(ExtraBot::new).collect(
            Collectors.toList());
        this.presenceManagerCreator = createPresenceManagerCreator();
    }

    public static void createInstance(PersonService personService,
        CollaborationEngine ce) {
        if (instance != null) {
            throw new IllegalStateException(
                "Only 1 instance should be created");
        }
        instance = new BotManager(personService, ce);
        Thread thread = new Thread(instance);
        thread.setDaemon(true);
        thread.setName("Bot-Thread");
        thread.start();
    }

    private int generateNumberOfEditsBeforeSave() {
        return 2 + (random.nextInt(4));
    }

    @Override
    public synchronized void run() {
        boolean initialized = false;
        while (!initialized) {
            doWait(1000);
            initialized = initializeIfPossible();
        }
        IntStream.range(0,extraBots.size()).forEach(this::createPresence);
        for (int i = 0; true; i = (i + 1) % extraBots.size()) {
            if(random.nextDouble() < 0.2) {
                createPresence(i);
            }
            UserHandler handler = handlerMap.get(ids.get(random.nextInt(ids.size())));
            //Ignore if has users.
            if(handler.hasUsers) continue;
            handler.presenceManager.markAsPresent(random.nextBoolean());

            doWait(500);

            logger.debug("Running bots");
            botMap.values().stream().filter(b -> !b.shouldStop)
                .collect(Collectors.toList()).forEach(bot -> {
                try {
                    bot.run();
                } catch (IllegalStateException e) {
                    logger.warn("Bot threw exception", e);
                }
            });
        }
    }

    private void doWait(int milliseconds) {
        try {
            wait(milliseconds);
        } catch (InterruptedException e) {
            logger.error("Thread interrupted", e);
        }
    }

    private static List<UserInfo> createBotUsers(int count) {
        return IntStream.range(0, count).mapToObj(i -> {
            UserInfo bot = BotUserGenerator.generateBotUser(BOT_PREFIX);
            String image = String
                .format("images/avatars/%d.png", (i % maxAvatarNumber) + 1);
            bot.setImage(image);
            return bot;
        }).collect(Collectors.toList());
    }

    /**
     * Fetch Person entities from the database and initialize if they are available.
     * @return true if initialized
     */
    private boolean initializeIfPossible() {
        this.ids = personService.findByQuery(null).stream().map(Person::getId)
            .collect(Collectors.toList());
        if (this.ids.isEmpty()) {
            return false;
        }
        fillPresenceObservers();
        return true;
    }

    private void createPresence(int i) {
        String topic = createTopic(random.nextInt(20) + 1);
        ExtraBot bot = extraBots.get(i);
        PresenceManager previousPresenceManager = bot.presenceManager;
        if (previousPresenceManager != null && topic
            .equals(previousPresenceManager.getTopicId())) {
            //Nothing to do
            return;
        }
        if (previousPresenceManager != null) {
            bot.presenceManager = null;
            previousPresenceManager.close();
        }
        bot.presenceManager = presenceManagerCreator.apply(bot.userInfo, topic);
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
        Map<Integer, UserHandler> handlerMap = new HashMap<>();
        for (Integer id : ids) {
            UserInfo userInfo = bots.get(id % bots.size());
            String topic = createTopic(id);
            PresenceManager presenceManager = presenceManagerCreator
                .apply(userInfo, topic);
            presenceManager.markAsPresent(random.nextBoolean());
            UserHandler userHandler = new UserHandler(id, userInfo, topic,
                presenceManager);
            presenceManager.setNewUserHandler(userHandler);
            handlerMap.put(id, userHandler);
        }

        this.handlerMap = Collections.unmodifiableMap(handlerMap);
    }

    private String createTopic(Integer id) {
        return String.format("person/%d", id);
    }

    class UserHandler implements NewUserHandler {

        private Integer id;
        private UserInfo userInfo;
        private String topic;
        private PresenceManager presenceManager;

        private Set<UserInfo> users = new HashSet<>();
        private volatile boolean hasUsers;

        public UserHandler(Integer id, UserInfo userInfo, String topic,
            PresenceManager presenceManager) {
            this.id = id;
            this.userInfo = userInfo;
            this.topic = topic;
            this.presenceManager = presenceManager;
        }

        @Override
        public Registration handleNewUser(UserInfo user) {
            if (isRealUser(user)) {
                users.add(user);
                update();
            }
            return () -> {
                if (isRealUser(user)) {
                    users.remove(user);
                    update();
                }
            };
        }

        private boolean isRealUser(UserInfo user) {
            String id = user.getId();
            return !id.startsWith(BotUserGenerator.BOT_ID_PREFIX) && !id
                .startsWith(BotManager.BOT_PREFIX);
        }

        private void update() {
            boolean hadUsers = hasUsers;
            hasUsers = !users.isEmpty();
            if (hadUsers == hasUsers) {
                return;
            }

            if (hasUsers) {
                presenceManager.markAsPresent(true);
                Person person = personService.get(id).orElse(null);
                botMap.computeIfAbsent(id,
                    id -> new Bot(topic, userInfo, person));
            } else {
                Bot bot = botMap.remove(id);
                if (bot != null) {
                    bot.stop();
                } else {
                    logger.debug("Bot not removed for {}", id);
                }
            }
        }
    }

    class Bot implements Runnable {
        private final int delayInSeconds = 2;

        UserInfo user;
        volatile TopicConnection topic;
        volatile Registration topicRegistration;
        Person person;
        volatile boolean shouldStop;

        int saveAfter = generateNumberOfEditsBeforeSave();
        int editCounter = 0;
        Instant lastExecution = null;
        ListIterator<Runnable> currentEditSteps;

        Bot(String topicId, UserInfo user, Person person) {
            this.user = user;
            this.person = person;
            log("Created bot");
            this.topicRegistration = ce
                .openTopicConnection(new EagerConnectionContext(), topicId,
                    user, topic -> {
                        this.topic = topic;
                        log("Topic connected");
                        return () -> {
                            shouldStop = true;
                            this.topic = null;
                            log("Topic disconnected");
                        };
                    });
        }

        @Override
        public void run() {
            if (topicRegistration == null || shouldStop) {
                return; // Not connected yet or stopped
            }
            boolean shouldWait = lastExecution != null && lastExecution.plusSeconds(delayInSeconds)
                .isAfter(Instant.now());
            if (shouldWait) {
                return; // Wait until enough time has passed between edits
            }
            lastExecution = Instant.now();
            if (editCounter >= saveAfter) {
                log("Called save");
                BotSaver.save(ce, topic, person, personService, user);

                editCounter = 0;
                saveAfter = generateNumberOfEditsBeforeSave();
                currentEditSteps = null;
            } else if (currentEditSteps != null && currentEditSteps.hasNext()) {
                currentEditSteps.next().run();
                if(!currentEditSteps.hasNext()) {
                    ++editCounter;
                }
            } else {
                log("Random edit");
                currentEditSteps = BotFieldEditor.editRandomField(topic, user)
                    .listIterator();
            }
        }

        void stop() {
            log("Bot removed");
            this.shouldStop = true;
            this.topicRegistration.remove();
            this.topicRegistration = null;
        }

        void log(String message) {
            logger.debug("{} for {}({})", message, this.person.getFirstName(),
                this.person.getId());
        }
    }

    static class ExtraBot {
       private UserInfo userInfo;
       private PresenceManager presenceManager;

       public ExtraBot(UserInfo userInfo) {
            this.userInfo = userInfo;
        }
    }
}
