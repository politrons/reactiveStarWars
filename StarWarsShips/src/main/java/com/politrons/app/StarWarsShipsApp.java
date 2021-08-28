package com.politrons.app;

import akka.Done;
import akka.actor.ActorSystem;
import akka.kafka.ConsumerSettings;
import akka.kafka.ProducerSettings;
import akka.kafka.Subscriptions;
import akka.kafka.javadsl.Committer;
import akka.kafka.javadsl.Consumer;
import akka.kafka.javadsl.Producer;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import com.typesafe.config.Config;
import io.vavr.control.Try;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static io.vavr.API.println;

/**
 * We use Akka Alpakka to create a Kafka consumer to receive request from other services in topic [starWarsShipsRequest], and
 * a producer to send the response into topic [starWarsShipsResponse].
 */
public class StarWarsShipsApp {

    private static final ActorSystem system = ActorSystem.create("StarWarsShipsApp");

    static Map<String, String> ships = Map.of(
            "episode1", "Falcon",
            "episode2", "Tie",
            "episode3", "Destroyer");

    public static void main(String[] args) {
        kafkaConsumer();
    }

    /**
     * In order to create a Kafka consumer with Alpaka, we use pattern at least one, where
     * Creating first a ConsumerSettings where we configure the Kafka broker info, the
     * strategy of ACK.
     *
     * Then Using [Consumer] we create a [Source] using [committableSource],
     * which makes it possible to commit offset positions to Kafka.
     *
     * Once we receive event from Kafka, We use our Kafka publisher to publish into the response topic.
     * Then we acknowledge the event using the [CommittableOffset] that is sent with the message.
     */
    private static void kafkaConsumer() {
        final var config = system.settings().config().getConfig("akka.kafka.consumer");
        final var consumerSettings = createConsumerSettings(config);
        Consumer.committableSource(consumerSettings, Subscriptions.topics("starWarsShipsRequest"))
                .mapAsync(
                        1,
                        msg -> {
                            String episode = new String(msg.record().value());
                            println("Request episode from client:" + episode);
                            kafkaPublisher(ships.getOrDefault(episode, "No ships info"));
                            Future<Done> doneFuture = msg.committableOffset().commitScaladsl();
                            final var completableFuture = new CompletableFuture<Done>();
                            doneFuture.onComplete(tryResponse -> {
                                if (tryResponse.isSuccess()) {
                                    completableFuture.complete(tryResponse.get());
                                } else {
                                    completableFuture.completeExceptionally(tryResponse.failed().get());
                                }
                                return completableFuture;
                            }, ExecutionContext.global());
                            return completableFuture;
                        }

                )
                .to(Sink.ignore())
                .run(system);
    }

    /**
     * Kafka Publisher, as we did with the consumer, first we have to create a [ProducerSettings]
     * Then we create an Akka stream [Source] where we emit just one [ProducerRecord] specifying the topic
     * where we want to send the event, and the message.
     * Then we run the stream using [Producer.plainSink] which needs to obtain the ProducerSettings created
     * previously to know how it must connect to Kafka.
     */
    private static void kafkaPublisher(String message) {
        final var producerConfig = system.settings().config().getConfig("akka.kafka.producer");
        final var producerSettings = createProducerSettings(producerConfig);

        var publisherResult =
                Source.single(new ProducerRecord<String, String>("starWarsShipsResponse", message))
                        .runWith(Producer.plainSink(producerSettings), system);
        var messageSent = Try.of(() -> publisherResult.toCompletableFuture().get());
        if (messageSent.isFailure()) {
            println("Error sending back message to Kafka. Caused by " + messageSent.getCause());
        }
    }

    private static ConsumerSettings<String, byte[]> createConsumerSettings(Config config) {
        return ConsumerSettings.create(config, new StringDeserializer(), new ByteArrayDeserializer())
                .withBootstrapServers("localhost:9092")
                .withGroupId("starWarsShipsGroupId")
                .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
                .withProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true")
                .withProperty(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "5000");
    }

    private static ProducerSettings<String, String> createProducerSettings(Config producerConfig) {
        return ProducerSettings.create(producerConfig, new StringSerializer(), new StringSerializer())
                .withBootstrapServers("localhost:9092");
    }
}
