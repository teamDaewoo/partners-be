package com.dooring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class DooringBffApplication {

    public static void main(String[] args) {
        SpringApplication.run(DooringBffApplication.class, args);
    }

}
