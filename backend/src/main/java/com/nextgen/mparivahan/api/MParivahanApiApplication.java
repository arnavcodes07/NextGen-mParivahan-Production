package com.nextgen.mparivahan.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class MParivahanApiApplication {
    public static void main(String[] args) { SpringApplication.run(MParivahanApiApplication.class, args); }

    @Bean CommandLineRunner seed(JdbcTemplate jdbc) {
        return args -> {
            jdbc.update("MERGE INTO app_users (mobile, mpin, full_name, email) KEY(mobile) VALUES (?,?,?,?)",
                    "9560559548", "8554", "Arnav Sharma", "demo@example.com");
            jdbc.update("MERGE INTO app_users (mobile, mpin, full_name, email) KEY(mobile) VALUES (?,?,?,?)",
                    "9876543210", "1234", "Arnav Sharma", "demo@example.com");
            jdbc.update("MERGE INTO vehicle_records (registration, owner_name, state_name, city, vehicle, fuel, status) KEY(registration) VALUES (?,?,?,?,?,?,?)",
                    "DL01AB1000", "Aarav Sharma", "Delhi", "New Delhi", "Maruti Swift", "Petrol", "Demo record");
            jdbc.update("MERGE INTO vehicle_records (registration, owner_name, state_name, city, vehicle, fuel, status) KEY(registration) VALUES (?,?,?,?,?,?,?)",
                    "MH01AB1001", "Priya Verma", "Maharashtra", "Mumbai", "Hyundai i20", "Diesel", "Demo record");
            jdbc.update("MERGE INTO vehicle_records (registration, owner_name, state_name, city, vehicle, fuel, status) KEY(registration) VALUES (?,?,?,?,?,?,?)",
                    "UP01AB1002", "Rahul Singh", "Uttar Pradesh", "Lucknow", "Tata Nexon", "Electric", "Demo record");
            jdbc.update("MERGE INTO licence_records (licence_number,owner_name,state_name,status) KEY(licence_number) VALUES (?,?,?,?)",
                    "DL-DEMO-2021-014", "Aarav Sharma", "Delhi", "Valid demo record");
            jdbc.update("MERGE INTO licence_records (licence_number,owner_name,state_name,status) KEY(licence_number) VALUES (?,?,?,?)",
                    "MH-DEMO-2022-015", "Priya Verma", "Maharashtra", "Valid demo record");
        };
    }
}
