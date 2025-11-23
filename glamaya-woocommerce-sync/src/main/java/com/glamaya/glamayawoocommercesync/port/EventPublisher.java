package com.glamaya.glamayawoocommercesync.port;

public interface EventPublisher {
    <K, V> void send(String topic, K key, V value);
}

