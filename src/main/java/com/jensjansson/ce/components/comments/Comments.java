package com.jensjansson.ce.components.comments;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.vaadin.collaborationengine.CollaborationEngine;
import com.vaadin.collaborationengine.CollaborationMap;
import com.vaadin.collaborationengine.UserInfo;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.shared.Registration;

public class Comments extends Div {

    CommentsDataStore dataStore = CommentsDataStore.getDataStore();
    Div header;
    Div commentLayout = new Div();
    private UserInfo user;
    HorizontalLayout addCommentLayout;
    TextField commentTextField;
    Button button;
    CollaborationMap collaborationMap;
    private String topicId;
    Registration registration;
    private List<CommentDTO> data = new ArrayList<>();

    public Comments(UserInfo user) {
        this.user = user;
        header = new Div(new Text("Conversation"));
        header.addClassName("comments-header");
        commentLayout.addClassName("comments-feed");
        addCommentLayout = createAddCommentLayout();
        addCommentLayout.addClassName("comments-add");
        add(header, commentLayout, addCommentLayout);
        addClassName("comments");
    }

    private void renderComments(List<CommentDTO> data) {
        commentLayout.removeAll();
        data.forEach(comment -> commentLayout.add(new Comment(comment)));
    }

    public void setTopic(String topicId){
        if(registration!=null){
            registration.remove();
        }
        this.topicId = topicId;
        if(topicId != null){
            registration = CollaborationEngine.getInstance().openTopicConnection(this, topicId, user, topic -> {
                collaborationMap = topic.getNamedMap("comments");
                CommentDTO initialValue = collaborationMap.get("comments", CommentDTO.class);
                collaborationMap.subscribe(mapChangeEvent -> {
                    renderComments(this.data);
                });
                return null;
            });
            this.data = dataStore.getData(topicId);
            renderComments(this.data);
            commentTextField.setEnabled(true);
            button.setEnabled(true);
        } else {
            commentLayout.removeAll();
            data.clear();
            commentTextField.setEnabled(false);
            button.setEnabled(false);
        }

    }

    private HorizontalLayout createAddCommentLayout() {

        commentTextField = new TextField();
        commentTextField.setPlaceholder("Message...");
        commentTextField.addClassName("comments-textfield");
        commentTextField.addKeyDownListener(Key.ENTER, event -> submitMessage());

        button = new Button("Send", event -> submitMessage());
        commentTextField.setEnabled(false);
        button.addClassName("comments-button");
        button.setEnabled(false);
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        return new HorizontalLayout(commentTextField, button);
    }

    private void submitMessage(){
        String message = commentTextField.getValue();
        commentTextField.clear();
        commentTextField.focus();
        CommentDTO comment = new CommentDTO(user, LocalDateTime.now(), message);
        dataStore.addMessage(topicId, comment);
        collaborationMap.put("comments", comment);
    }
}
