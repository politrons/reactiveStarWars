package com.politrons.service;

import com.politrons.infra.ActorsConnector;
import com.politrons.infra.PlanetsConnector;
import io.vavr.Tuple2;
import io.vavr.concurrent.Future;
import io.vertx.core.Vertx;

/**
 * Service that use two connectors, to make two calls in parallel using Vavr Future.
 * Then using [zip] operator we merge both futures together, and as result we obtain
 * a Tuple with both future's outputs.
 */
public class StarWarsService {

    private final ActorsConnector actorsConnector;
    private final PlanetsConnector planetsConnector;

    public StarWarsService(Vertx vertx) {
        planetsConnector = new PlanetsConnector(vertx);
        actorsConnector = new ActorsConnector(vertx);
    }

    public Future<Tuple2<String, String>> getMovieInfo(String episode) {
        Future<String> futurePlanets = planetsConnector.makeGrpcRequest(episode)
                .onFailure(t -> System.out.println("Error obtaining planets from service. Caused by " + t.getMessage()));
        Future<String> charactersFuture = actorsConnector.connect(episode)
                .map(String::toUpperCase)
                .onFailure(t -> System.out.println("Error obtaining characters from service. Caused by " + t.getMessage()));
        return futurePlanets.zip(charactersFuture);
    }
}

