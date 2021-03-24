package com.jensjansson.ce.data.generator;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import com.jensjansson.ce.data.entity.Person;
import com.jensjansson.ce.data.service.PersonRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.vaadin.artur.exampledata.DataType;
import org.vaadin.artur.exampledata.ExampleDataGenerator;

import com.vaadin.flow.spring.annotation.SpringComponent;

@SpringComponent
public class DataGenerator {

    @Bean
    public CommandLineRunner loadData(PersonRepository personRepository) {
        return args -> {
            Logger logger = LoggerFactory.getLogger(getClass());
            if (personRepository.count() != 0L) {
                logger.info("Using existing database");
                return;
            }
            long seed = 123L;

            logger.info("Generating demo data");

            logger.info("... generating 100 Person entities...");
            personRepository.saveAll(generateData(100, seed));

            logger.info("Generated demo data");
        };
    }

    public static List<Person> generateData(int count, long seed) {
        ExampleDataGenerator<Person> personRepositoryGenerator = new ExampleDataGenerator<>(
                Person.class, seed);
        personRepositoryGenerator.setData(Person::setFirstName,
                DataType.FIRST_NAME);
        personRepositoryGenerator.setData(Person::setLastName,
                DataType.LAST_NAME);
        personRepositoryGenerator.setData(Person::setTitle,
                new TitleGenerator());
        personRepositoryGenerator.setData(Person::setAvatar,
                new AvatarGenerator());
        personRepositoryGenerator.setData(Person::setDepartment,
                new DepartmentGenerator());
        personRepositoryGenerator.setData(Person::setTeam, new TeamGenerator());
        personRepositoryGenerator.setData(Person::setEmail, DataType.EMAIL);
        personRepositoryGenerator.setData(Person::setPhoneNumber,
                new PhoneNumberGenerator());
        return personRepositoryGenerator.create(count);
    }

    public static class TitleGenerator extends DataType<String> {

        @Override
        public String getValue(Random random) {
            List<String> titles = Arrays.asList("Software Developer",
                    "Designer", "Product Owner", "Product Marketer",
                    "Key Account Manager",
                    "Business Development Representative", "Quality Assurance");
            return titles.get(random.nextInt(titles.size()));
        }
    }

    public static class AvatarGenerator extends DataType<String> {

        @Override
        public String getValue(Random random) {
            return "https://i.pravatar.cc/150?img=" + (random.nextInt(70) + 1);
        }
    }

    public static class DepartmentGenerator extends DataType<String> {

        @Override
        public String getValue(Random random) {
            return "Engineering";
        }
    }

    public static class TeamGenerator extends DataType<String> {

        @Override
        public String getValue(Random random) {
            return "Core Products";
        }
    }

    public static class PhoneNumberGenerator extends DataType<String> {

        @Override
        public String getValue(Random random) {
            return "+358 " + (random.nextInt(9000) + 1000) + "  "
                    + (random.nextInt(900) + 100);
        }
    }
}
