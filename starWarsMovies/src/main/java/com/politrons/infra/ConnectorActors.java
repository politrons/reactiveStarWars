package com.politrons.infra;

import io.vavr.Function2;
import io.vavr.concurrent.Future;
import io.vavr.concurrent.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.WebSocket;

import java.util.concurrent.TimeUnit;

import static io.vavr.API.println;

/**
 * Connector responsible to obtain the actors of one particular movie.
 */
public class ConnectorActors {

    private final Vertx vertx;
    private Promise<String> promiseRequest;
    private Promise<String> promiseResponse;

    public ConnectorActors(Vertx vertx) {
        this.vertx = vertx;
        promiseRequest = Promise.make();
        promiseResponse = Promise.make();
        actorsStream();
    }

    public Future<String> connect(String episode) {
        Future<String> futureResponse = promiseResponse.future();
        promiseRequest.success(episode);
        return futureResponse;
    }

    private void actorsStream() {
        HttpClient client = vertx.createHttpClient();
        client.webSocket(8889, "localhost", "/", (asyncResult) -> {
            WebSocket ws = asyncResult.result();
            sendRequestFunc.apply(ws, "none");
            ws.textMessageHandler((response) -> {
                println("Server response:" + response);
                promiseResponse.success(response);
                promiseResponse = Promise.make();
                promiseRequest = Promise.make();
                promiseRequest.future()
                        .onSuccess(episode -> sendRequestFunc.apply(ws, episode));
            }).exceptionHandler((e) -> {
                System.out.println("Closed, restarting in 10 seconds");
                restart(client, 5);
            }).closeHandler((__) -> {
                System.out.println("Closed, restarting in 10 seconds");
                restart(client, 10);
            });
        });
    }

    private final Function2<WebSocket, String, String> sendRequestFunc =
            (ws, episode) -> {
                ws.writeTextMessage(episode);
                return episode;
            };

    private void restart(HttpClient client, int delay) {
        client.close();
        vertx.setTimer(TimeUnit.SECONDS.toMillis(delay), (__) -> {
            actorsStream();
        });
    }

}
