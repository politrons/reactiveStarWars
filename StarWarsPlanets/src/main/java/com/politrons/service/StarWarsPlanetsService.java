package com.politrons.service;

import io.grpc.stub.StreamObserver;
import planets.PlanetRequest;
import planets.PlanetResponse;
import planets.StarWarsPlanetServiceGrpc;

import java.util.Map;

/**
 * gRPC Service that use the proto file contract to implement the service [StarWarsPlanetServiceGrpc][StarWarsPlanetServiceImplBase]
 * Then we implement that service receiving the stream which we use to invoke the onNext() callback.
 */
public class StarWarsPlanetsService {

    static Map<String, String> planets = Map.of(
            "episode1", "Tatooine, Naboo, Coruscant",
            "episode2", "Coruscant, Kamino, Endor",
            "episode3", "Coruscant, Mustafar, Hoth");

    public static StarWarsPlanetServiceGrpc.StarWarsPlanetServiceImplBase create() {
        return new StarWarsPlanetServiceGrpc.StarWarsPlanetServiceImplBase() {
            @Override
            public void getPlanets(PlanetRequest request,
                                   StreamObserver<PlanetResponse> responseObserver) {

                responseObserver.onNext(
                        PlanetResponse.newBuilder()
                                .setPlanets(planets.getOrDefault(request.getEpisode(), "No planets info"))
                                .build());
                responseObserver.onCompleted();
            }
        };
    }
}
