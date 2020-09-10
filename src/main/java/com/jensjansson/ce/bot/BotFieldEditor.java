package com.jensjansson.ce.bot;

import static com.jensjansson.ce.bot.BotRunner.sleepRandom;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.util.Supplier;

import com.jensjansson.ce.data.entity.Person;
import com.jensjansson.ce.data.generator.DataGenerator;
import com.jensjansson.ce.views.persons.PersonsView;

import com.vaadin.collaborationengine.CollaborationBinderUtil;
import com.vaadin.collaborationengine.TopicConnection;
import com.vaadin.collaborationengine.UserInfo;

class BotFieldEditor {

    private static Map<String, Supplier<Object>> fieldToValueProvider = new ConcurrentHashMap<>();

    static {
        fieldToValueProvider.put("firstName",
                () -> generatePerson().getFirstName());
        fieldToValueProvider.put("lastName",
                () -> generatePerson().getLastName());
        fieldToValueProvider.put("email", () -> generatePerson().getEmail());
        fieldToValueProvider.put("happiness",
                () -> getRandomEntry(PersonsView.HAPPINESS_VALUES));
    }

    static void editRandomField(TopicConnection topic, UserInfo user) {

        String propertyName = getRandomEntry(fieldToValueProvider.keySet());

        Object value = fieldToValueProvider.get(propertyName).get();

        sleepRandom(1, 3);

        if (propertyName.equals("happiness")) {
            CollaborationBinderUtil.addEditor(topic, propertyName, user,
                    PersonsView.HAPPINESS_VALUES.indexOf(value));
            // RadioButtonGroup changes the value immediately when
            CollaborationBinderUtil.setFieldValue(topic, propertyName, value);
            sleepRandom(3, 5);
        } else {
            CollaborationBinderUtil.addEditor(topic, propertyName, user);

            sleepRandom(3, 5);
            CollaborationBinderUtil.setFieldValue(topic, propertyName, value);

            sleepRandom(0, 2);
        }

        CollaborationBinderUtil.removeEditor(topic, propertyName, user);

    }

    private static Person generatePerson() {
        return DataGenerator.generateData(1, (long) (Math.random() * 1000))
                .get(0);
    }

    private static <T> T getRandomEntry(Collection<T> collection) {
        return collection.stream()
                .skip((int) (collection.size() * Math.random())).findFirst()
                .get();
    }

}
