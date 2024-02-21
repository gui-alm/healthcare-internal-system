package me.a06.meditrack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EntityScan("me.a06.meditrack.domain")
public class MeditrackApplication {

    public static void main(String[] args) {
        SpringApplication.run(MeditrackApplication.class, args);
    }

}
