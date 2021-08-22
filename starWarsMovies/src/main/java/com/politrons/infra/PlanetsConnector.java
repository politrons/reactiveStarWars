package com.politrons.infra;

import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import io.vertx.core.Vertx;
import io.vertx.grpc.VertxChannelBuilder;
import planets.PlanetRequest;
import planets.PlanetResponse;
import planets.StarWarsPlanetServiceGrpc;

public class PlanetsConnector {


    private final Vertx vertx;

    public PlanetsConnector(Vertx vertx) {
        this.vertx = vertx;
    }

    public void makeRequest() {

        ManagedChannel channel = VertxChannelBuilder
                .forAddress(vertx, "localhost", 8810)
                .usePlaintext()
                .build();

        // Get a stub to use for interacting with the remote service
        StarWarsPlanetServiceGrpc.StarWarsPlanetServiceStub stub = StarWarsPlanetServiceGrpc.newStub(channel);

        PlanetRequest request = PlanetRequest.newBuilder().setEpisode("Episode1").build();

        // Call the remote service
        stub.getPlanets(request, new StreamObserver<>() {
            private PlanetResponse response;

            @Override
            public void onNext(PlanetResponse response) {
                this.response = response;
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println("Coult not reach server " + throwable.getMessage());
            }

            @Override
            public void onCompleted() {
                System.out.println("Got the server response: " + response.getPlanets());
            }
        });


    }


}
