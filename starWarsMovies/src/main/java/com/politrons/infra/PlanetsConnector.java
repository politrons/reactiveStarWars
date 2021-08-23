package com.politrons.infra;

import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import io.vavr.concurrent.Future;
import io.vavr.concurrent.Promise;
import io.vertx.core.Vertx;
import io.vertx.grpc.VertxChannelBuilder;
import planets.PlanetRequest;
import planets.PlanetResponse;
import planets.StarWarsPlanetServiceGrpc;

/**
 * gRPC connector to connect to StarWarsPlanet.
 * In order to establish a gRPC communication we do:
 * * We create a [ManagedChannel] using patter builder with Vertx Factory [VertxChannelBuilder]
 * * Get a stub [StarWarsPlanetServiceStub] to use for interacting with the remote service.
 * * Using the Stub invoke the function described in contract [getPlanets] passing the request message
 * defined [PlanetRequest]
 * * Then we define the Stream with the callbacks the server will invoke once response.
 */
public class PlanetsConnector {


    private final Vertx vertx;

    public PlanetsConnector(Vertx vertx) {
        this.vertx = vertx;
    }

    public Future<String> makeGrpcRequest(String episode) {
        var promise = Promise.<String>make();
        var channel = VertxChannelBuilder
                .forAddress(vertx, "localhost", 8810)
                .usePlaintext()
                .build();

        StarWarsPlanetServiceGrpc.StarWarsPlanetServiceStub stub = StarWarsPlanetServiceGrpc.newStub(channel);

        PlanetRequest request = PlanetRequest
                .newBuilder()
                .setEpisode(episode)
                .build();

        stub.getPlanets(request, new StreamObserver<>() {
            private PlanetResponse response;

            @Override
            public void onNext(PlanetResponse response) {
                this.response = response;
                promise.success(response.getPlanets());
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println("Could not reach server " + throwable.getMessage());
            }

            @Override
            public void onCompleted() {
                System.out.println("[gRPC] Server response: " + response.getPlanets());
            }
        });
        return promise.future();

    }


}
