package com.jensjansson.ce.bot;

import static com.jensjansson.ce.bot.BotAvatarUtil.addAvatar;
import static com.jensjansson.ce.bot.BotAvatarUtil.getBotCount;
import static com.jensjansson.ce.bot.BotAvatarUtil.getRealUserCount;
import static com.jensjansson.ce.bot.BotAvatarUtil.onUsersChanged;
import static com.jensjansson.ce.bot.BotAvatarUtil.removeAvatar;

import com.vaadin.collaborationengine.ActivationHandler;
import com.vaadin.collaborationengine.CollaborationEngine;
import com.vaadin.collaborationengine.ConnectionContext;
import com.vaadin.collaborationengine.TopicConnection;
import com.vaadin.collaborationengine.UserInfo;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.shared.Registration;

public class BotRunner implements Runnable {

    public static void onUserJoined(String topicId) {
        CollaborationEngine.getInstance()
                .openTopicConnection(new ConnectionContext() {
                    @Override
                    public Registration setActivationHandler(
                            ActivationHandler activationHandler) {
                        activationHandler.setActive(true);
                        return null;
                    }

                    @Override
                    public void dispatchAction(Command command) {
                        command.execute();
                    }
                }, topicId, topic -> {
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
                    BotRunner botRunner = new BotRunner(topic);
                    new Thread(botRunner).start();
                    return () -> botRunner.shouldStop = true;
                });
    }

    private TopicConnection topic;
    private UserInfo user;
    private volatile boolean shouldStop;

    public BotRunner(TopicConnection topic) {
        this.topic = topic;
        user = BotUserGenerator.generateBotUser();

        onUsersChanged(topic, () -> {
            shouldStop = getRealUserCount(topic) == 0;
        });
    }

    @Override
    public void run() {
        sleepRandom(1, 3);
        addAvatar(topic, user);

        while (!shouldStop) {
            BotFieldEditor.editRandomField(topic, user);
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

}
