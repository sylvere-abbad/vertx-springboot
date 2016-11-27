package com.vecteurplus.text.sbvx;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.rxjava.ext.mongo.MongoClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rx.Single;

import java.util.List;

/**
 * Created by sylvere_ab on 23/11/2016.
 */
@Service
public class MarcheServiceImpl implements MarcheService {

  private static final String COLLECTION = "marche";

  @Autowired
  private MongoClient _mongo;

  public MarcheServiceImpl() {}

  private Single<JsonObject> toQueryById(Long id) {
    return Single.just(new JsonObject())
      .map(query -> query.put("_id", id));
  }

  @Override
  public Single<JsonObject> get(Long id) {
    return get(id, null);
  }

  @Override
  public Single<JsonObject> get(Long id, JsonObject fields) {
    return toQueryById(id)
      .flatMap(queryById -> _mongo.findOneObservable(COLLECTION, queryById, fields).toSingle());
  }

  @Override
  public Single<List<JsonObject>> find(JsonObject query) {
    return find(query, new FindOptions());
  }

  @Override
  public Single<List<JsonObject>> find(JsonObject query, FindOptions options) {
    return _mongo.findWithOptionsObservable(COLLECTION, query, options).toSingle();
  }

  @Override
  public Single<Boolean> remove(Long id) {
    return toQueryById(id)
      .flatMap(queryById -> _mongo.removeDocumentObservable(COLLECTION, queryById).toSingle())
      .map(result -> result.getRemovedCount() == 1);
  }

  @Override
  public Single<Boolean> replace(Long id, JsonObject doc) {
    return toQueryById(id)
      .flatMap(queryById -> _mongo.replaceDocumentsObservable(COLLECTION, queryById, doc).toSingle())
      .map(result -> result.getDocModified() == 1);
  }
}
