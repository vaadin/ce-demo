package com.jensjansson.ce.bot;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

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
            String oldValue = (String) map.get(MAP_KEY);
            List<UserInfo> oldUsers = jsonToUsers(oldValue);
            List<UserInfo> newUsers = updater.apply(oldUsers.stream())
                    .collect(Collectors.toList());
            if (map.replace(MAP_KEY, oldValue, usersToJson(newUsers))) {
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
        List<UserInfo> users = jsonToUsers((String) map.get(MAP_KEY));
        if (users == null) {
            return 0;
        }
        return (int) users.stream().filter(filter).count();
    }

    private static CollaborationMap getMap(TopicConnection topic) {
        return topic.getNamedMap(CollaborationAvatarGroup.class.getName());
    }

    private static String usersToJson(List<UserInfo> users) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(users);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(
                    "Failed to encode the list of users as a JSON string.", e);
        }
    }

    private static List<UserInfo> jsonToUsers(String json) {
        if (json == null) {
            return Collections.emptyList();
        }
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(json,
                    new TypeReference<List<UserInfo>>() {
                    });
        } catch (JsonProcessingException e) {
            throw new RuntimeException(
                    "Failed to parse the list of users from a JSON string.", e);
        }
    }
}
