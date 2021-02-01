package com.jensjansson.ce.components.comments;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.github.javafaker.Faker;
import com.github.javafaker.Name;

import com.vaadin.collaborationengine.UserInfo;

public class CommentsDataStore {

    Map<String, List<CommentDTO>> topics = new HashMap<>();

    static CommentsDataStore ds = new CommentsDataStore();

    public static CommentsDataStore getDataStore(){
        return ds;
    }
    public List<CommentDTO> getData(String topicId) {
        if(topics.containsKey(topicId)){
            return topics.get(topicId);
        } else {
            List<CommentDTO> comments = generateFakeData();
            topics.put(topicId, comments);
            return comments;
        }

    }

    public List<CommentDTO> generateFakeData(){
        Random random = new Random();
        Faker faker = new Faker();
        int number = random.nextInt(2)+3;
        List<CommentDTO> comments = new ArrayList<>();
        for(int i = 0; i<number; i++){
            Name name = faker.name();
            String avatar = "https://i.pravatar.cc/200?img=" + i+5;
            UserInfo userInfo = new UserInfo(name.username(), name.firstName() + " " + name.lastName(), avatar);
            comments.add(new CommentDTO(userInfo, faker.date().past(5, TimeUnit.DAYS).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime(), faker.dune().quote()));
        }
        comments.sort((one, two) -> {
            return two.date.toEpochSecond(ZoneOffset.UTC) > one.date.toEpochSecond(ZoneOffset.UTC) ? -1 : (two.date.toEpochSecond(ZoneOffset.UTC) < one.date.toEpochSecond(ZoneOffset.UTC)) ? 1 : 0;
        });
        return comments;
    }

    public void addMessage(String topicId, CommentDTO comment) {
        topics.get(topicId).add(comment);
    }
}
