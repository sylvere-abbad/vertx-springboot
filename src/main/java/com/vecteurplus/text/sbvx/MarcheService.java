package com.vecteurplus.text.sbvx;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
import rx.Single;

import java.util.List;

/**
 * Created by sylvere_ab on 23/11/2016.
 */
public interface MarcheService {

  Single<JsonObject> get(Long id);

  Single<JsonObject> get(Long id, JsonObject fields);

  Single<List<JsonObject>> find(JsonObject query);

  Single<List<JsonObject>> find(JsonObject query, FindOptions options);

  Single<Boolean> remove(Long id);

  Single<Boolean> replace(Long id, JsonObject doc);

}
