package com.politrons.service;

import com.politrons.infra.ActorsConnector;
import com.politrons.infra.PlanetsConnector;
import com.politrons.infra.ShipsConnector;
import io.vavr.Tuple2;
import io.vavr.concurrent.Future;
import io.vertx.core.Vertx;

/**
 * Service that use three connectors, to make three calls in parallel using Vavr Future.
 * Then using [zip] operator we merge futures together, and as result we obtain
 * a Tuple with future's outputs.
 */
public class StarWarsService {

    private final ActorsConnector actorsConnector;
    private final PlanetsConnector planetsConnector;
    private final ShipsConnector shipsConnector;

    public StarWarsService(Vertx vertx) {
        planetsConnector = new PlanetsConnector(vertx);
        actorsConnector = new ActorsConnector(vertx);
        shipsConnector = new ShipsConnector();
    }

    public Future<Tuple2<Tuple2<String, String>, String>> getMovieInfo(String episode) {
        var futurePlanets = planetsConnector.makeGrpcRequest(episode)
                .onFailure(t -> System.out.println("Error obtaining planets from service. Caused by " + t.getMessage()));
        var charactersFuture = actorsConnector.connect(episode)
                .map(String::toUpperCase)
                .onFailure(t -> System.out.println("Error obtaining characters from service. Caused by " + t.getMessage()));
        var shipsFuture = shipsConnector.getShips(episode)
                .map(String::toUpperCase)
                .onFailure(t -> System.out.println("Error obtaining ships from service. Caused by " + t.getMessage()));
        return futurePlanets.zip(charactersFuture).zip(shipsFuture);
    }
}

