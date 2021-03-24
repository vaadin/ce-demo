package com.jensjansson.ce.views.persons;

import java.util.UUID;

import com.jensjansson.ce.data.entity.Person;
import com.jensjansson.ce.data.service.PersonService;
import com.jensjansson.ce.views.main.MainView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import com.vaadin.collaborationengine.CollaborationEngine;
import com.vaadin.collaborationengine.CollaborationMap;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.avatar.Avatar;
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
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.data.VaadinSpringDataHelpers;

@Route(value = "employees", layout = MainView.class)
@PageTitle("Employees")
public class EmployeesView extends Div {

    private final Grid<Person> grid;

    private CollaborationMap refreshGridMap;

    private final EditorView editorView;

    private final Dialog dialog;

    public EmployeesView(@Autowired PersonService personService,
            MainView mainView) {
        setSizeFull();

        editorView = new EditorView(mainView.getLocalUser(), personService,
                new EditorView.SaveNotifier() {
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
                });
        dialog = new Dialog(editorView);
        dialog.setWidth("80%");
        dialog.setHeight("80%");
        dialog.addDialogCloseActionListener(event -> {
            editorView.editPerson(null);
            dialog.close();
        });
        dialog.setCloseOnEsc(true);
        dialog.setCloseOnOutsideClick(true);
        dialog.addThemeVariants(DialogVariant.LUMO_NO_PADDING);

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
        return new Avatar(person.getFirstName() + " " + person.getLastName(),
                person.getAvatar());
    }

    private Component createOwnerInfo(Person person) {
        Span name = new Span(
                new Text(person.getFirstName() + " " + person.getLastName()));
        name.addClassNames("font-semibold");
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

    private Component createEditButton(Person person) {
        Button edit = new Button(null, VaadinIcon.EDIT.create(), click -> {
            editPerson(person);
        });
        edit.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        return edit;
    }

    private void editPerson(Person person) {
        editorView.editPerson(person);
        if (person != null) {
            dialog.open();
        }
    }
}
