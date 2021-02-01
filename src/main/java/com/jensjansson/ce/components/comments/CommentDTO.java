package com.jensjansson.ce.components.comments;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

import com.vaadin.collaborationengine.UserInfo;

public class CommentDTO {

    UserInfo user;
    LocalDateTime date;
    String message;


    public CommentDTO() {
    }

    public CommentDTO(UserInfo user, LocalDateTime date, String message) {
        this.user = user;
        this.date = date;
        this.message = message;
    }

    public UserInfo getUser() {
        return user;
    }

    public void setUser(UserInfo user) {
        this.user = user;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommentDTO that = (CommentDTO) o;
        return Objects.equals(user, that.user) &&
                Objects.equals(date, that.date) &&
                Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, date, message);
    }
}
