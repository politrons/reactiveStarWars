package com.politrons.it;

import com.politrons.app.StarWarsActorsApp;
import com.politrons.app.StarWarsMoviesApp;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class ReactiveStarWarsIT {

    Vertx vertx = Vertx.vertx();

    @Test
    public void endToEnd() throws InterruptedException {
        vertx.deployVerticle(new StarWarsActorsApp());
        Thread.sleep(2000);
        vertx.deployVerticle(new StarWarsMoviesApp());
//        Thread.sleep(6000000);
    }
}
