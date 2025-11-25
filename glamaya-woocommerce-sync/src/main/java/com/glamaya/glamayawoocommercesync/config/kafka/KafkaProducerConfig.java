package com.glamaya.glamayawoocommercesync.config.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.support.serializer.StringOrBytesSerializer;

/**
 * Configuration class for setting up Kafka producer factories.
 * This configures the key and value serializers for Kafka messages,
 * ensuring proper serialization of Java objects to Kafka.
 */
@Configuration
public class KafkaProducerConfig {

    /**
     * Configures the {@link ProducerFactory} with custom key and value serializers.
     * The key is serialized as a String or Bytes, and the value is serialized as JSON.
     *
     * @param producerFactory The {@link ProducerFactory} to configure.
     * @param mapper          The {@link ObjectMapper} used for JSON serialization.
     */
    KafkaProducerConfig(ProducerFactory<Object, Object> producerFactory, ObjectMapper mapper) {
        // Key serializer for Kafka messages
        var keySerializer = new StringOrBytesSerializer();
        // Value serializer for Kafka messages, using Jackson for JSON
        var valueSerializer = new JsonSerializer<>(mapper);
        valueSerializer.setAddTypeInfo(true); // Include type information in JSON for deserialization

        // Apply custom serializers to the producer factory
        var factory = (DefaultKafkaProducerFactory<Object, Object>) producerFactory;
        factory.setKeySerializer(keySerializer);
        factory.setValueSerializer(valueSerializer);
    }
}
