package com.politrons.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.vavr.Function1;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;

import static io.vavr.API.*;
import static io.vavr.Patterns.$None;
import static io.vavr.Patterns.$Some;
import static java.lang.String.format;

public class StarWarsActorsApp extends AbstractVerticle {

    @Override
    public void start(Promise<Void> startPromise) {
        println("Running StarWarsActors server....");
        var mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        HttpServer server = vertx.createHttpServer();
        server.webSocketHandler(ctx ->
                        ctx.textMessageHandler((episode) -> {
                            String actors = findActors.apply(episode);
                            ctx.writeTextMessage(actors);
                        }))
                .listen(8889, asyncResult -> {
                    if (asyncResult.succeeded()) {
                        startPromise.complete();
                        println("StarWarsActors server started on port 8888");
                    } else {
                        startPromise.fail(asyncResult.cause());
                        println("StarWarsActors server failed. Caused by " + asyncResult.cause().getMessage());
                    }
                });
    }

    private final Function1<Throwable, String> processRequestError = (t) -> {
        println(format("Error in request. Caused by %s", t.getMessage()));
        return t.getMessage();
    };

    private final Function1<String, String> findActors = (episode) -> {
        println("StarWars:" + episode);
        return Match(Option(episode)).of(
                Case($Some($()), episodeValue -> "Han Solo:" + episodeValue),
                Case($None(), t -> "No episode info")
        );
    };
}
