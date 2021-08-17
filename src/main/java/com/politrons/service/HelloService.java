package com.politrons.service;

import io.vavr.concurrent.Future;

public class HelloService {
    public Future<String> getReactiveHello() {
        return Future.of(() -> "Hello world from reactive platform");
    }
}

