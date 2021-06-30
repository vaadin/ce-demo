package com.jensjansson.ce.bot;

import com.jensjansson.ce.data.entity.Person;
import com.jensjansson.ce.data.service.PersonService;

import com.vaadin.collaborationengine.CollaborationEngine;
import com.vaadin.collaborationengine.TopicConnection;
import com.vaadin.collaborationengine.TopicConnectionRegistration;
import com.vaadin.collaborationengine.UserInfo;

import static com.jensjansson.ce.bot.BotAvatarUtil.addAvatar;
import static com.jensjansson.ce.bot.BotAvatarUtil.getBotCount;
import static com.jensjansson.ce.bot.BotAvatarUtil.getRealUserCount;
import static com.jensjansson.ce.bot.BotAvatarUtil.onUsersChanged;
import static com.jensjansson.ce.bot.BotAvatarUtil.removeAvatar;

public class BotRunner implements Runnable {

    public static TopicConnectionRegistration onUserJoined(String topicId, UserInfo localUser,
            Person person, PersonService personService) {
        CollaborationEngine ce = CollaborationEngine.getInstance();
        return ce.openTopicConnection(new EagerConnectionContext(), topicId, localUser,
                topic -> {
                    if (getBotCount(topic) > 0) {
                        // Only one bot at a time for each item
                        return null;
                    }
                    /*
                     * TODO: Use Timer instead of spawning multiple Threads.
                     * Having many threads is not efficient, but this is not a
                     * very serious issue as long as the number of items in the
                     * grid (and thus bot threads) is limited as currently.
                     */
                    BotRunner botRunner = new BotRunner(ce, topic, person,
                            personService);
                    new Thread(botRunner).start();
                    return () -> botRunner.shouldStop = true;
                });
    }

    private CollaborationEngine ce;
    private TopicConnection topic;
    private UserInfo user;
    private volatile boolean shouldStop;

    private Person person;
    private PersonService personService;

    public BotRunner(CollaborationEngine ce, TopicConnection topic,
            Person person, PersonService personService) {
        this.ce = ce;
        this.topic = topic;
        this.person = person;
        this.personService = personService;
        user = BotUserGenerator.generateBotUser();

        onUsersChanged(topic, () -> {
            shouldStop = getRealUserCount(topic) == 0;
        });
    }

    @Override
    public void run() {
        sleepRandom(1, 3);

        if (getBotCount(topic) > 0) {
            // Multiple users joined during the sleep period, and another bot
            // thread added its avatar already.
            return;
        }

        addAvatar(topic, user);

        int editCounter = 0;
        int saveAfter = generateNumberOfEditsBeforeSave();

        while (!shouldStop) {
            BotFieldEditor.editRandomField(topic, user);

            editCounter++;
            if (editCounter >= saveAfter) {
                sleepRandom(1, 2);
                BotSaver.save(ce, topic, person, personService, user);

                editCounter = 0;
                saveAfter = generateNumberOfEditsBeforeSave();
            }
        }
        removeAvatar(topic, user);
    }

    public static void sleepRandom(double lowerBoundSeconds,
            double upperBoundSeconds) {
        double seconds = (Math.random()
                * (upperBoundSeconds - lowerBoundSeconds)) + lowerBoundSeconds;
        sleep(seconds);
    }

    public static void sleep(double seconds) {
        try {
            Thread.sleep((long) (seconds * 1000));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static int generateNumberOfEditsBeforeSave() {
        return 2 + (int) (Math.random() * 4);
    }

}
