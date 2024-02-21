package me.a06.meditrack;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class CustomConfiguration {

    @Bean
    public List<String> stringList() {
        return new ArrayList<>();
    }

}
