package com.jensjansson.ce.bot;

import java.io.IOException;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jensjansson.ce.data.entity.Person;
import com.jensjansson.ce.data.service.PersonService;
import com.jensjansson.ce.views.persons.EditorView;

import com.vaadin.collaborationengine.CollaborationBinder;
import com.vaadin.collaborationengine.CollaborationEngine;
import com.vaadin.collaborationengine.TopicConnection;
import com.vaadin.collaborationengine.UserInfo;

class BotSaver {

    static void save(CollaborationEngine ce, TopicConnection personTopic,
            Integer personId, PersonService personService, UserInfo user) {
        Person person = getPersonFromFields(personTopic);
        person.setId(personId);
        personService.update(person);
        ce.openTopicConnection(new EagerConnectionContext(), "refreshGrid",
                user, topic -> {
                    topic.getNamedMap("refreshGrid").put("refreshGrid",
                            UUID.randomUUID().toString());
                    return null;
                });
        EditorView.sendSaveNotification(personTopic.getNamedMap("save"), user);
    }

    private static Person getPersonFromFields(TopicConnection topic) {
        Person person = new Person();
        person.setFirstName(getFieldValue(topic, "firstName", String.class));
        person.setLastName(getFieldValue(topic, "lastName", String.class));
        person.setEmail(getFieldValue(topic, "email", String.class));
        person.setPhoneNumber(
                getFieldValue(topic, "phoneNumber", String.class));
        person.setHappiness(getFieldValue(topic, "happiness", String.class));
        return person;
    }

    private static <T> T getFieldValue(TopicConnection topic,
            String propertyName, Class<T> valueType) {
        ObjectNode fieldStateJson = topic
                .getNamedMap(CollaborationBinder.class.getName())
                .get(propertyName, ObjectNode.class);
        if (fieldStateJson == null || fieldStateJson.isEmpty()) {
            return null;
        }

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.treeToValue(fieldStateJson.get("value"),
                    valueType);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
