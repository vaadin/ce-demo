package com.jensjansson.ce.bot;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.collaborationengine.CollaborationAvatarGroup;
import com.vaadin.collaborationengine.CollaborationMap;
import com.vaadin.collaborationengine.TopicConnection;
import com.vaadin.collaborationengine.UserInfo;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.server.Command;

class BotAvatarUtil {

    private static final String MAP_KEY = "users";

    public static void addAvatar(TopicConnection topic, UserInfo user) {
        updateAvatars(topic,
                oldValue -> Stream.concat(oldValue, Stream.of(user)));
    }

    public static void removeAvatar(TopicConnection topic, UserInfo user) {
        updateAvatars(topic,
                oldValue -> oldValue.filter(u -> !Objects.equals(u, user)));
    }

    private static void updateAvatars(TopicConnection topic,
            SerializableFunction<Stream<UserInfo>, Stream<UserInfo>> updater) {
        CollaborationMap map = getMap(topic);
        while (true) {
            List<UserInfo> oldValue = (List<UserInfo>) map.get(MAP_KEY);
            List<UserInfo> newValue = updater.apply(
                    oldValue != null ? oldValue.stream() : Stream.empty())
                    .collect(Collectors.toList());
            if (map.replace(MAP_KEY, oldValue, newValue)) {
                break;
            }
        }
    }

    public static void onUsersChanged(TopicConnection topic, Command action) {
        getMap(topic).subscribe(e -> action.execute());
    }

    public static int getBotCount(TopicConnection topic) {
        return getUserCount(topic,
                u -> u.getId().startsWith(BotUserGenerator.BOT_ID_PREFIX));
    }

    public static int getRealUserCount(TopicConnection topic) {
        return getUserCount(topic,
                u -> !u.getId().startsWith(BotUserGenerator.BOT_ID_PREFIX));
    }

    private static int getUserCount(TopicConnection topic,
            Predicate<UserInfo> filter) {
        CollaborationMap map = getMap(topic);
        List<UserInfo> users = (List<UserInfo>) map.get(MAP_KEY);
        if (users == null) {
            return 0;
        }
        return (int) users.stream().filter(filter).count();
    }

    private static CollaborationMap getMap(TopicConnection topic) {
        return topic.getNamedMap(CollaborationAvatarGroup.class.getName());
    }
}
