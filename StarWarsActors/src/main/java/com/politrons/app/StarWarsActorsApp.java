package com.politrons.app;

import com.politrons.service.HelloService;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

import static io.vavr.API.*;
import static io.vavr.Patterns.$Failure;
import static io.vavr.Patterns.$Success;

public class StarWarsActorsApp extends AbstractVerticle {

    @Override
    public void start(Promise<Void> startPromise) {

        var service = new HelloService();

        HttpServer server = vertx.createHttpServer();

        Router router = Router.router(vertx);

        router.get("/episode")
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
                });

        server.requestHandler(router).
                listen(8889, http -> {
                    if (http.succeeded()) {
                        startPromise.complete();
                        System.out.println("HTTP server started on port 8888");
                    } else {
                        startPromise.fail(http.cause());
                    }
                });
    }
}
