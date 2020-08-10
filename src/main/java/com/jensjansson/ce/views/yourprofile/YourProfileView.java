package com.jensjansson.ce.views.yourprofile;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.jensjansson.ce.views.main.MainView;

@Route(value = "profile", layout = MainView.class)
@RouteAlias(value = "", layout = MainView.class)
@PageTitle("Your profile")
@CssImport("styles/views/yourprofile/yourprofile-view.css")
public class YourProfileView extends Div {

    public YourProfileView() {
        setId("yourprofile-view");
        add(new Label("Content placeholder"));
    }

}
