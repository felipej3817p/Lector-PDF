package com.backend.repository;

import com.backend.model.AreaCode;
import com.backend.model.Employee;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends MongoRepository<Employee, String> {
    Optional<Employee> findByDocumentNumber(String documentNumber);
    List<Employee> findByAreaCodeIn(Collection<AreaCode> areaCodes);
}