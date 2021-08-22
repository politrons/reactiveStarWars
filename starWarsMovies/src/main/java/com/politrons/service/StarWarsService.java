package com.politrons.service;

import com.politrons.infra.ActorsConnector;
import com.politrons.infra.PlanetsConnector;
import io.vavr.concurrent.Future;
import io.vertx.core.Vertx;

public class StarWarsService {

    private final ActorsConnector actorsConnector;
    private final PlanetsConnector planetsConnector;

    public StarWarsService(Vertx vertx) {
        planetsConnector = new PlanetsConnector(vertx);
        actorsConnector = new ActorsConnector(vertx);
    }

    public Future<String> getMovieInfo(String episode) {
        planetsConnector.makeRequest();
        return actorsConnector.connect(episode)
                .map(String::toUpperCase)
                .onFailure(t -> System.out.println("Error obtaining actors from service. Caused by " + t.getMessage()));
    }
}

