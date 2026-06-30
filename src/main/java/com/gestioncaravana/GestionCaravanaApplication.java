package com.gestioncaravana;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.gestioncaravana")
public class GestionCaravanaApplication {

  public static void main(String[] args) {
    SpringApplication.run(GestionCaravanaApplication.class, args);
  }
}
