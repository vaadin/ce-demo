package com.jensjansson.ce.views.yourprofile;

import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.*;
import com.jensjansson.ce.views.main.MainView;

import java.util.Arrays;
import java.util.List;

@Route(value = "profile", layout = MainView.class)
@RouteAlias(value = "", layout = MainView.class)
@PageTitle("Your profile")
@CssImport("styles/views/yourprofile/yourprofile-view.css")
public class YourProfileView extends FlexLayout implements BeforeEnterObserver {

    MainView mainView;
    TextField name;
    AvatarField avatars;

    public YourProfileView(MainView mainView) {
        this.mainView = mainView;

        setId("your-profile-view");
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        name = new TextField("Your name", e -> {
            if(e.isFromClient()) {
                mainView.setUserName(e.getValue());
            }
        });
        name.setValueChangeMode(ValueChangeMode.LAZY);
        name.focus();

        avatars = new AvatarField();
        avatars.setLabel("Choose avatar");
        Anchor link = new Anchor("https://icon-icons.com/pack/xmas-giveaway-:)/1736", "Icons by Laura Reen");
        link.getStyle().set("color", "gray");
        link.getElement().setAttribute("theme", "font-size-xs");
        VerticalLayout center = new VerticalLayout(name, avatars, link);
        center.setHorizontalComponentAlignment(Alignment.END, link);
        center.setWidth("100%");
        center.setMaxWidth("400px");

        add(center);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        if (mainView != null) {
            name.setValue(mainView.getUserName());
            avatars.select(mainView.getUserAvatar());
        }
    }

    private void setAvatar(String identifier) {
        mainView.setUserAvatar(identifier);
        avatars.select(identifier);
    }

    public class AvatarField extends CustomField<VerticalLayout> {
        List<AvatarBox> avatars = Arrays.asList(
                new AvatarBox("1"),
                new AvatarBox("2"),
                new AvatarBox("3"),
                new AvatarBox("4"),
                new AvatarBox("5"),
                new AvatarBox("6"),
                new AvatarBox("7"),
                new AvatarBox("8")
        );

        public AvatarField() {
            FlexLayout layout = new FlexLayout(avatars.toArray(new AvatarBox[0]));
            layout.setFlexWrap(FlexWrap.WRAP);
            add(layout);
        }

        @Override
        protected VerticalLayout generateModelValue() {
            return null;
        }

        @Override
        protected void setPresentationValue(VerticalLayout verticalLayout) {
        }

        public void select(String identifier) {
            for (AvatarBox avatar : avatars) {
                avatar.setSelected(identifier.equals(avatar.getIdentifier()));
            }
        }
    }

    public class AvatarBox extends Image {
        private String identifier;

        public AvatarBox(String identifier) {
            super("images/avatars/" + identifier + ".png", "Avatar");
            this.identifier = "images/avatars/" + identifier + ".png";
            addClassName("avatar-box");
            setWidth("64px");
            setHeight("64px");
            addClickListener(e -> {
                YourProfileView.this.setAvatar(this.identifier);
            });
        }

        public String getIdentifier() {
            return identifier;
        }

        public void setSelected(boolean selected) {
            if (selected) {
                addClassName("selected");
            } else {
                removeClassName("selected");
            }
        }
    }

}