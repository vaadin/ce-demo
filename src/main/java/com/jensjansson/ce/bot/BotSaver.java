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
            Person person, PersonService personService, UserInfo user) {
        getPersonFromFields(person, personTopic);
        personService.update(person);
        EditorView.sendSaveNotification(personTopic.getNamedMap("save"), user);
    }

    private static Person getPersonFromFields(Person person,
            TopicConnection topic) {
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
        return topic.getNamedMap(CollaborationBinder.class.getName())
                .get(propertyName, valueType);
    }

}
