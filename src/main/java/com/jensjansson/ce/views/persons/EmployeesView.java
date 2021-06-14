package com.jensjansson.ce.views.persons;

import com.jensjansson.ce.data.entity.Person;
import com.jensjansson.ce.data.service.PersonService;
import com.jensjansson.ce.views.main.MainView;
import com.vaadin.collaborationengine.CollaborationEngine;
import com.vaadin.collaborationengine.CollaborationMap;
import com.vaadin.collaborationengine.PresenceAdapter;
import com.vaadin.collaborationengine.UserInfo;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarGroup;
import com.vaadin.flow.component.avatar.AvatarVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.dialog.DialogVariant;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.data.VaadinSpringDataHelpers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.util.Objects;
import java.util.UUID;

@Route(value = "employees", layout = MainView.class)
@PageTitle("Employees")
public class EmployeesView extends Div {

    private final Grid<Person> grid;

    private CollaborationMap refreshGridMap;

    private final EditorView editorView;

    private final Dialog dialog;

    private final UserInfo localUser;

    public EmployeesView(@Autowired PersonService personService,
            MainView mainView) {
        setSizeFull();
        this.localUser = mainView.getLocalUser();

        editorView = new EditorView(localUser, personService,
                new EditorView.EditorActionNotifier() {
                    @Override
                    public void updateGrid(Person person) {
                        grid.getDataProvider().refreshAll();
                        if (refreshGridMap != null) {
                            refreshGridMap.put("refreshGrid",
                                    UUID.randomUUID().toString());
                        }
                        dialog.close();
                    }

                    @Override
                    public void stopEditingPerson() {
                        dialog.close();
                    }

                    @Override
                    public void deletePerson() {
                        dialog.close();
                        Notification notification = new Notification(
                                "Delete has been disabled for demo purposes.");
                        notification.addThemeVariants(
                                NotificationVariant.LUMO_SUCCESS);
                        notification.setDuration(5000);
                        notification.open();
                    }
                });

        dialog = new Dialog(editorView);
        dialog.addThemeName("editor-view-dialog");
        dialog.addThemeVariants(DialogVariant.LUMO_NO_PADDING);

        dialog.addDialogCloseActionListener(event -> {
            editorView.editPerson(null, null);
            dialog.close();
        });

        dialog.setCloseOnEsc(true);
        dialog.setCloseOnOutsideClick(true);

        grid = new Grid<>();
        grid.removeAllColumns();
        grid.addColumn(new ComponentRenderer<>(this::createAvatar))
                .setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(new ComponentRenderer<>(this::createOwnerInfo))
                .setFlexGrow(2);
        grid.addColumn(new ComponentRenderer<>(this::createTeamInfo))
                .setFlexGrow(1);
        grid.addColumn(new ComponentRenderer<>(this::createContactInfo))
                .setFlexGrow(2);
        grid.addColumn(new ComponentRenderer<>(this::createPresentComponent)).setFlexGrow(1);
        grid.addColumn(new ComponentRenderer<>(this::createEditButton))
                .setWidth("5em").setFlexGrow(0)
                .setTextAlign(ColumnTextAlign.END);

        grid.setItems(
                query -> personService
                        .list(PageRequest
                                .of(query.getPage(), query.getPageSize(),
                                        VaadinSpringDataHelpers
                                                .toSpringDataSort(query)))
                        .stream());

        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);
        grid.setHeightFull();
        grid.setSelectionMode(Grid.SelectionMode.NONE);

        add(grid);

        CollaborationEngine.getInstance().openTopicConnection(this,
                "refreshGrid", mainView.getLocalUser(), topicConnection -> {
                    refreshGridMap = topicConnection.getNamedMap("refreshGrid");
                    refreshGridMap.subscribe(
                            e -> grid.getDataProvider().refreshAll());
                    return () -> refreshGridMap = null;
                });
    }

    private Component createAvatar(Person person) {
        Avatar avatar = new Avatar(
                person.getFirstName() + " " + person.getLastName(),
                person.getAvatar());
        avatar.addClassName("mt-xs");
        avatar.addThemeVariants(AvatarVariant.LUMO_LARGE);
        return avatar;
    }

    private Component createOwnerInfo(Person person) {
        Span name = new Span(
                new Text(person.getFirstName() + " " + person.getLastName()));
        name.addClassNames("font-semibold text-l");
        Span title = new Span(
                new Text(person.getTitle() != null ? person.getTitle() : ""));
        title.addClassNames("text-s", "text-secondary");
        Div layout = new Div(name, title);
        layout.addClassNames("leading-m", "py-s", "flex", "flex-col");
        return layout;
    }

    private Component createTeamInfo(Person person) {
        Span department = new Span(new Text(
                person.getDepartment() != null ? person.getDepartment() : ""));
        Span team = new Span(
                new Text(person.getTeam() != null ? person.getTeam() : ""));
        team.addClassNames("text-s");
        Div layout = new Div(department, team);
        layout.addClassNames("leading-m", "py-s", "flex", "flex-col",
                "text-secondary");
        return layout;
    }

    private Component createContactInfo(Person person) {
        Span email = new Span(
                new Text(person.getEmail() != null ? person.getEmail() : ""));

        Span phone = new Span(new Text(
                person.getPhoneNumber() != null ? person.getPhoneNumber()
                        : ""));
        phone.addClassNames("text-s");
        Div layout = new Div(email, phone);
        layout.addClassNames("leading-m", "py-s", "flex", "flex-col",
                "text-secondary");
        return layout;
    }

    private Component createPresentComponent(Person person) {
        return new PresenceComponent(person);
    }

    class PresenceComponent extends AvatarGroup {
        public PresenceComponent(Person person) {
            Objects.requireNonNull(person);
            PresenceAdapter presenceAdapter = new PresenceAdapter(this,
                localUser, getTopicId(person));
            presenceAdapter.setAutoPresence(false);

            presenceAdapter.setNewUserHandler(e -> {
                String description = String.format("%s is editing this row", e.getName());
                AvatarGroupItem item = new AvatarGroupItem(description,
                    e.getImage());
                item.setColorIndex(
                    CollaborationEngine.getInstance().getUserColorIndex(e));
                add(item);
                return () -> remove(item);
            });
        }
    }

    private Component createEditButton(Person person) {
        Button edit = new Button(null, VaadinIcon.EDIT.create(), click -> {
            editPerson(person);
        });
        edit.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        return edit;
    }

    private String getTopicId(Person person) {
        String topicId = null;
        if (person != null && person.getId() != null) {
            topicId = "person/" + String.valueOf(person.getId());
        }
        return topicId;
    }

    private void editPerson(Person person) {
        editorView.editPerson(person, getTopicId(person));
        if (person != null) {
            dialog.open();
        }
    }
}
