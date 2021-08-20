package com.politrons.app;

import com.politrons.service.CastingService;
import io.vavr.Function1;
import io.vavr.concurrent.Future;
import io.vavr.control.Option;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;

import java.io.Serializable;

import static io.vavr.API.*;
import static io.vavr.Patterns.$None;
import static io.vavr.Patterns.$Some;

public class StarWarsActorsApp extends AbstractVerticle {

    @Override
    public void start(Promise<Void> startPromise) {
        println("Running StarWarsActors server....");
        var service = new CastingService();
        HttpServer server = vertx.createHttpServer();
        server.webSocketHandler(ctx ->
                        ctx.textMessageHandler((episode) -> {
                            Match(Option(episode)).of(
                                            Case($Some($()), service::getCastingForEpisode),
                                            Case($None(), Future.of(() -> "No episode info"))
                                    )
                                    .onSuccess(ctx::writeTextMessage)
                                    .onFailure(t -> ctx.writeTextMessage(t.getMessage()));
                        }))
                .listen(8889, asyncResult -> {
                    if (asyncResult.succeeded()) {
                        startPromise.complete();
                        println("StarWarsActors server started on port 8889");
                    } else {
                        startPromise.fail(asyncResult.cause());
                        println("StarWarsActors server failed. Caused by " + asyncResult.cause().getMessage());
                    }
                });
    }
}
