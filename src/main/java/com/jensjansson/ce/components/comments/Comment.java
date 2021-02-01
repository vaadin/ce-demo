package com.jensjansson.ce.components.comments;

import org.ocpsoft.prettytime.PrettyTime;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.html.Div;

public class Comment extends Div {
    public Comment(CommentDTO comment) {
        addClassName("comments-comment");
        PrettyTime p = new PrettyTime();
        Avatar avatar = new Avatar(comment.getUser().getName(), comment.getUser().getImage());
        avatar.addClassName("comments-comment-avatar");
        Div name = new Div(new Text(comment.getUser().getName()));
        name.addClassName("comments-comment-name");
        Div date = new Div(new Text(p.format(comment.getDate())));
        date.addClassName("comments-comment-date");
        Div nameAndDate = new Div(name, date);
        nameAndDate.addClassName("comments-comment-namedate");
        Div message = new Div(new Text(comment.getMessage()));
        message.addClassName("comments-comment-message");
        Div content = new Div(nameAndDate, message);
        content.addClassName("comments-comment-content");
        add(avatar, content);
    }
}
