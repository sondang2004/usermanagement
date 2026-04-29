package com.example.usermanagement.config;

import com.example.usermanagement.entity.Employee;
import com.example.usermanagement.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final EmployeeRepository employeeRepository;

    @Bean
    public CommandLineRunner initData() {
        return args -> {
            if (employeeRepository.count() == 0) {
                log.info("Initializing sample data...");
                
                Employee employee1 = Employee.builder()
                        .name("John Doe")
                        .email("john.doe@example.com")
                        .position("Software Engineer")
                        .department("IT")
                        .build();

                Employee employee2 = Employee.builder()
                        .name("Jane Smith")
                        .email("jane.smith@example.com")
                        .position("HR Manager")
                        .department("HR")
                        .build();

                employeeRepository.save(employee1);
                employeeRepository.save(employee2);
                
                log.info("Sample data initialization completed.");
            }
        };
    }
}
