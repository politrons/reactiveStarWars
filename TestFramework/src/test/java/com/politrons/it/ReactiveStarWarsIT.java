package com.politrons.it;

import com.politrons.app.StarWarsActorsApp;
import com.politrons.app.StarWarsMoviesApp;
import com.politrons.app.StarWarsPlanetsApp;
import io.vavr.concurrent.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.junit5.VertxExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class ReactiveStarWarsIT {

    private final Vertx vertx = Vertx.vertx();

    @Test
    public void endToEnd() throws InterruptedException {
        vertx.deployVerticle(new StarWarsActorsApp());
        Thread.sleep(2000);
        vertx.deployVerticle(new StarWarsMoviesApp());
        Thread.sleep(2000);
        vertx.deployVerticle(new StarWarsPlanetsApp());

        var promise = Promise.<JsonObject>make();
        WebClient client = WebClient.create(vertx);
        client
                .get(8888, "localhost", "/movie/episode1")
                .as(BodyCodec.jsonObject())
                .send()
                .onSuccess(response -> promise.success(response.body()))
                .onFailure(promise::failure);

        JsonObject entries = promise.future().get();
        assert entries.getString("characters").contains("ANAKIN");
        assert entries.getString("planets").contains("Tatooine");
    }
}
