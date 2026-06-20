package com.partion.blockchain;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@EnableKafka
@SpringBootApplication
public class PartionBlockchainApplication {

    public static void main(String[] args) {
        SpringApplication.run(PartionBlockchainApplication.class, args);
    }
}
