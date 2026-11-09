package br.com.zela.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ZelaApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZelaApiApplication.class, args);
    }
}
