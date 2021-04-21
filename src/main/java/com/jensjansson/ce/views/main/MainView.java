package com.jensjansson.ce.views.main;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import com.jensjansson.ce.views.about.AboutView;
import com.jensjansson.ce.views.persons.EmployeesView;

import com.vaadin.collaborationengine.UserInfo;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarVariant;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Nav;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.spring.annotation.UIScope;

@org.springframework.stereotype.Component
@UIScope
public class MainView extends AppLayout {

    private Nav menu;
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
        drawerToggle.addClassName("text-secondary");
        drawerToggle.setThemeName("contrast");

        viewTitle = new H1();
        viewTitle.addClassNames("m-0", "text-l");

        Header layout = new Header();
        layout.addClassNames("bg-base", "border-b", "border-contrast-10",
                "box-border", "flex", "h-xl", "items-center", "w-full");
        layout.add(drawerToggle, viewTitle);
        return layout;
    }

    private Component createSideMenu() {
        H2 header = new H2("Collaboration Engine Demo");
        header.addClassNames("flex", "items-center", "h-xl", "m-0", "px-m",
                "text-m");

        menu = createMenuLinks();
        Footer footer = createMenuFooter();

        com.vaadin.flow.component.html.Section section = new com.vaadin.flow.component.html.Section(
                header, menu, footer);
        section.addClassNames("bg-base", "flex", "flex-col", "items-stretch",
                "min-h-full");
        return section;
    }

    private Nav createMenuLinks() {
        Nav menu = new Nav();
        menu.addClassNames("mb-l");

        menu.add(createLink(VaadinIcon.EDIT, "Employees", EmployeesView.class),
                createLink(VaadinIcon.PICTURE, "About", AboutView.class));

        return menu;
    }

    private RouterLink createLink(VaadinIcon vaadinIcon, String text,
            Class<? extends Component> view) {
        RouterLink link = new RouterLink(null, view);

        Icon icon = vaadinIcon.create();
        icon.addClassNames("box-border", "icon-s", "me-s");

        Span span = new Span(text);
        span.addClassNames("text-s", "font-medium");

        link.add(icon, span);
        link.addClassNames("flex", "h-m", "items-center", "mx-s", "px-s",
                "relative", "text-secondary");
        return link;
    }

    private Footer createMenuFooter() {
        avatar = new Avatar(localUser.getName(), localUser.getImage());
        avatar.addClassNames("mr-xs");
        avatar.addThemeVariants(AvatarVariant.LUMO_XSMALL);

        userLabel = new Span(localUser.getName());
        userLabel.addClassNames("font-medium", "text-s", "text-secondary");

        Footer footer = new Footer(avatar, userLabel);
        footer.addClassNames("flex", "items-center", "mb-m", "mt-auto", "px-m");
        return footer;
    }

    @Override
    protected void afterNavigation() {
        super.afterNavigation();
        String title = getContent().getClass().getAnnotation(PageTitle.class)
                .value();
        viewTitle.setText(title);
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
