package com.vecteurplus.text.sbvx;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.eventbus.EventBus;
import io.vertx.rxjava.core.eventbus.Message;
import io.vertx.rxjava.core.http.HttpServer;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.rabbitmq.RabbitMQClient;
import io.vertx.rxjava.redis.RedisClient;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import rx.Single;

import javax.annotation.PostConstruct;
import java.util.Arrays;


/**
 * Created by sylvere_ab on 26/11/2016.
 */
@Component
public class MainVerticle extends AbstractVerticle {

  private static final Logger logger = LogManager.getLogger();

  @Autowired
  private RabbitMQClient _rabbitMQClient;

  @Autowired
  private RedisClient _redisClient;

  @Autowired
  private MarcheService _marcheService;

  @Autowired
  private MetricRegistry _metricRegistry;

  private Histogram _histogram;

  @Value("${http.port}")
  private int _httpPort;

  public MainVerticle() {
    //
  }

  @PostConstruct
  protected  void initialize() {
    _histogram = _metricRegistry.histogram("histogram.marches.get");
  }

  private void updateMarche(RoutingContext routingContext) {
    Single.just(routingContext.request().getParam("id"))
      .map(Long::parseLong)
      .flatMap(id -> _marcheService.replace(id, routingContext.getBodyAsJson()))
      .map(updated -> updated ? HttpResponseStatus.OK.code() : HttpResponseStatus.NOT_FOUND.code())
      .subscribe(
        status -> routingContext.response().setStatusCode(status),
        routingContext::fail
      );
  }

  private void deleteMarche(RoutingContext routingContext) {
    Single.just(routingContext.request().getParam("id"))
      .map(Long::parseLong)
      .flatMap(_marcheService::remove)
      .map(deleted -> deleted ? HttpResponseStatus.NO_CONTENT.code() : HttpResponseStatus.NOT_FOUND.code())
      .subscribe(
        status -> routingContext.response().setStatusCode(status),
        routingContext::fail
      );
  }

  private void getMarches(RoutingContext routingContext) {
    Single.just(new FindOptions().setLimit(10))
      .flatMap(options -> _marcheService.find(new JsonObject(), options))
      .map(JsonArray::new)
      .map(results -> results.encodePrettily())
      .subscribe(
        json -> routingContext.response().putHeader("content-type", "application/json").end(json),
        routingContext::fail
      );
  }

  protected StopWatch startWatch() {
    StopWatch stopWatch = new StopWatch();
    stopWatch.start();
    return stopWatch;
  }

  protected void stopWatchAndUpdate(StopWatch stopWatch) {
    stopWatch.stop();
    _histogram.update(stopWatch.getTime());
  }

  private void getMarche(RoutingContext routingContext) {
    StopWatch stopWatch = startWatch();
    Single.just(routingContext.request().getParam("id"))
      .map(Long::parseLong)
      .flatMap(_marcheService::get)
      .doOnSuccess(result -> stopWatchAndUpdate(stopWatch))
      .subscribe(
        marche -> encodeOrNotFound(marche, routingContext),
        routingContext::fail
      );
  }

  private void encodeOrNotFound(JsonObject result, RoutingContext routingContext) {
    if (result == null) routingContext.response().setStatusCode(HttpResponseStatus.NOT_FOUND.code()).end();
    else routingContext.response().putHeader("content-type", "application/json").end(result.encodePrettily());
  }

  private void hello(RoutingContext routingContext) {
    routingContext.response()
      .putHeader("content-type", "text/html")
      .end("<h1>Hello from MainRxVerticle</h1>");
  }

  private void gotMarche(Message<Object> message) {
    JsonObject body = (JsonObject)message.body();
    logger.info("Got message from marches.get : {}", body.encodePrettily());
    message.reply(true);
  }

  private Router setupRouter(Router router) {
    router.route(HttpMethod.GET, "/hello").handler(this::hello);
    router.route(HttpMethod.GET, "/marches").handler(this::getMarches);
    router.route(HttpMethod.GET, "/marches/:id").handler(this::getMarche);
    router.route(HttpMethod.DELETE, "/marches/:id").handler(this::deleteMarche);
    router.route(HttpMethod.PUT, "/marches/:id").handler(this::updateMarche);
    return router;
  }

  private void setupEventBusConsumers(EventBus eventBus) {
    eventBus.consumer("marches.get").handler(this::gotMarche);
  }

  private Single<Void> setupRabbitConsumers(RabbitMQClient rabbit) {
    logger.info("Setting up rabbit consumers.");
    return rabbit.basicConsumeObservable("vertx.marches.get", "marches.get").toSingle()
      .doOnSuccess(aVoid -> logger.info("Consumer for vertx.marches.get queue declared."));
  }

  private Single<JsonObject> setupRabbitTopology(RabbitMQClient rabbit) {
    logger.info("Setting up rabbit topology.");
    return rabbit.queueDeclareObservable("vertx.marches.get", true, false, false).toSingle()
      .doOnSuccess(result -> logger.info("Rabbit topology set up done : {} ", result.encodePrettily()));
  }

  private Single<Void> setupRabbit(RabbitMQClient rabbit) {
    return setupRabbitTopology(rabbit)
      .flatMap(result -> setupRabbitConsumers(rabbit));
  }

  @Override
  public void start(Future<Void> startFuture) throws Exception {
    HttpServer httpServer = vertx.createHttpServer();
    Router router = Router.router(vertx);
    setupRouter(router);
    setupEventBusConsumers(vertx.eventBus());
    httpServer.requestHandler(router::accept);
    Single.zip(
        httpServer.listenObservable(_httpPort).toSingle(),
        _rabbitMQClient.startObservable().toSingle(),
        Arrays::asList
      )
      .flatMap(results -> setupRabbit(_rabbitMQClient))
      .flatMap(result -> Single.<Void>just(null))
      .subscribe(startFuture::complete, startFuture::fail);
  }
}
