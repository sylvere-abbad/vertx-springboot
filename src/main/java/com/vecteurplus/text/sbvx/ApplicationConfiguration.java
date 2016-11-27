package com.vecteurplus.text.sbvx;

import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.mongo.MongoClient;
import io.vertx.rxjava.rabbitmq.RabbitMQClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Created by sylvere_ab on 26/11/2016.
 */
@Configuration
public class ApplicationConfiguration {

  @Autowired
  private Environment env;

  @Bean
  Vertx vertx() {
    return Vertx.vertx();
  }

  @Bean
  JsonObject mongoConfiguration() {
    return new JsonObject()
      .put("host", env.getProperty("mongodb.host"))
      .put("port", env.getProperty("mongodb.port", Integer.class, 27017))
      .put("db_name", env.getProperty("mongodb.db_name"));
  }

  @Bean
  MongoClient mongoClient(Vertx vertx, JsonObject mongoConfiguration) {
    return MongoClient.createShared(vertx, mongoConfiguration);
  }

  @Bean
  JsonObject rabbitmqConfiguration() {
    return new JsonObject()
      .put("host", env.getProperty("rabbitmq.host"))
      .put("port", env.getProperty("rabbitmq.port", Integer.class, 5672))
      .put("user", env.getProperty("rabbitmq.user"))
      .put("password", env.getProperty("rabbitmq.password"))
      .put("virtualHost", env.getProperty("rabbitmq.virtualHost"));
  }

  @Bean
  RabbitMQClient rabbitMQClient(Vertx vertx, JsonObject rabbitmqConfiguration) {
    return RabbitMQClient.create(vertx, rabbitmqConfiguration);
  }
}
