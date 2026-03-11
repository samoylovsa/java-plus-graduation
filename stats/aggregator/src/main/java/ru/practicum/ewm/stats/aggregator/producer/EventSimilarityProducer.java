package ru.practicum.ewm.stats.aggregator.producer;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventSimilarityProducer {

    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    @Value("${app.kafka.topics.events-similarity}")
    private String eventsSimilarityTopic;

    @PreDestroy
    public void cleanup() {
        try {
            kafkaTemplate.flush();
            log.debug("Kafka Event Similarity Producer flushed on shutdown");
        } catch (Exception e) {
            log.warn("Error flushing Event Similarity Producer on shutdown", e);
        }
    }

    public void send(long eventA, long eventB, double score, Instant timestamp) {
        EventSimilarityAvro avro = new EventSimilarityAvro(eventA, eventB, score, timestamp);
        byte[] payload = encode(avro);
        String key = eventA + "_" + eventB;
        kafkaTemplate.send(eventsSimilarityTopic, key, payload);
        log.debug("Sent EventSimilarityAvro to topic {}: eventA={}, eventB={}, score={}", eventsSimilarityTopic, eventA, eventB, score);
    }

    private byte[] encode(EventSimilarityAvro avro) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(out, null);
        SpecificDatumWriter<EventSimilarityAvro> writer = new SpecificDatumWriter<>(EventSimilarityAvro.class);
        try {
            writer.write(avro, encoder);
            encoder.flush();
        } catch (IOException e) {
            log.error("Failed to encode EventSimilarityAvro: eventA={}, eventB={}, score={}, timestamp={}",
                    avro.getEventA(), avro.getEventB(), avro.getScore(), avro.getTimestamp(), e);
            throw new IllegalStateException("Failed to encode EventSimilarityAvro", e);
        }
        return out.toByteArray();
    }
}
