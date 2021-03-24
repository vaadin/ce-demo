package com.jensjansson.ce.data.service;

import java.util.List;

import com.jensjansson.ce.data.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonRepository extends JpaRepository<Person, Integer> {

    List<Person> findByFirstNameLikeIgnoreCase(String var1);
}
