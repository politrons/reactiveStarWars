package com.politrons.app;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;

import java.util.Random;

public class StarWarsActorsApp extends AbstractVerticle {

    @Override
    public void start() {
        startServer(vertx);
    }

    private void startServer(Vertx vertx) {
        HttpServer server = vertx.createHttpServer();
        server.webSocketHandler(ctx ->
                        ctx.textMessageHandler((msg) -> {
                            System.out.println("Client request:" + msg);
                            if (msg.isEmpty()) {
                                ctx.writeTextMessage("No info");
                            } else {
                                if ((new Random()).nextInt(100) == 0) {
                                    ctx.close();
                                } else {
                                    ctx.writeTextMessage("Han Solo");
                                }
                            }
                        }))
                .listen(8889);
    }

}
