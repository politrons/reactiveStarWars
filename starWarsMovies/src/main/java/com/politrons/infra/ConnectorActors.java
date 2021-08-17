package com.politrons.infra;

import io.vavr.concurrent.Future;
import io.vavr.concurrent.Promise;
import io.vavr.control.Try;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.WebSocket;

import java.util.concurrent.TimeUnit;

/**
 * Connector responsible to obtain the actors of one particular movie.
 */
public class ConnectorActors {

    private final Vertx vertx;
    private Promise<String> promiseRequest;
    private Promise<String> promiseResponse;

    public ConnectorActors(Vertx vertx) {
        this.vertx = vertx;
        promiseResponse = Promise.make();
        promiseRequest = Promise.make();
        actorsStream();
    }

    public Future<String> connect(String episode) {
        promiseRequest.success(episode);
        return promiseResponse.future();
    }

    private void actorsStream() {
        HttpClient client = vertx.createHttpClient();
        client.webSocket(8889, "localhost", "/", (asyncResult) -> {
            WebSocket ws = asyncResult.result();
            ws.writeTextMessage("");
            ws.textMessageHandler((response) -> {
                System.out.println("Server response:" + response);
                promiseResponse.success(response);
                promiseResponse = Promise.make();
                promiseRequest = Promise.make();
                promiseRequest.future()
                        .onSuccess(ws::writeTextMessage);
            }).exceptionHandler((e) -> {
                System.out.println("Closed, restarting in 10 seconds");
                restart(client, 5);
            }).closeHandler((__) -> {
                System.out.println("Closed, restarting in 10 seconds");
                restart(client, 10);
            });

        });
    }


    private void restart(HttpClient client, int delay) {
        client.close();
        vertx.setTimer(TimeUnit.SECONDS.toMillis(delay), (__) -> {
            actorsStream();
        });
    }

}
