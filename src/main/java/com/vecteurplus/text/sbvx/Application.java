package com.vecteurplus.text.sbvx;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;

/**
 * Created by sylvere_ab on 26/11/2016.
 */
@SpringBootApplication
public class Application {

  @Autowired
  private MainVerticle _mainVerticle;

  public static void main(String[] args) {
    SpringApplication.run(Application.class);
  }

  @PostConstruct
  public void deployVerticles() {
    io.vertx.core.Vertx.vertx().deployVerticle(_mainVerticle);
  }
}
