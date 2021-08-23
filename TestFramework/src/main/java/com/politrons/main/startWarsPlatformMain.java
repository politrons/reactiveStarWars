package com.politrons.main;

import com.politrons.app.StarWarsActorsApp;
import com.politrons.app.StarWarsMoviesApp;
import com.politrons.app.StarWarsPlanetsApp;
import io.vertx.core.Vertx;

/**
 * Main class to run the platform
 */
public class startWarsPlatformMain {

    private final static Vertx vertx = Vertx.vertx();

    public static void main(String[] args) throws InterruptedException {
        vertx.deployVerticle(new StarWarsActorsApp());
        Thread.sleep(2000);
        vertx.deployVerticle(new StarWarsMoviesApp());
        Thread.sleep(2000);
        vertx.deployVerticle(new StarWarsPlanetsApp());
        Thread.sleep(600000000);
    }

}
