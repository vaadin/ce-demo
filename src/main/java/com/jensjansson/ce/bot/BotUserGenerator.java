package com.jensjansson.ce.bot;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import com.vaadin.collaborationengine.UserInfo;

public class BotUserGenerator {

    static final String BOT_ID_PREFIX = "bot-";

    private static List<String> names = Arrays.asList("Jens", "Leif", "Tan",
            "Pekka", "Yuriy", "Serhii");

    private static AtomicInteger counter = new AtomicInteger(0);

    public static UserInfo generateBotUser() {
       return generateBotUser(BOT_ID_PREFIX);
    }

    public static UserInfo generateBotUser(String prefix) {
        UserInfo user = new UserInfo(
                prefix + UUID.randomUUID().toString());
        user.setName("Bot " + names.get(counter.getAndUpdate(oldValue -> {
            if (oldValue < names.size() - 1) {
                return oldValue + 1;
            } else {
                return 0;
            }
        })));
        return user;
    }

}
