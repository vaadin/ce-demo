package com.jensjansson.ce.views.about;

import com.jensjansson.ce.views.main.MainView;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "about", layout = MainView.class)
@PageTitle("About")
public class AboutView extends FlexLayout {

    public AboutView() {
        addClassNames("m-xl", "flex", "flex-col", "items-center");

        //@formatter:off
        String content = "<div class=\"max-w-40em\">" +
              "<h3>Demo features</h3>" +
              "<p>This application demonstrates the collaborative experiences that you can create with Vaadin Collaboration Engine. " +
                "Clicking a table row in the persons view opens a form for editing the information of that person. " +
                "To enable real-time collaboration with other users, the form has these additional features:" +
                "<ul>" +
                  "<li>Avatars of other users will be displayed above the form if there are other users editing the same person. Your own avatar, which you have picked in the profile page, will be displayed to the other users. Try hovering your mouse over the avatars to reveal the users' names.</li>" +
                  "<li>The fields indicate when other users are editing them by showing a highlight around the component. You can see who is editing by hovering over the indicator. The color of the highlight indicator matches the color of the avatar.</li>" +
                  "<li>When any user enters a new value to a field, the value is displayed to other users in the same form.</li>" +
                "</ul>" +
              "</p>" +
              "<h3>How to test the collaboration</h3>" +
              "<p>" +
                "To make it easy to see the collaborative features in action even if no one else is currently editing the same person as you are, " +
                "we have implemented artificial bot users. If you start editing a person that no one else is currently editing, a bot user should join shortly. " +
                "You can recognize the bots as the users without an avatar image, whose names start with \"Bot\"." +
              "</p>" +
              "<p>" +
                "You can also test the collaboration by opening the app in two browser windows side by side. " +
                "Open the same person editor in both windows, make changes to the fields in one window and notice how the other one is updated." +
              "</p>" +
              "<h3>Learn more</h3>" +
              "<p>" +
                "To learn more about the Collaboration Engine, visit <a href=\"https://vaadin.com/collaboration\" target=\"_blank\">the Vaadin web site</a>." +
              "</p>" +
              "<h3>Feedback</h3>" +
              "<p>" +
                "Do you have any feedback about Collaboration Engine or another topic? We'd love to hear from you! <a href=\"https://vaad.in/feedback\" target=\"_blank\">Leave us a message</a>." +
              "</p>" +
            "</div>";
        //@formatter:on

        add(new Html(content));
    }

}
