package com.glamaya.glamayawoocommercesync.port.out;

/**
 * Defines the outbound port for publishing events to a message broker (e.g., Kafka).
 * This interface abstracts the details of the messaging technology,
 * allowing the application core to remain decoupled from the specific
 * message broker implementation.
 */
public interface EventPublisher {
    /**
     * Sends an event to a specified topic.
     *
     * @param topic The name of the topic to send the event to.
     * @param key   The key for the event, used for partitioning.
     * @param value The value (payload) of the event.
     * @param <K>   The type of the event key.
     * @param <V>   The type of the event value.
     */
    <K, V> void send(String topic, K key, V value);
}
