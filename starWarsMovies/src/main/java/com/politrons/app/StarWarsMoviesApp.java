package com.politrons.app;

import com.politrons.service.StarWarsService;
import io.vavr.Tuple2;
import io.vavr.concurrent.Future;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

import static io.vavr.API.*;
import static io.vavr.Patterns.$Failure;
import static io.vavr.Patterns.$Success;

/**
 * Verticle responsible to receive request from client to obtain information
 * of one specific episode.
 * The verticle invoke two other services in the platform in a reactive way:
 * * StarWarsActor: Request using [WebSocket] which we have already open and we use using Concurrency Channel pattern.
 * * StarWarsPlanets: Request using [gRPC] which keep an open [StreamObserver] between services. We usr Google Proto
 */
public class StarWarsMoviesApp extends AbstractVerticle {

    @Override
    public void start(Promise<Void> startPromise) {
        println("Running StarWarsMovies server....");
        var service = new StarWarsService(vertx);
        var server = vertx.createHttpServer();
        var router = Router.router(vertx);

        router.get("/movie/:episode")
                .respond(ctx -> {
                    var promise = Promise.<JsonObject>promise();
                    service.getMovieInfo(ctx.pathParam("episode"))
                            .onComplete(tryResult -> {
                                JsonObject jsonObj = Match(tryResult).of(
                                        Case($Success($()), this::createJsonResponse),
                                        Case($Failure($()), x -> new JsonObject().put("Error", x.getMessage())));
                                promise.complete(jsonObj);
                            });
                    return promise.future();
                }).failureHandler(ctx ->
                        Future.successful(new JsonObject().put("Error",
                                String.format("Server error. Caused by %s", ctx.failure().getMessage()))));

        server.requestHandler(router).
                listen(8888, http -> {
                    if (http.succeeded()) {
                        startPromise.complete();
                        println("StarWarsMovies server started on port 8888");
                    } else {
                        startPromise.fail(http.cause());
                    }
                });
    }

    private JsonObject createJsonResponse(Tuple2<Tuple2<String, String>, String> tuple) {
        JsonObject entries = new JsonObject();
        entries.put("characters", tuple._1._2);
        entries.put("planets", tuple._1._1);
        entries.put("ships", tuple._2);
        return entries;
    }
}
