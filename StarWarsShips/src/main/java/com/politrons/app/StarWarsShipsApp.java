package com.politrons.app;

import akka.Done;
import akka.actor.ActorSystem;
import akka.kafka.ConsumerSettings;
import akka.kafka.ProducerSettings;
import akka.kafka.Subscriptions;
import akka.kafka.javadsl.Consumer;
import akka.kafka.javadsl.Producer;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import com.typesafe.config.Config;
import io.vavr.API;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;


public class StarWarsShipsApp {

    private static final ActorSystem system = ActorSystem.create("StarWarsShipsApp");

    public static void main(String[] args) throws InterruptedException, ExecutionException {
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
                            API.println("Message from Kafka:" + msg.record());
                            return CompletableFuture.completedFuture(msg.record().value())
                                    .thenApply(done -> msg.committableOffset());
                        }

                )
                .to(Sink.ignore())
                .run(system);


        Thread.sleep(5000);

        final Config producerConfig = system.settings().config().getConfig("akka.kafka.producer");
        final ProducerSettings<String, String> producerSettings =
                ProducerSettings.create(producerConfig, new StringSerializer(), new StringSerializer())
                        .withBootstrapServers("localhost:9092");

        CompletionStage<Done> done =
                Source.range(1, 100)
                        .map(number -> number.toString())
                        .map(value -> new ProducerRecord<String, String>("starWarsShips", value))
                        .runWith(Producer.plainSink(producerSettings), system);

        done.toCompletableFuture().get();


    }
}
