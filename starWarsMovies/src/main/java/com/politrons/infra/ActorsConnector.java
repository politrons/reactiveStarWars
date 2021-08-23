package com.politrons.infra;

import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.processors.BehaviorProcessor;
import io.reactivex.rxjava3.processors.FlowableProcessor;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.processors.ReplayProcessor;
import io.vavr.Function2;
import io.vavr.concurrent.Future;
import io.vavr.concurrent.Promise;
import io.vavr.control.Try;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.WebSocket;

import java.util.concurrent.TimeUnit;

import static io.vavr.API.println;

/**
 * WebSocket connector: Using Vertx WebSocket, we keep an open communication between the service and
 * the StarWarsActor service.
 * Connector responsible to obtain the actors of one particular movie.
 */
public class ActorsConnector {

    private final Vertx vertx;
    private final PublishProcessor<String> channelRequest = PublishProcessor.create();
    private final PublishProcessor<String> channelResponse = PublishProcessor.create();

    public ActorsConnector(Vertx vertx) {
        this.vertx = vertx;
        actorsStream();
    }

    public Future<String> connect(String episode) {
        Promise<String> promiseResponse = Promise.make();
        Disposable subscribe = channelResponse.subscribe(actors -> Try.of(() -> promiseResponse.success(actors)));
        println(subscribe.isDisposed());
        boolean send = channelRequest.offer(episode);
        println("Request send successful:" + send);
        return promiseResponse.future();
    }

    /**
     * WebSocket stream that keep the communication open between servers.
     * We use [PublishProcessor] as Channels where we have the [channelRequest] where we
     * receive the info from the client for the request, and the [channelResponse]
     * which is the channel we use to complete a future once we receive the response from the other
     * server.
     */
    private void actorsStream() {
        HttpClient client = vertx.createHttpClient();
        client.webSocket(8889, "localhost", "/", (asyncResult) -> {
            WebSocket ws = asyncResult.result();
            sendRequestFunc.apply(ws, "none");
            Disposable subscribe = channelRequest
                    .subscribe(episode -> sendRequestFunc.apply(ws, episode),
                            t -> println("Error in communication between servers"));
            println("Actor Stream ready:" + subscribe.isDisposed());
            ws.textMessageHandler((response) -> {
                println("[WebSocket] Server response:" + response);
                var responseStatus = channelResponse.offer(response);
                if (!responseStatus) println("Error sending actors info to the client");
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
