package com.politrons.service;

import com.politrons.infra.ConnectorActors;
import io.vavr.concurrent.Future;
import io.vertx.core.Vertx;

public class StarWarsService {

    private Vertx vertx;
    private ConnectorActors connector;

    public StarWarsService(Vertx vertx) {
        this.vertx = vertx;
        connector  = new ConnectorActors(vertx);
    }

    public Future<String> getMovieInfo(String episode) {
        return connector.connect(episode);
//        return Future.of(() -> "Hello world from reactive platform")
//                .map(String::toUpperCase)
//                .onFailure(t -> System.out.println("Error obtaining actors from service. Caused by " + t.getMessage()))
//                .flatMap(text -> Future.of(() -> text + "!!!"));
    }
}

