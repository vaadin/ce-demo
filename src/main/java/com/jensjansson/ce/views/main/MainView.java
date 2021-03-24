package com.jensjansson.ce.views.main;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import com.jensjansson.ce.views.about.AboutView;
import com.jensjansson.ce.views.persons.EmployeesView;
import com.jensjansson.ce.views.yourprofile.YourProfileView;

import com.vaadin.collaborationengine.UserInfo;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.spring.annotation.UIScope;

@org.springframework.stereotype.Component
@UIScope
public class MainView extends AppLayout {

    private Div menu;
    private H1 viewTitle;
    private Avatar avatar;
    private UserInfo localUser;
    private Span userLabel;

    public MainView() {
        localUser = new UserInfo(UUID.randomUUID().toString());
        localUser.setName("Anonymous User");
        localUser.setImage("images/avatars/"
                + ThreadLocalRandom.current().nextInt(1, 8 + 1) + ".png");

        setPrimarySection(Section.DRAWER);
        addToNavbar(true, createHeaderContent());
        addToDrawer(createSideMenu());
    }

    private Component createHeaderContent() {
        DrawerToggle drawerToggle = new DrawerToggle();
        drawerToggle.setThemeName("contrast");
        viewTitle = new H1();
        viewTitle.addClassNames("m-0", "text-l");
        Div layout = new Div();
        layout.addClassNames("bg-base", "border-b", "border-contrast-10",
                "box-border", "flex", "flex-row", "h-xl", "items-center",
                "w-full");
        layout.add(drawerToggle, viewTitle);
        return layout;
    }

    private Component createSideMenu() {
        H2 header = new H2("Collaboration Engine Demo");
        header.addClassNames("flex", "items-center", "h-xl", "m-0", "px-m",
                "text-m");
        menu = createMenuLinks();
        HorizontalLayout avatar = createMenuAvatar();

        Div layout = new Div(header, menu, avatar);
        layout.addClassNames("bg-contrast-5", "flex", "flex-col",
                "items-strech", "min-h-full");
        return layout;
    }

    private Div createMenuLinks() {
        Div menu = new Div();

        menu.add(createLink(VaadinIcon.EDIT, "Employees", EmployeesView.class),
                createLink(VaadinIcon.PICTURE, "About", AboutView.class));
        menu.addClassNames("p-s", "box-border", "flex", "flex-col",
                "flex-grow");
        menu.setWidthFull();
        return menu;
    }

    private RouterLink createLink(VaadinIcon vaadinIcon, String text,
            Class<? extends Component> view) {
        RouterLink link = new RouterLink(null, view);
        Icon icon = vaadinIcon.create();
        icon.addClassNames("box-border", "icon-m", "me-s", "p-xs");
        Span span = new Span(text);
        span.addClassNames("text-s", "font-medium");
        link.add(icon, span);
        link.addClassNames("rounded-s", "h-m", "px-s", "mx-s", "flex",
                "items-center", "box-border");
        return link;
    }

    private HorizontalLayout createMenuAvatar() {
        avatar = new Avatar(localUser.getName(), localUser.getImage());
        avatar.addClassNames("pointer");
        userLabel = new Span(localUser.getName());
        HorizontalLayout layout = new HorizontalLayout(avatar, userLabel);
        layout.addClassNames("p-m", "pointer");
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.addClickListener(event -> {
            layout.getUI().ifPresent(ui -> ui.navigate(YourProfileView.class));
        });
        return layout;
    }

    @Override
    protected void afterNavigation() {
        super.afterNavigation();
        updateSelectedView();
        String title = getContent().getClass().getAnnotation(PageTitle.class)
                .value();
        viewTitle.setText(title);
    }

    private void updateSelectedView() {
        String currentRoute = RouteConfiguration.forSessionScope()
                .getUrl(getContent().getClass());
        menu.getChildren().forEach(component -> {
            RouterLink link = ((RouterLink) component);
            if (link.getHref().equals(currentRoute)) {
                link.addClassName("bg-primary-10");
            } else {
                link.removeClassName("bg-primary-10");
            }
        });
    }

    public UserInfo getLocalUser() {
        return localUser;
    }

    public void setUserName(String userName) {
        localUser.setName(userName);
        avatar.setName(userName);
        userLabel.setText(userName);
    }

    public void setUserAvatar(String avatarUrl) {
        localUser.setImage(avatarUrl);
        avatar.setImage(avatarUrl);
    }
}
