package com.jensjansson.ce.bot;

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
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.jensjansson.ce.data.entity.Person;
import com.jensjansson.ce.data.service.PersonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.collaborationengine.CollaborationEngine;
import com.vaadin.collaborationengine.CollaborationMap;
import com.vaadin.collaborationengine.PresenceHandler;
import com.vaadin.collaborationengine.PresenceManager;
import com.vaadin.collaborationengine.TopicConnection;
import com.vaadin.collaborationengine.UserInfo;
import com.vaadin.flow.shared.Registration;

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
    /**
     * The images in /images/avatar go
     * from 1.png to 8.png
     */
    private static final int maxAvatarNumber = 8;
    /**
     * There should be only one BotManager thread, which is initialized in {@link #createInstance(PersonService, CollaborationEngine)}
     */
    private static BotManager instance;

    private PersonService personService;
    private CollaborationEngine ce;
    /**
     * Ids from the {@link Person} entities.
     */
    private List<Integer> ids = Collections.emptyList();
    private List<UserInfo> bots;
    private List<ExtraBot> extraBots;

    private Map<Integer,UserHandler> handlerMap;
    private ConcurrentHashMap<Integer, Bot> botMap = new ConcurrentHashMap<>();
    /**
     * RefreshGridMap is used to communicate with EmployeesView that a given
     * entity has been changed in the database and should be reloaded.
     */
    private CollaborationMap refreshGridMap;

    BotManager(PersonService personService, CollaborationEngine ce) {
        this.personService = personService;
        this.ce = ce;
        this.bots = createBotUsers(botCount);
        this.extraBots = createBotUsers(5).stream().map(ExtraBot::new).collect(
            Collectors.toList());

        //  Connect to the refreshGrid topic.
        ce.openTopicConnection(ce.getSystemContext(),
            "refreshGrid", createBotUsers(1).get(0), topicConnection -> {
                refreshGridMap = topicConnection.getNamedMap("refreshGrid");
                return () -> refreshGridMap = null;
            });
    }

    /**
     * Called by the {@link com.jensjansson.ce.Application} class on startup to create a BotManager instance.
     * @param personService {@link PersonService}
     * @param ce {@link CollaborationEngine}
     */
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

    /**
     * Generates a random number that will be used as the number of edits a
     * specific bot will do before saving the entity.
     *
     * @return A random number between 2 and 5.
     */
    private int generateNumberOfEditsBeforeSave() {
        return 2 + (random.nextInt(4));
    }

    @Override
    public synchronized void run() {
        // We need to wait until the DataGenerator has run and initialized the
        // database.
        boolean initialized = false;
        while (!initialized) {
            doWait(1000);
            initialized = initializeIfPossible();
        }
        IntStream.range(0,extraBots.size()).forEach(this::createPresence);
        for (int i = 0; true; i = (i + 1) % extraBots.size()) {
            // 20% chance of changing the presence of bot i.
            if(random.nextDouble() < 0.2) {
                // Marks bot extraBots[i] as present in a random topic.
                createPresence(i);
            }
            // Gets the UserHandler for a random topic. The topic is always valid.
            UserHandler handler = handlerMap.get(ids.get(random.nextInt(ids.size())));
            // Ignore if has users (eg: someone is editing the topic).
            // We want topics with users to be active.
            if(handler.hasUsers) continue;
            // Randomly change the presence in the topic.
            handler.presenceManager.markAsPresent(random.nextBoolean());

            // Sleep for 500 milliseconds
            doWait(500);

            // Run 1 iteration for each edit bot.
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

    // Wait, ignore InterrupedException
    private void doWait(int milliseconds) {
        try {
            wait(milliseconds);
        } catch (InterruptedException e) {
            logger.error("Thread interrupted", e);
        }
    }

    /**
     * Creates the specified number of {@link UserInfo} instances, which will
     * be used for bots.
     *
     * @param count number of bot users to create.
     * @return List of {@link UserInfo} instances created.
     */
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
            // Still not available.
            return false;
        }
        fillPresenceObservers();
        return true;
    }

    /**
     * Makes extraBot[i] present in a random topic.
     * @param i Bot to change (index in extraBots list)
     */
    private void createPresence(int i) {
        String topic = createTopic(random.nextInt(20) + 1);
        ExtraBot bot = extraBots.get(i);
        PresenceManager previousPresenceManager = bot.presenceManager;
        if (previousPresenceManager != null && topic
            .equals(previousPresenceManager.getTopicId())) {
            //Nothing to do. The Bot was already present in that topic.
            return;
        }
        // Close the PresenceManager for the previous topic.
        if (previousPresenceManager != null) {
            bot.presenceManager = null;
            previousPresenceManager.close();
        }
        // Create a new PresenceManager in the new topic.
        bot.presenceManager = new PresenceManager(
            ce.getSystemContext(), bot.userInfo, topic, ce);
        bot.presenceManager.markAsPresent(true);
    }

    /**
     * Fills handlerMap. For each of the available {@link Person} instances,
     * a bot will be assigned and a new PresenceManager will be created.
     * The PresenceManager has a 50% chance of being marked as present initially.
     */
    private void fillPresenceObservers() {
        Map<Integer, UserHandler> handlerMap = new HashMap<>();
        for (Integer id : ids) {
            UserInfo userInfo = bots.get(id % bots.size());
            String topic = createTopic(id);
            PresenceManager presenceManager = new PresenceManager(
                ce.getSystemContext(), userInfo, topic, ce);
            presenceManager.markAsPresent(random.nextBoolean());
            UserHandler userHandler = new UserHandler(id, userInfo, topic,
                presenceManager);
            presenceManager.setPresenceHandler(userHandler);
            handlerMap.put(id, userHandler);
        }

        this.handlerMap = Collections.unmodifiableMap(handlerMap);
    }

    /**
     * Creates a String representing the topic for a given {@link Person} id.
     * @param id id of the {@link Person} entity
     * @return topic string
     */
    private String createTopic(Integer id) {
        return String.format("person/%d", id);
    }

    /**
     * PresenceHandler which observes a topic. When a real user connects, the
     * assigned bot will be marked as present in the topic and a new editor bot
     * will be created.
     *
     * A real user connects to the topic when the EditorView is open for that
     * {@link Person} entity.
     */
    class UserHandler implements PresenceHandler {

        /**
         * Id of the {@link Person} entity.
         */
        private Integer id;
        /**
         * Bot {@link UserInfo}
         */
        private UserInfo userInfo;
        private String topic;
        private PresenceManager presenceManager;

        /**
         * List of real users (not bots).
         */
        private Set<UserInfo> users = new HashSet<>();
        /**
         * If there are real users connected to the topic.
         */
        private volatile boolean hasUsers;

        public UserHandler(Integer id, UserInfo userInfo, String topic,
            PresenceManager presenceManager) {
            this.id = id;
            this.userInfo = userInfo;
            this.topic = topic;
            this.presenceManager = presenceManager;
        }

        @Override
        public Registration handlePresence(PresenceContext context) {
            UserInfo user = context.getUser();
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
                .openTopicConnection(ce.getSystemContext(), topicId,
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
                refreshGridMap.put(String.valueOf(person.getId()), person);

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
