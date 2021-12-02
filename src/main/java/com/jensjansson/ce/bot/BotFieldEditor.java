package com.jensjansson.ce.bot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.vaadin.collaborationengine.CollaborationBinder;
import com.vaadin.collaborationengine.CollaborationBinderUtil;
import com.vaadin.collaborationengine.TopicConnection;
import com.vaadin.collaborationengine.UserInfo;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jensjansson.ce.data.entity.Person;
import com.jensjansson.ce.data.generator.DataGenerator;
import com.jensjansson.ce.views.persons.EditorView;
import org.apache.logging.log4j.util.Supplier;

class BotFieldEditor {

    private static Map<String, Supplier<Object>> fieldToValueProvider = new ConcurrentHashMap<>();

    static {
        fieldToValueProvider.put("firstName",
                () -> generatePerson().getFirstName());
        fieldToValueProvider.put("lastName",
                () -> generatePerson().getLastName());
        fieldToValueProvider.put("email", () -> generatePerson().getEmail());
        fieldToValueProvider.put("happiness",
                () -> getRandomEntry(EditorView.HAPPINESS_VALUES));
    }

    static List<Runnable> editRandomField(TopicConnection topic,
        UserInfo user) {

        List<Runnable> result = new ArrayList<>();
        String propertyName = getRandomEntry(fieldToValueProvider.keySet());

        Object value = fieldToValueProvider.get(propertyName).get();

        if (propertyName.equals("happiness")) {
            result.add(() -> {
                CollaborationBinderUtil.addEditor(topic, propertyName, user,
                    EditorView.HAPPINESS_VALUES.indexOf(value));
                // RadioButtonGroup changes the value immediately when
                CollaborationBinderUtil
                    .setFieldValue(topic, propertyName, value);
            });
        } else {
            result.add(() -> CollaborationBinderUtil
                .addEditor(topic, propertyName, user));

            result.add(() -> {
                // Skip changing the value if there's another focused user
                if (getEditorCount(topic, propertyName) < 2) {
                    CollaborationBinderUtil
                        .setFieldValue(topic, propertyName, value);
                }
            });
        }

        result.add(() -> CollaborationBinderUtil.removeEditor(topic, propertyName, user));
        return result;
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

    private static int getEditorCount(TopicConnection topic,
            String propertyName) {
        return (int) topic.getNamedList(CollaborationBinder.class.getName()).getItems(ObjectNode.class)
            .stream().filter(node -> propertyName.equals(node.get("propertyName").asText())).count();
    }

}
