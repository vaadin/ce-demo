package com.jensjansson.ce.views.about;

import com.jensjansson.ce.views.main.MainView;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "about", layout = MainView.class)
@PageTitle("About")
@CssImport("styles/views/about/about-view.css")
public class AboutView extends FlexLayout {

    public AboutView() {
        addClassName("about-view");

        //@formatter:off
        String content = "<div>" +
              "<h3>Demo Features</h3>" +
              "<p>This application demonstrates the collaborative experiences that you can create with the Vaadin Collaboration Engine. " +
                "Clicking a table row in the persons page opens a simple form for editing the information of that person. " +
                "To enable real-time collaboration with other users, the form has these additional features:" +
                "<ul>" +
                  "<li>The avatars right above the form indicate all the other users who are looking at the same form at the same time. Your own avatar, which you have picked in the profile page, will be displayed to the other users here. Try hovering your mouse over the avatars to reveal the user names.</li>" +
                  "<li>The fields indicate when other users are editing them by showing a highlight around the component.</li>" +
                  "<li>When any users enters a new value to a field, the value is updated in the views of the other users who are editing the same person.</li>" +
                "</ul>" +
              "</p>" +
              "<h3>How to Test the Collaboration</h3>" +
              "<p>" +
                "To make it easy to see the collaborative features in action even if no one else is currently editing the same person as you are, " +
                "we have implemented artificial bot users. If you start editing a person that no one else is currently editing, a bot user should join shortly. " +
                "You can recognize the bots as the users without an avatar image, whose names start with \"Bot\"." +
              "</p>" +
              "<p>" +
                "You can also test the collaboration by opening the app in two browser windows side by side. " +
                "Open the same person editor in both windows, make changes to the fields in one window and notice how the other one is updated." +
              "</p>" +
              "<h3>Where to Go Next</h3>" +
              "<p>" +
                "To learn more about the Collaboration Engine, visit <a href=\"https://vaadin.com/collaboration\" target=\"_blank\">the Vaadin web site</a>." +
              "</p>" +
            "</div>";
        //@formatter:on

        add(new Html(content));
    }

}
