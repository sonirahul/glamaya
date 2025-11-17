package com.glamaya.glamayawoocommercesync.config.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.support.serializer.StringOrBytesSerializer;

@Configuration
public class KafkaProducerConfig {

    KafkaProducerConfig(ProducerFactory<Object, Object> producerFactory, ObjectMapper mapper) {
        var keySerializer = new StringOrBytesSerializer();
        var valueSerializer = new JsonSerializer<>(mapper);
        valueSerializer.setAddTypeInfo(true);
        var factory = (DefaultKafkaProducerFactory<Object, Object>) producerFactory;
        factory.setKeySerializer(keySerializer);
        factory.setValueSerializer(valueSerializer);
    }
}
