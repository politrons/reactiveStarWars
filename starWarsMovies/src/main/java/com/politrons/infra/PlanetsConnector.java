package com.politrons.infra;

import examples.GreeterGrpc;
import examples.HelloReply;
import examples.HelloRequest;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import io.vertx.core.Vertx;
import io.vertx.grpc.VertxChannelBuilder;

public class PlanetsConnector {


    private final Vertx vertx;

    public PlanetsConnector(Vertx vertx) {
        this.vertx = vertx;
    }

    public void makeRequest() {

        ManagedChannel channel = VertxChannelBuilder
                .forAddress(vertx, "localhost", 8810)
                .usePlaintext()
                .build();

        // Get a stub to use for interacting with the remote service
        GreeterGrpc.GreeterStub stub = GreeterGrpc.newStub(channel);

        HelloRequest request = HelloRequest.newBuilder().setName("Julien").build();

        // Call the remote service
        stub.sayHello(request, new StreamObserver<>() {
            private HelloReply helloReply;

            @Override
            public void onNext(HelloReply helloReply) {
                this.helloReply = helloReply;
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println("Coult not reach server " + throwable.getMessage());
            }

            @Override
            public void onCompleted() {
                System.out.println("Got the server response: " + helloReply.getMessage());
            }
        });


    }


}
