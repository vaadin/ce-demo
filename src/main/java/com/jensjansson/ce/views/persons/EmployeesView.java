package com.jensjansson.ce.views.persons;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.collaborationengine.CollaborationEngine;
import com.vaadin.collaborationengine.CollaborationMap;
import com.vaadin.collaborationengine.PresenceManager;
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
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.data.VaadinSpringDataHelpers;

import com.jensjansson.ce.data.entity.Person;
import com.jensjansson.ce.data.service.PersonService;
import com.jensjansson.ce.views.main.MainView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

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
                        grid.getDataProvider().refreshItem(person);
                        if (refreshGridMap != null) {
                            refreshGridMap.put(String.valueOf(person.getId()),person);
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
        grid.addColumn(createAvatarRenderer()).setFlexGrow(0);
        grid.addColumn(createOwnerInfoRenderer()).setFlexGrow(2);
        grid.addColumn(createTeamInfoRenderer()).setFlexGrow(1);
        grid.addColumn(createContactInfoRenderer())
            .setFlexGrow(2);
        grid.addColumn(new ComponentRenderer<>(this::createPresenceComponent))
            .setFlexGrow(1);
        grid.addColumn(createEditButtonRenderer())
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
                refreshGridMap.subscribe(e -> grid.getDataProvider()
                    .refreshItem(e.getValue(Person.class)));
                return () -> refreshGridMap = null;
                });

    }

    private LitRenderer<Person> createAvatarRenderer() {
        final String value = "<vaadin-avatar"
            + " class=\"mt-xs\""
            + " theme=\"large\""
            + " img=${item.img}"
            + " name=${item.name}"
            + "></vaadin-avatar>";
        return LitRenderer.<Person>of(value)
            .withProperty("img", Person::getAvatar)
            .withProperty("name", Person::getFullName);
    }

    private ValueProvider<Person, String> getEmptyIfNull(ValueProvider<Person, String> provider) {
        return person -> {
            String value = provider.apply(person);
            return value != null ? value : "";
        };
    }

    private LitRenderer<Person> createOwnerInfoRenderer() {
        String template = "<div class=\"leading-m py-s flex flex-col\">"
            + "<span class=\"font-semibold text-l\">${item.name}</span>"
            + "<span class=\"text-s text-secondary\">${item.title}</span></div>";
        return LitRenderer.<Person>of(template)
            .withProperty("name", Person::getFullName)
            .withProperty("title", getEmptyIfNull(Person::getTitle));
    }

    private LitRenderer<Person> createTeamInfoRenderer() {
        String template = "<div class=\"leading-m py-s flex flex-col text-secondary\">"
            + "<span>${item.department}</span>"
            + "<span class=\"text-s\">${item.team}</span>"
            + "</div>";
        return LitRenderer.<Person>of(template)
            .withProperty("department", getEmptyIfNull(Person::getDepartment))
            .withProperty("team", getEmptyIfNull(Person::getTeam));
    }

   private PresenceComponent createPresenceComponent(Person person) {
        String topicId = getTopicId(person);
        return new PresenceComponent(localUser, topicId);
    }

    private LitRenderer<Person> createContactInfoRenderer() {
        String template = "<div class=\"leading-m py-s flex flex-col text-secondary\">"
            + "<span>${item.email}</span>"
            + "<span class=\"text-s\">${item.phoneNumber}</span>"
            + "</div>";
        return LitRenderer.<Person>of(template)
            .withProperty("email", getEmptyIfNull(Person::getEmail))
            .withProperty("phoneNumber", getEmptyIfNull(Person::getPhoneNumber));
    }

    private LitRenderer<Person> createEditButtonRenderer() {
        String template =
            "<vaadin-button theme=\"icon tertiary\" @click=${handleClick} >"
                + "<vaadin-icon icon=\"vaadin:edit\" slot=\"prefix\"></vaadin-icon>"
                + "</vaadin-button>";
        return LitRenderer.<Person>of(template)
            .withFunction("handleClick", this::editPerson);
    }

    public static String getTopicId(Person person) {
        String topicId = null;
        if (person != null && person.getId() != null) {
            topicId = "person/" + person.getId();
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

/**
 * Component that shows avatars of the users marked as present in the topic
 * without marking the current user as present.
 * Used in the grid to display bots and users editing a given entity.
 */
class PresenceComponent extends AvatarGroup {

    private static final Logger logger = LoggerFactory
        .getLogger(PresenceComponent.class);

    public PresenceComponent(UserInfo localUser, String topicId) {
        Objects.requireNonNull(localUser);
        Objects.requireNonNull(topicId);
        PresenceManager presenceManager = new PresenceManager(this,
            localUser, topicId);
        presenceManager.markAsPresent(false);
        
        

		presenceManager.setPresenceHandler(e -> {
			String description = String.format("%s is editing this row", e.getUser().getName());
			AvatarGroupItem item = new AvatarGroupItem(description, e.getUser().getImage());
			item.setColorIndex(e.getUser().getColorIndex());
			if (Objects.equals(localUser.getId(), e.getUser().getId())) {
				// Set the local user as the first item.
				setItems(Stream.concat(Stream.of(item), getItems().stream()).collect(Collectors.toList()));
			} else {
				add(item);
			}
			return () -> remove(item);
		});
        addAttachListener( e -> logger.debug("Attached to " + topicId));
        addDetachListener( e -> logger.debug("Detached from" + topicId));
    }
}
