package com.politrons.app;

import akka.actor.ActorSystem;
import akka.kafka.ConsumerSettings;
import akka.kafka.ProducerSettings;
import akka.kafka.Subscriptions;
import akka.kafka.javadsl.Consumer;
import akka.kafka.javadsl.Producer;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import io.vavr.control.Try;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static io.vavr.API.println;


public class StarWarsShipsApp {

    private static final ActorSystem system = ActorSystem.create("StarWarsShipsApp");

    static Map<String, String> ships = Map.of(
            "episode1", "Falcon",
            "episode2", "Tie",
            "episode3", "Destroyer");

    public static void main(String[] args) {
        kafkaConsumer();
    }

    private static void kafkaConsumer() {
        final var config = system.settings().config().getConfig("akka.kafka.consumer");
        final var consumerSettings =
                ConsumerSettings.create(config, new StringDeserializer(), new ByteArrayDeserializer())
                        .withBootstrapServers("localhost:9092")
                        .withGroupId("starWarsShipsGroupId")
                        .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
                        .withProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true")
                        .withProperty(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "5000");

        Consumer.committableSource(consumerSettings, Subscriptions.topics("starWarsShips"))
                .mapAsync(
                        1,
                        msg -> {
                            String episode = new String(msg.record().value());
                            println("Request episode from client:" + episode);
                            kafkaPublisher(ships.getOrDefault(episode,"No ships info"));
                            return CompletableFuture.completedFuture(episode)
                                    .thenApply(done -> msg.committableOffset());
                        }

                )
                .to(Sink.ignore())
                .run(system);
    }

    private static void kafkaPublisher(String message) {
        final var producerConfig = system.settings().config().getConfig("akka.kafka.producer");
        final var producerSettings =
                ProducerSettings.create(producerConfig, new StringSerializer(), new StringSerializer())
                        .withBootstrapServers("localhost:9092");

        var publisherResult =
                Source.single(new ProducerRecord<String, String>("starWarsShipsResponse", message))
                        .runWith(Producer.plainSink(producerSettings), system);
        var messageSent = Try.of(() -> publisherResult.toCompletableFuture().get());
        if(messageSent.isFailure()) {
            println("Error sending back message to Kafka. Caused by " + messageSent.getCause());
        }
    }
}
