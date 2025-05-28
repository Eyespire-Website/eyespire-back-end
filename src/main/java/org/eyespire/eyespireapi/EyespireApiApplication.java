package org.eyespire.eyespireapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"org.eyespire.eyespireapi"})
public class EyespireApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(EyespireApiApplication.class, args);
    }

}
