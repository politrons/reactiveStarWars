package com.politrons.app;

import examples.GreeterGrpc;
import examples.HelloReply;
import examples.HelloRequest;
import io.grpc.stub.StreamObserver;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.grpc.VertxServer;
import io.vertx.grpc.VertxServerBuilder;

public class StarWarsPlanetsApp extends AbstractVerticle {

    GreeterGrpc.GreeterImplBase service = new GreeterGrpc.GreeterImplBase() {
        @Override
        public void sayHello(HelloRequest request,
                             StreamObserver<HelloReply> responseObserver) {

            responseObserver.onNext(
                    HelloReply.newBuilder()
                            .setMessage(request.getName())
                            .build());
            responseObserver.onCompleted();
        }
    };

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        VertxServer rpcServer = VertxServerBuilder
                .forAddress(vertx, "localhost", 8810)
                .addService(service)
                .build();

        // Start is asynchronous
        rpcServer.start();
    }
}
