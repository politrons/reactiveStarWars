package com.politrons.infra;

import akka.Done;
import akka.actor.ActorSystem;
import akka.kafka.ProducerSettings;
import akka.kafka.javadsl.Producer;
import akka.stream.javadsl.Source;
import io.vavr.concurrent.Future;
import io.vavr.control.Try;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.function.Function;

import static io.vavr.API.*;
import static io.vavr.Patterns.$Failure;
import static io.vavr.Patterns.$Success;

/**
 * Connector based in Kafka. We use
 */
public class ShipsConnector {

    private final ActorSystem system = ActorSystem.create("StarWarsShipsApp");

    public Future<String> getShips(String episode) {

        final var producerConfig = system.settings().config().getConfig("akka.kafka.producer");
        final var producerSettings =
                ProducerSettings.create(producerConfig, new StringSerializer(), new StringSerializer())
                        .withBootstrapServers("localhost:9092");

        var publisherResult =
                Source.single(new ProducerRecord<String, String>("starWarsShips", episode))
                        .runWith(Producer.plainSink(producerSettings), system);

        Match(Try.of(() -> publisherResult.toCompletableFuture().get())).of(
                Case($Success($()), processSuccessChannel()),
                Case($Failure($()), processFailureChannel())
        );

        return Future.successful("Falcon");
    }

    private Function<Done, Done> processSuccessChannel() {
        return done -> {
            println("Message sent successful");
            return done;
        };
    }

    private Function<Throwable, Throwable> processFailureChannel() {
        return t -> {
            println("Message not sent. Caused by " + t);
            return t;
        };
    }

}
