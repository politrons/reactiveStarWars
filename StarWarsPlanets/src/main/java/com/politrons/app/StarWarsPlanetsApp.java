package com.politrons.app;

import io.grpc.stub.StreamObserver;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.grpc.VertxServer;
import io.vertx.grpc.VertxServerBuilder;
import planets.PlanetRequest;
import planets.PlanetResponse;
import planets.StarWarsPlanetServiceGrpc;

public class StarWarsPlanetsApp extends AbstractVerticle {

    StarWarsPlanetServiceGrpc.StarWarsPlanetServiceImplBase service = new StarWarsPlanetServiceGrpc.StarWarsPlanetServiceImplBase() {
        @Override
        public void getPlanets(PlanetRequest request,
                               StreamObserver<PlanetResponse> responseObserver) {

            responseObserver.onNext(
                    PlanetResponse.newBuilder()
                            .setPlanets(request.getEpisode())
                            .build());
            responseObserver.onCompleted();
        }
    };

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        VertxServer rpcServer = VertxServerBuilder
                .forAddress(vertx, "localhost", 8810)
                .addService(service)
                .build();

        // Start is asynchronous
        rpcServer.start();
    }
}
