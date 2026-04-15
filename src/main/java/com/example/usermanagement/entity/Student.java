package com.example.usermanagement.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;
    @Entity
    @Table(name = "Astudent")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public class Student {

        @Id
        @GeneratedValue
        @UuidGenerator
        private UUID id;
        private String name;
        private String email;
    }