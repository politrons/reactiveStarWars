package com.politrons.service;

import io.vavr.concurrent.Future;

public class HelloService {

    public Future<String> getReactiveHello() {
        return Future.of(() -> "Hello world from reactive platform")
                .map(String::toUpperCase)
                .onFailure(t -> System.out.println("Error obtaining actors from service. Caused by " + t.getMessage()))
                .flatMap(text -> Future.of(() -> text + "!!!"));
    }
}

