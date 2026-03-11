package ru.practicum.ewm.stats.analyzer.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.analyzer.service.EventSimilarityUpdateService;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventSimilarityKafkaConsumer {

    private final EventSimilarityUpdateService updateService;

    @KafkaListener(
            topics = "${app.kafka.topics.events-similarity}",
            groupId = "${app.kafka.consumer.events-similarity-group-id}",
            containerFactory = "eventsSimilarityKafkaListenerContainerFactory"
    )
    public void consume(@Payload byte[] payload,
                        @Header(value = KafkaHeaders.RECEIVED_TOPIC, required = false) String topic) {
        try {
            SpecificDatumReader<EventSimilarityAvro> reader = new SpecificDatumReader<>(EventSimilarityAvro.class);
            var decoder = DecoderFactory.get().binaryDecoder(payload, null);
            EventSimilarityAvro avro = reader.read(null, decoder);
            updateService.process(avro);
        } catch (IOException e) {
            log.error("Failed to deserialize EventSimilarityAvro from topic {}: {}", topic, e.getMessage(), e);
            throw new IllegalStateException("EventSimilarityAvro deserialization failed", e);
        }
    }
}
