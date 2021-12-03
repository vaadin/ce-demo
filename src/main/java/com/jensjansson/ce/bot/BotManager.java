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

    /**
     * Number of main bots ({@link UserInfo} instances.
     * Each bot will be connected to a number of Person's topics.
     */
    private static final int botCount = 20;

    /**
     * The images in /images/avatar go from 1.png to 8.png
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
    /**
     * Main bots. There are 20 in total, each will connect to a certain number
     * of Person's topics.
     */
    private List<UserInfo> bots;

    /**
     * Extra bots that change topics randomly.
     */
    private List<ExtraBot> extraBots;

    /**
     * Maps a {@link UserHandler} to a {@link Person#getId()}.
     * The {@link UserHandler} contains
     */
    private Map<Integer,UserHandler> handlerMap;

    /**
     * Maps a {@link EditBot} to an {@link Person#getId()}. The entry will be added to
     * this map when a real person connects to the topic by opening the edit
     * window. It will be cleared when all real users have left.
     */
    private ConcurrentHashMap<Integer, EditBot> editBotMap = new ConcurrentHashMap<>();

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

    @Override
    public synchronized void run() {
        // We need to wait until the DataGenerator has run and initialized the
        // database.
        boolean initialized = false;
        while (!initialized) {
            doWait(1000);
            initialized = initializeIfPossible();
        }

        // Make the extra bots present in random topics.
        IntStream.range(0,extraBots.size()).forEach(this::createPresence);

        // Loop forever, i goes from 0 to extraBots.size() - 1
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
            editBotMap.values().stream().filter(b -> !b.shouldStop)
                .collect(Collectors.toList()).forEach(editBot -> {
                try {
                    editBot.run();
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
         * If there are real users (not bots) connected to the topic.
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
            // Update will change the status of the bot. It is only called
            // when a real user connects or disconnects.
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

        /**
         *
         * @param user {@link UserInfo} to check
         * @return true if the user it NOT a bot. False otherwise.
         */
        private boolean isRealUser(UserInfo user) {
            String id = user.getId();
            return !id.startsWith(BotUserGenerator.BOT_ID_PREFIX) && !id
                .startsWith(BotManager.BOT_PREFIX);
        }

        /**
         * Updates the status of this bot if hasUsers changes.
         * hasUsers will be true if the users collection is not empty.
         * The users collection contains all users connected to this topic that
         * are not bots.
         *
         * If hasUsers changes from false to true,
         * {@link PresenceManager#markAsPresent(boolean)} is called with
         * <code>true</code> and a new {@link EditBot} is created and placed in
         * editBotMap.
         *
         * If hasUsers changes from true to false, the {@link EditBot} will be
         * removed from the editBotMap and discarded.
         */
        private void update() {
            boolean hadUsers = hasUsers;
            hasUsers = !users.isEmpty();
            if (hadUsers == hasUsers) {
                // If hasUsers hasn't changed, nothing needs to be done.
                return;
            }

            if (hasUsers) {
                // The first real user connected to the topic.
                presenceManager.markAsPresent(true);
                Person person = personService.get(id).orElse(null);
                editBotMap.computeIfAbsent(id,
                    id -> new EditBot(topic, userInfo, person));
            } else {
                // All real users have left the topic.
                EditBot editBot = editBotMap.remove(id);
                if (editBot != null) {
                    editBot.stop();
                } else {
                    // This is not expected to happen.
                    logger.debug("EditBot not removed for {}", id);
                }
            }
        }

    }

    /**
     * This is a bot that makes changes to the person entity values at certain intervals.
     */
    class EditBot implements Runnable {
        /**
         * How much time to wait between edits.
         */
        private final int delayInSeconds = 2;

        /**
         * UserInfo for the bot.
         */
        UserInfo user;
        volatile TopicConnection topic;
        volatile Registration topicRegistration;
        /**
         * Entity that will be changed.
         */
        Person person;
        /**
         * Is set to true when the topic is disconnected or when close is called.
         */
        volatile boolean shouldStop;

        /**
         * How many edits to do before saving.
         */
        int saveAfter = generateNumberOfEditsBeforeSave();
        /**
         * How many edits have been done since the last save.
         */
        int editCounter = 0;
        /**
         * Last time the run method was called. Used to determine if enough
         * time has passed between edits.
         */
        Instant lastExecution = null;
        /**
         * A single edit corresponds to multiple steps.
         * Typically, those are:
         * <ol>
         *     <li>{@link com.vaadin.collaborationengine.CollaborationBinderUtil#addEditor(TopicConnection, String, UserInfo, int)}</li>
         *     <li>{@link com.vaadin.collaborationengine.CollaborationBinderUtil#setFieldValue(TopicConnection, String, Object)}</li>
         *     <li>{@link com.vaadin.collaborationengine.CollaborationBinderUtil#removeEditor(TopicConnection, String, UserInfo)}</li>
         * </ol>
         * There needs to be a delay between each step so the user can see what is happening.
         *
         * @see BotFieldEditor#editRandomField(TopicConnection, UserInfo)
         */
        ListIterator<Runnable> currentEditSteps;

        EditBot(String topicId, UserInfo user, Person person) {
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

        /**
         * Called by the main thread every 500 milliseconds while present
         * in the editBotMap.
         *
         * Nothing will be done if the bot is stopped, not connected to the
         * topic or if not enough time has passed between edits.
         *
         * Otherwise, one of the following will happen
         * <ol>
         *     <li>If there is no edit in progress, and not enough edits have
         *     been performed,
         *     {@link BotFieldEditor#editRandomField(TopicConnection, UserInfo)}
         *     is called to create a new edit.</li>
         *     <li>If there is an edit in progress, the next step will be executed.
         *     If it is the last step of the edit, editCount will be incremented.
         *     </li>
         *     <li>If enough edits have been done and no edit is in progress, the
         *     entity will be saved in the database.</li>
         * </ol>
         */
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
                // Enough edits have been performed, save the entity to the
                // database and send a notification to the topic.
                log("Called save");
                BotSaver.save(ce, topic, person, personService, user);
                refreshGridMap.put(String.valueOf(person.getId()), person);

                // reset values for the next edit.
                editCounter = 0;
                saveAfter = generateNumberOfEditsBeforeSave();
                currentEditSteps = null;
            } else if (currentEditSteps != null && currentEditSteps.hasNext()) {
                // There is an edit in progress. Run the next step.
                currentEditSteps.next().run();
                if(!currentEditSteps.hasNext()) {
                    // If this was the last step of the current edit,
                    // increment editCounter
                    ++editCounter;
                }
            } else {
                // Create a new random edit.
                log("Random edit");
                currentEditSteps = BotFieldEditor.editRandomField(topic, user)
                    .listIterator();
            }
        }

        /**
         * Disconnect from topic and mark bot for removal.
         */
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
        /**
         * Generates a random number that will be used as the number of edits a
         * specific bot will do before saving the entity.
         *
         * @return A random number between 2 and 5.
         */
        private int generateNumberOfEditsBeforeSave() {
            return 2 + (random.nextInt(4));
        }

    }

    /**
     * Each {@link Person} in the database has one of the 20 main bot users
     * assigned to it. Additionally, there are 5 of these ExtraBots that
     * connect to random Person's topics.
     */
    static class ExtraBot {
       /**
        *  {@link UserInfo} of the bot.
        */
       private UserInfo userInfo;

        /**
         * Current presenceManager. When the bot changes topic, the current
         * presence manager is closed and a new one is created
         */
        private PresenceManager presenceManager;

       public ExtraBot(UserInfo userInfo) {
            this.userInfo = userInfo;
        }
    }
}
