package com.jensjansson.ce.views.persons;

import com.jensjansson.ce.bot.BotRunner;
import com.jensjansson.ce.data.entity.Person;
import com.jensjansson.ce.data.service.PersonService;
import com.vaadin.collaborationengine.CollaborationAvatarGroup;
import com.vaadin.collaborationengine.CollaborationBinder;
import com.vaadin.collaborationengine.UserInfo;
import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.jensjansson.ce.views.main.MainView;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.artur.helpers.CrudServiceDataProvider;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Route(value = "persons", layout = MainView.class)
@PageTitle("Persons")
@CssImport("styles/views/persons/persons-view.css")
public class PersonsView extends Div {

    public static final List<String> HAPPINESS_VALUES = Arrays.asList(
            "Raptorous", "Ecstatic", "Joyful", "Indifferent", "Dreadful");

    private Grid<Person> grid;

    private TextField firstName = new TextField();
    private TextField lastName = new TextField();
    private RadioButtonGroup<String> happiness = new RadioButtonGroup<>();
    private TextField email = new TextField();
    private PasswordField password = new PasswordField();

    private Button cancel = new Button("Cancel");
    private Button save = new Button("Save");

    private CollaborationAvatarGroup avatarGroup;
    private CollaborationBinder<Person> binder;
    private Person person;

    public PersonsView(@Autowired PersonService personService, MainView mainView) {
        setId("persons-view");
        // Configure Grid
        UserInfo localUser = new UserInfo(UUID.randomUUID().toString());
        localUser.setName(mainView.getUserName());
        localUser.setImage(mainView.getUserAvatar());

        avatarGroup = new CollaborationAvatarGroup(
                localUser);
        avatarGroup.getContent().setMax(4);

        grid = new Grid<>(Person.class);
        grid.setColumns("firstName", "lastName", "email");
        grid.setDataProvider(new CrudServiceDataProvider<Person, Void>(personService));

        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);
        grid.setHeightFull();

        // when a row is selected or deselected, populate form
        grid.asSingleSelect().addValueChangeListener(event -> {
                    populateForm(event.getValue());
                }

        );

        // Configure Form
        binder = new CollaborationBinder<>(Person.class, localUser);

        // Bind fields. This where you'd define e.g. validation rules
        binder.bindInstanceFields(this);

        // note that password field isn't bound since that property doesn't exist in
        // Person

        // the grid valueChangeEvent will clear the form too
        cancel.addClickListener(e -> grid.asSingleSelect().clear());

        save.addClickListener(e -> {
            Person person = new Person();
            try {
                binder.writeBean(person);
                if (this.person != null) {
                    person.setId(this.person.getId());
                }
                personService.update(person);
                grid.select(null);
                grid.getDataProvider().refreshAll();
                Notification.show("Person details stored.");
            } catch (ValidationException validationException) {
                validationException.printStackTrace();
                Notification.show("An exception happened while trying to store the person details.");
            }
        });

        SplitLayout splitLayout = new SplitLayout();
        splitLayout.setSizeFull();

        createGridLayout(splitLayout);
        createEditorLayout(splitLayout);

        add(splitLayout);
    }

    private void createEditorLayout(SplitLayout splitLayout) {
        Div editorLayoutDiv = new Div();
        editorLayoutDiv.setId("editor-layout");

        Div editorDiv = new Div();
        editorDiv.setId("editor");
        editorDiv.add(avatarGroup);
        editorLayoutDiv.add(editorDiv);

        happiness.setItems(HAPPINESS_VALUES);
        happiness.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);
        happiness.setValue("Raptorous");
        add(happiness);

        FormLayout formLayout = new FormLayout();
        addFormItem(editorDiv, formLayout, firstName, "First name");
        addFormItem(editorDiv, formLayout, lastName, "Last name");
        addFormItem(editorDiv, formLayout, happiness, "How excited are they?");
        addFormItem(editorDiv, formLayout, email, "Email");
        addFormItem(editorDiv, formLayout, password, "Password");
        createButtonLayout(editorLayoutDiv);

        splitLayout.addToSecondary(editorLayoutDiv);
    }

    private void createButtonLayout(Div editorLayoutDiv) {
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setId("button-layout");
        buttonLayout.setWidthFull();
        buttonLayout.setSpacing(true);
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        buttonLayout.add(save, cancel);
        editorLayoutDiv.add(buttonLayout);
    }

    private void createGridLayout(SplitLayout splitLayout) {
        Div wrapper = new Div();
        wrapper.setId("grid-wrapper");
        wrapper.setWidthFull();
        splitLayout.addToPrimary(wrapper);
        wrapper.add(grid);
    }

    private void addFormItem(Div wrapper, FormLayout formLayout, AbstractField field, String fieldName) {
        formLayout.addFormItem(field, fieldName);
        wrapper.add(formLayout);
        field.getElement().getClassList().add("full-width");
    }

    private void populateForm(Person value) {
        this.person = value;
        // Value can be null as well, that clears the form
        String topicId = null;
        if (value != null && value.getId() != null) {
            topicId = "person/" + String.valueOf(value.getId());
        }
        binder.setTopic(topicId, () -> value);
        avatarGroup.setTopic(topicId);
        // The password field isn't bound through the binder, so handle that
        password.setValue("");

        if (topicId != null) {
            BotRunner.onUserJoined(topicId);
        }
    }
}
