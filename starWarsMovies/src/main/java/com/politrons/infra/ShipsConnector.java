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
import scala.concurrent.ExecutionContext;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static io.vavr.API.*;
import static io.vavr.Patterns.$Failure;
import static io.vavr.Patterns.$Success;

/**
 * Connector based in Kafka Alpakka. We use
 */
public class ShipsConnector {

    private final ActorSystem system = ActorSystem.create("StarWarsShipsApp");

    public Future<String> getShips(String episode) {
        sendRequest(episode);
        return getShipsSubscriber();
    }

    /**
     * Kafka Publisher, as we did with the consumer, first we have to create a [ProducerSettings]
     * Then we create an Akka stream [Source] where we emit just one [ProducerRecord] specifying the topic
     * where we want to send the event, and the message.
     * Then we run the stream using [Producer.plainSink] which needs to obtain the ProducerSettings created
     * previously to know how it must connect to Kafka.
     */
    private void sendRequest(String episode) {
        final var producerConfig = system.settings().config().getConfig("akka.kafka.producer");
        final var producerSettings =
                ProducerSettings.create(producerConfig, new StringSerializer(), new StringSerializer())
                        .withBootstrapServers("localhost:9092");

        var publisherResult =
                Source.single(new ProducerRecord<String, String>("starWarsShipsRequest", episode))
                        .runWith(Producer.plainSink(producerSettings), system);

        Match(Try.of(() -> publisherResult.toCompletableFuture().get())).of(
                Case($Success($()), processSuccessChannel()),
                Case($Failure($()), processFailureChannel())
        );
    }

    /**
     * In order to create a Kafka consumer with Alpakka, we use pattern at least one, where
     * Creating first a ConsumerSettings where we configure the Kafka broker info, the
     * strategy of ACK.
     * <p>
     * Then Using [Consumer] we create a [Source] using [committableSource],
     * which makes it possible to commit offset positions to Kafka.
     * <p>
     * Once we receive event from Kafka, We use our Kafka publisher to publish into the response topic.
     * Then we acknowledge the event using the [CommittableOffset] that is sent with the message.
     */
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
                            return ackMessage(msg);
                        }

                )
                .to(Sink.ignore())
                .run(system);
        return promise.future();
    }

    /**
     * Function to acknowledge the event into Kafka to move the offset in kafka broker.
     * In order to do it, we use the [CommittableOffset] provided in the event received.
     * Then we use a Java promise [CompletableFuture] to be filled with success or error
     * depending of what the [commitScaladsl] Future response in [onComplete] callback.
     */
    private CompletableFuture<ConsumerMessage.CommittableOffset> ackMessage(ConsumerMessage.CommittableMessage<String, byte[]> msg) {
        ConsumerMessage.CommittableOffset committableOffset = msg.committableOffset();
        var future = new CompletableFuture<ConsumerMessage.CommittableOffset>();
        committableOffset.commitScaladsl().onComplete(tryResponse -> {
            if (tryResponse.isSuccess()) {
                future.complete(committableOffset);
            } else {
                future.completeExceptionally(tryResponse.failed().get());
            }
            return tryResponse;
        }, ExecutionContext.global());
        return future;
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
