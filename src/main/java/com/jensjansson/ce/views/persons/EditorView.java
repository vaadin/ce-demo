package com.jensjansson.ce.views.persons;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jensjansson.ce.data.entity.Person;
import com.jensjansson.ce.data.service.PersonService;

import com.vaadin.collaborationengine.CollaborationAvatarGroup;
import com.vaadin.collaborationengine.CollaborationBinder;
import com.vaadin.collaborationengine.CollaborationEngine;
import com.vaadin.collaborationengine.CollaborationMap;
import com.vaadin.collaborationengine.CollaborationMessageInput;
import com.vaadin.collaborationengine.CollaborationMessageList;
import com.vaadin.collaborationengine.TopicConnectionRegistration;
import com.vaadin.collaborationengine.UserInfo;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.shared.Registration;

public class EditorView extends Div {

    public interface EditorActionNotifier {
        public void updateGrid(Person person);

        public void stopEditingPerson();

        public void deletePerson();
    }

    public static final List<String> HAPPINESS_VALUES = Arrays.asList(
            "Raptorous", "Ecstatic", "Joyful", "Indifferent", "Dreadful");

    private Tabs tabs;
    private Section details;
    private Section comments;

    CollaborationMessageList list;
    CollaborationMessageInput input;

    private Button close = new Button();
    private Button delete = new Button("Delete...");
    private Button cancel = new Button("Cancel");
    private Button save = new Button("Save");

    private CollaborationAvatarGroup avatarGroup;
    private UserInfo localUser;
    private PersonService personService;
    private EditorActionNotifier editorActionNotifier;
    private CollaborationBinder<Person> binder;

    private Person person;

    private CollaborationMap saveMap;
    private Registration topicConnectionRegistration;
    private TopicConnectionRegistration expirationRegistration;

    public EditorView(UserInfo localUser, PersonService personService,
            EditorActionNotifier editorActionNotifier) {
        this.localUser = localUser;
        this.personService = personService;
        this.editorActionNotifier = editorActionNotifier;

        addClassNames("editor-view", "flex", "flex-col");
        setHeightFull();

        // the grid valueChangeEvent will clear the form too
        close.addClickListener(e -> editorActionNotifier.stopEditingPerson());
        delete.addClickListener(e -> editorActionNotifier.deletePerson());
        cancel.addClickListener(e -> editorActionNotifier.stopEditingPerson());
        save.addClickListener(e -> {
            try {
                binder.writeBean(person);
                if (this.person != null) {
                    person.setId(this.person.getId());
                }
                editorActionNotifier.updateGrid(person);
                personService.update(person);
                sendSaveNotification();
            } catch (ValidationException validationException) {
                validationException.printStackTrace();
                Notification.show(
                        "An exception happened while trying to store the person details.");
            }
        });

        Header header = createHeader();
        details = createDetails();
        comments = createComments();
        Div content = new Div(details, comments);
        content.addClassNames("flex", "flex-grow", "overflow-auto");
        add(header, content);
    }

    private Header createHeader() {
        H2 heading = new H2("Edit employee");
        heading.addClassNames("text-xl", "lg:text-2xl", "my-0", "mr-auto");

        avatarGroup = new CollaborationAvatarGroup(localUser, null);
        avatarGroup.addClassName("mx-m");
        avatarGroup.setMaxItemsVisible(4);
        avatarGroup.setWidth("auto");

        close.addClassNames("text-secondary");
        close.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        close.setIcon(VaadinIcon.CLOSE_SMALL.create());

        Tab detailsTab = new Tab("Details");
        Tab commentsTab = new Tab("Comments");
        tabs = new Tabs(detailsTab, commentsTab);
        tabs.addClassNames("editor-view-tabs", "lg:hidden");
        tabs.addSelectedChangeListener(e -> {
            if (e.getSelectedTab().equals(detailsTab)) {
                details.addClassName("flex");
                comments.removeClassName("flex");
            } else {
                details.removeClassName("flex");
                comments.addClassName("flex");
            }
        });

        Div div = new Div(heading, avatarGroup, close);
        div.addClassNames("flex", "items-center", "p-l");

        Header header = new Header(div, tabs);
        header.addClassNames("flex", "flex-col");
        return header;
    }

    private Section createDetails() {
        /* Personal info */
        H3 personalHeader = new H3("Personal information");
        personalHeader.addClassNames("text-l", "lg:text-xl");

        TextField firstName = new TextField("First name");
        TextField lastName = new TextField("Last name");

        TextField email = new TextField("Email");
        email.setValueChangeMode(ValueChangeMode.EAGER);

        TextField phoneNumber = new TextField("Phone number");

        /* Employment info */
        H3 employmentHeader = new H3("Employment information");
        employmentHeader.addClassNames("text-l", "lg:text-xl");

        TextField title = new TextField("Title");

        ComboBox<String> department = new ComboBox<>("Department");
        department.setItems("Engineering", "Product", "Marketing",
                "Customer Success", "Operations", "Sales");

        ComboBox<String> team = new ComboBox<>("Team");
        team.setItems("Core products", "Added value", "Support", "Design");

        RadioButtonGroup<String> happiness = new RadioButtonGroup<>();
        happiness.setLabel("How excited are they?");
        happiness.setItems(HAPPINESS_VALUES);
        happiness.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);

        // Configure Form
        binder = new CollaborationBinder<>(Person.class, localUser);
        binder.forField(firstName).bind("firstName");
        binder.forField(lastName).bind("lastName");
        binder.forField(email).bind("email");
        binder.forField(phoneNumber).bind("phoneNumber");
        binder.forField(title).bind("title");
        binder.forField(department).bind("department");
        binder.forField(team).bind("team");
        binder.forField(happiness).bind("happiness");

        // Bind fields. This where you'd define e.g. validation rules
        binder.bindInstanceFields(this);

        Div content = new Div();
        content.add(personalHeader, firstName, lastName, email, phoneNumber,
                employmentHeader, title, department, team, happiness);
        content.addClassNames("flex", "flex-col", "flex-auto", "pb-m", "px-m",
                "overflow-auto");

        /* Footer */
        delete.addThemeVariants(ButtonVariant.LUMO_TERTIARY,
                ButtonVariant.LUMO_ERROR);
        delete.addClassName("mr-auto");

        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        cancel.addClassName("mx-m");

        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Footer footer = new Footer(delete, cancel, save);
        footer.addClassNames("bg-contrast-5", "flex", "px-m", "py-s");
        footer.add(delete, cancel, save);

        Section details = new Section(content, footer);
        details.addClassNames("editor-view-details", "lg:flex", "flex-col",
                "flex-grow", "hidden", "flex");
        details.setWidth("50%");
        return details;
    }

    private Section createComments() {
        list = new CollaborationMessageList(localUser, null);
        list.addClassNames("flex-grow");
        list.setSizeUndefined();

        input = new CollaborationMessageInput(list);
        input.addClassNames("bg-contrast-5");
        input.setSizeUndefined();

        Section comments = new Section(list, input);
        comments.addClassNames("editor-view-comments", "lg:flex", "flex-col",
                "flex-grow", "hidden");
        comments.setWidth("50%");
        return comments;
    }

    protected void editPerson(Person person, String topicId) {
        this.person = person;
        //  A null topicId clears the form
        binder.setTopic(topicId, () -> person);
        avatarGroup.setTopic(topicId);
        list.setTopic(topicId);
        if (expirationRegistration != null) {
            expirationRegistration.remove();
        }
        if (topicId != null) {
            expirationRegistration = CollaborationEngine.getInstance()
                    .openTopicConnection(this, topicId, localUser,
                            topicConnection -> {
                                topicConnection
                                        .getNamedList(
                                                CollaborationMessageList.class
                                                        .getName())
                                        .setExpirationTimeout(
                                                Duration.ofMinutes(15));
                                return null;
                            });
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
