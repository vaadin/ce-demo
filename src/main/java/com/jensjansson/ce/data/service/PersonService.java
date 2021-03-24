package com.jensjansson.ce.data.service;

import java.util.List;

import com.jensjansson.ce.data.entity.Person;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.vaadin.artur.helpers.CrudService;

@Service
public class PersonService extends CrudService<Person, Integer> {

    private PersonRepository repository;

    public PersonService(@Autowired PersonRepository repository) {
        this.repository = repository;
    }

    @Override
    protected PersonRepository getRepository() {
        return repository;
    }

    public List<Person> findByQuery(String query) {
        return getRepository().findAll();
    }
}
