package com.politrons.app;

import com.politrons.service.HelloService;
import io.vavr.concurrent.Future;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

import static io.vavr.API.*;
import static io.vavr.Patterns.$Failure;
import static io.vavr.Patterns.$Success;

public class StarWarsMoviesApp extends AbstractVerticle {

    @Override
    public void start(Promise<Void> startPromise) {

        var service = new HelloService();

        HttpServer server = vertx.createHttpServer();

        Router router = Router.router(vertx);

        router.get("/movie")
                .respond(ctx -> {
                    var promise = Promise.<JsonObject>promise();
                    service.getReactiveHello()
                            .onComplete(tryResult -> {
                                JsonObject jsonObj = Match(tryResult).of(
                                        Case($Success($()), value -> new JsonObject().put("message", value)),
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
                        System.out.println("HTTP server started on port 8888");
                    } else {
                        startPromise.fail(http.cause());
                    }
                });
    }
}