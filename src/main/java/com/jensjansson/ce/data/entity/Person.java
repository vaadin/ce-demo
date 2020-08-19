package com.jensjansson.ce.data.entity;

import javax.persistence.Entity;

import com.jensjansson.ce.data.AbstractEntity;

@Entity
public class Person extends AbstractEntity {

private String firstName;
public String getFirstName() {
  return firstName;
}
public void setFirstName(String firstName) {
  this.firstName = firstName;
}
private String lastName;
public String getLastName() {
  return lastName;
}
public void setLastName(String lastName) {
  this.lastName = lastName;
}
private String email;
public String getEmail() {
  return email;
}
public void setEmail(String email) {
  this.email = email;
}
private String happiness;
public String getHappiness() {
  return happiness;
}
public void setHappiness(String happiness) {
  this.happiness = happiness;
}
}
