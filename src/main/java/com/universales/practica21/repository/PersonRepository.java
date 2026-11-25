package com.universales.practica21.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.universales.practica21.entity.Person;

@Repository("personRepository")
public interface PersonRepository extends JpaRepository<Person, Long>{
	
	Optional<Person> findByusername(String username);

}
