package com.jensjansson.ce.views.persons;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jensjansson.ce.bot.BotRunner;
import com.jensjansson.ce.data.entity.Person;
import com.jensjansson.ce.data.service.PersonService;

import com.vaadin.collaborationengine.CollaborationAvatarGroup;
import com.vaadin.collaborationengine.CollaborationBinder;
import com.vaadin.collaborationengine.CollaborationEngine;
import com.vaadin.collaborationengine.CollaborationMap;
import com.vaadin.collaborationengine.CollaborationMessageInput;
import com.vaadin.collaborationengine.CollaborationMessageList;
import com.vaadin.collaborationengine.UserInfo;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.shared.Registration;

public class EditorView extends Div {

    public interface SaveNotifier {
        public void updateGrid(Person person);

        public void stopEditingPerson();
    }

    public static final List<String> HAPPINESS_VALUES = Arrays.asList(
            "Raptorous", "Ecstatic", "Joyful", "Indifferent", "Dreadful");

    CollaborationMessageList list;
    CollaborationMessageInput input;

    private Button close = new Button("Close");
    private Button save = new Button("Save");

    private CollaborationAvatarGroup avatarGroup;
    private UserInfo localUser;
    private PersonService personService;
    private CollaborationBinder<Person> binder;

    private Person person;

    private CollaborationMap saveMap;
    private Registration topicConnectionRegistration;

    public EditorView(UserInfo localUser, PersonService personService,
            SaveNotifier saveNotifier) {
        this.localUser = localUser;
        this.personService = personService;

        // the grid valueChangeEvent will clear the form too
        close.addClickListener(e -> saveNotifier.stopEditingPerson());

        save.addClickListener(e -> {
            try {
                binder.writeBean(person);
                if (this.person != null) {
                    person.setId(this.person.getId());
                }
                saveNotifier.updateGrid(person);
                personService.update(person);
                sendSaveNotification();
            } catch (ValidationException validationException) {
                validationException.printStackTrace();
                Notification.show(
                        "An exception happened while trying to store the person details.");
            }
        });

        Div form = createForm();
        Div comments = createComments();
        add(form, comments);
        addClassNames("flex", "flex-row");
        setSizeFull();
    }

    private Div createForm() {
        H2 header = new H2("Edit person");
        header.addClassNames("m-0");
        Div headerLayout = new Div(header);
        headerLayout.addClassNames("border-b", "border-contrast-10",
                "box-border", "flex", "flex-row", "h-xl", "px-m",
                "items-center", "w-full", "flex-shrink-0");

        H3 personalHeader = new H3("Personal information");
        TextField firstName = new TextField("First name");
        TextField lastName = new TextField("Last name");
        RadioButtonGroup<String> happiness = new RadioButtonGroup<>();
        happiness.setLabel("How excited are they?");
        happiness.setItems(HAPPINESS_VALUES);
        happiness.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);
        happiness.setValue("Raptorous");
        TextField email = new TextField("Email");
        email.setValueChangeMode(ValueChangeMode.EAGER);
        TextField phoneNumber = new TextField("Phone number");
        H3 employmentHeader = new H3("Employment information");
        TextField title = new TextField("Title");
        ComboBox<String> department = new ComboBox<>("Department");
        department.setItems("Engineering", "Product", "Marketing",
                "Customer Success", "Operations", "Sales");
        ComboBox<String> team = new ComboBox<>("Team");
        team.setItems("Core products", "Added value", "Support", "Design");

        // Configure Form
        binder = new CollaborationBinder<>(Person.class, localUser);
        binder.forField(firstName).bind("firstName");
        binder.forField(lastName).bind("lastName");
        binder.forField(email).bind("email");
        binder.forField(phoneNumber).bind("phoneNumber");
        binder.forField(title).bind("title");
        binder.forField(department).bind("department");
        binder.forField(team).bind("team");

        // Bind fields. This where you'd define e.g. validation rules
        binder.bindInstanceFields(this);

        Div formLayout = new Div();
        formLayout.add(personalHeader, firstName, lastName, email, phoneNumber,
                employmentHeader, title, department, team, happiness);
        formLayout.addClassNames("flex", "flex-col", "flex-grow", "flex-shrink",
                "p-m", "overflow-auto");

        Div buttonLayout = new Div();
        buttonLayout.addClassNames("border-t", "border-contrast-10",
                "bg-contrast-5", "box-border", "flex", "flex-row", "h-xl",
                "items-center", "p-s", "w-full", "flex-shrink-0", "justify-end",
                "spacing-e-m", "h-xl");
        close.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        buttonLayout.add(close, save);

        Div layout = new Div(headerLayout, formLayout, buttonLayout);

        layout.addClassNames("flex", "flex-col", "flex-grow");
        layout.setSizeFull();
        return layout;

    }

    private Div createComments() {
        H2 header = new H2("Chat");
        header.addClassNames("flex-grow", "m-0");
        avatarGroup = new CollaborationAvatarGroup(localUser, null);
        avatarGroup.setMaxItemsVisible(4);
        avatarGroup.setWidth(null);
        avatarGroup.addClassNames("width-auto");
        Div headerLayout = new Div(header, avatarGroup);
        headerLayout.addClassNames("border-b", "border-l", "border-contrast-10",
                "box-border", "flex", "flex-row", "items-center", "px-m",
                "w-full", "flex-shrink-0", "h-xl");
        list = new CollaborationMessageList(localUser, null);
        list.addClassNames("border-l", "border-contrast-10", "flex-grow");
        input = new CollaborationMessageInput(localUser, null);
        input.addClassNames("border-t", "border-l", "border-contrast-10",
                "bg-contrast-5", "box-border", "flex", "flex-row",
                "items-center", "p-s", "w-full", "flex-shrink-0", "min-h-xl",
                "items-end");
        Div layout = new Div(headerLayout, list, input);
        layout.addClassNames("flex", "flex-col", "flex-grow");
        list.setSizeFull();
        input.setWidthFull();
        layout.setSizeFull();
        return layout;
    }

    protected void editPerson(Person person) {
        this.person = person;
        // Value can be null as well, that clears the form
        String topicId = null;
        if (person != null && person.getId() != null) {
            topicId = "person/" + String.valueOf(person.getId());
        }
        binder.setTopic(topicId, () -> person);
        avatarGroup.setTopic(topicId);
        list.setTopic(topicId);
        input.setTopic(topicId);
        System.out.println("Topic: " + topicId);
        if (topicId != null) {
            BotRunner.onUserJoined(topicId, localUser, person, personService);
        }
        connectToSaveNotifications(topicId);
    }

    private void connectToSaveNotifications(String topicId) {
        saveMap = null;
        if (topicConnectionRegistration != null) {
            topicConnectionRegistration.remove();
        }
        if (topicId != null) {
            topicConnectionRegistration = CollaborationEngine.getInstance()
                    .openTopicConnection(this, topicId, localUser,
                            topicConnection -> {
                                saveMap = topicConnection.getNamedMap("save");
                                saveMap.subscribe(e -> {
                                    if (e.getValue(Object.class) == null) {
                                        return;
                                    }
                                    ObjectMapper om = new ObjectMapper();
                                    try {
                                        JsonNode jsonNode = om.readTree(
                                                e.getValue(String.class));
                                        String savingUser = jsonNode
                                                .get("userName").asText();
                                        String savingUserId = jsonNode
                                                .get("userId").asText();
                                        if (Objects.equals(savingUserId,
                                                localUser.getId())) {
                                            savingUser = "you";
                                        }
                                        showSaveNotification(savingUser);
                                    } catch (JsonProcessingException jsonProcessingException) {
                                        jsonProcessingException
                                                .printStackTrace();
                                    }
                                });
                                return null;
                            });
        }
    }

    private void showSaveNotification(String username) {
        Notification notification = new Notification(
                "Changes saved by " + username);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        notification.setDuration(5000);
        notification.open();
    }

    private void sendSaveNotification() {
        if (saveMap != null) {
            sendSaveNotification(saveMap, localUser);
        } else {
            showSaveNotification(localUser.getName());
        }
    }

    public static void sendSaveNotification(CollaborationMap map,
            UserInfo user) {
        ObjectMapper om = new ObjectMapper();
        ObjectNode objectNode = om.createObjectNode();
        objectNode.put("userName", user.getName());
        objectNode.put("userId", user.getId());
        objectNode.put("messageId", UUID.randomUUID().toString());
        map.put("save", objectNode.toString());
        // Value needs to be cleared right away, or the notification
        // will be shown when starting to edit the item, and thus
        // connecting to the topic.
        map.put("save", null);
    }
}
