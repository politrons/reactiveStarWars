package com.politrons.infra;

import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.processors.BehaviorProcessor;
import io.reactivex.rxjava3.processors.FlowableProcessor;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.processors.ReplayProcessor;
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

    private PublishProcessor<String> channelRequest = PublishProcessor.create();
    private PublishProcessor<String> channelResponse = PublishProcessor.create();


    public ConnectorActors(Vertx vertx) {
        this.vertx = vertx;
        actorsStream();
    }

    public Future<String> connect(String episode) {
        Promise<String> promiseResponse = Promise.make();
        Disposable subscribe = channelResponse.subscribe(actors -> promiseResponse.success(actors));
        println(subscribe.isDisposed());
        boolean send = channelRequest.offer(episode);
        println("Request send successfull:" + send);
        return promiseResponse.future();
    }

    /**
     * WebSocket stream that keep the communication open between servers.
     * We use [Promises] as Channels where we have the [promiseRequest] where we
     * receive the info from the client for the request, and the [promiseResponse]
     * which is the channel/future we use once we receive the response from the other
     * server.
     */
    private void actorsStream() {
        HttpClient client = vertx.createHttpClient();
        client.webSocket(8889, "localhost", "/", (asyncResult) -> {
            WebSocket ws = asyncResult.result();
            sendRequestFunc.apply(ws, "none");
            channelRequest
                    .subscribe(episode -> sendRequestFunc.apply(ws, episode),
                            t -> println("Error in communication between servers"));
            ws.textMessageHandler((response) -> {
                println("Server response:" + response);
                channelResponse.offer(response);
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
                println("Sending request for movie:" + episode);
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
