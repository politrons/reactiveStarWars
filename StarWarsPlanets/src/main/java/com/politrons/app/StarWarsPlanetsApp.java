package com.politrons.app;

import com.politrons.service.StarWarsPlanetsService;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.grpc.VertxServer;
import io.vertx.grpc.VertxServerBuilder;

/**
 * Using vertx-grpc library we can create a [VertxServer] using building pattern with
 * [VertxServerBuilder] factory.
 * Then we add the gRPC Service defined using Protobuf contract file.
 */
public class StarWarsPlanetsApp extends AbstractVerticle {

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        VertxServer rpcServer = VertxServerBuilder
                .forAddress(vertx, "localhost", 8810)
                .addService(StarWarsPlanetsService.create())
                .build();

        // Start is asynchronous
        rpcServer.start();
    }
}
