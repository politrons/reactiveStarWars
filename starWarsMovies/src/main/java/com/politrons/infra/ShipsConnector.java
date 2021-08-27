package com.politrons.infra;

import akka.Done;
import akka.actor.ActorSystem;
import akka.kafka.ConsumerMessage;
import akka.kafka.ConsumerSettings;
import akka.kafka.ProducerSettings;
import akka.kafka.Subscriptions;
import akka.kafka.javadsl.Consumer;
import akka.kafka.javadsl.Producer;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import io.vavr.concurrent.Future;
import io.vavr.concurrent.Promise;
import io.vavr.control.Try;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.concurrent.CompletableFuture;
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
        sendRequest(episode);
        return getShipsSubscriber();
    }

    private void sendRequest(String episode) {
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
    }

    private Future<String> getShipsSubscriber() {
        var promise = Promise.<String>make();
        final var config = system.settings().config().getConfig("akka.kafka.consumer");
        final var consumerSettings =
                ConsumerSettings.create(config, new StringDeserializer(), new ByteArrayDeserializer())
                        .withBootstrapServers("localhost:9092")
                        .withGroupId("starWarsShipsGroupId")
                        .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
                        .withProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true")
                        .withProperty(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "5000");

        Consumer.committableSource(consumerSettings, Subscriptions.topics("starWarsShipsResponse"))
                .mapAsync(
                        1,
                        msg -> {
                            String message = new String(msg.record().value());
                            println("Episode ships response from Kafka:" + message);
                            promise.success(message);
                            return ackMessage(msg, message);
                        }

                )
                .to(Sink.ignore())
                .run(system);
        return promise.future();
    }

    private CompletableFuture<ConsumerMessage.CommittableOffset> ackMessage(ConsumerMessage.CommittableMessage<String, byte[]> msg, String message) {
        return CompletableFuture.completedFuture(message)
                .thenApply(done -> msg.committableOffset());
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
