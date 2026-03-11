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
import ru.practicum.ewm.stats.analyzer.service.UserActionUpdateService;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionsKafkaConsumer {

    private final UserActionUpdateService updateService;

    @KafkaListener(
            topics = "${app.kafka.topics.user-actions}",
            groupId = "${app.kafka.consumer.user-actions-group-id}",
            containerFactory = "userActionsKafkaListenerContainerFactory"
    )
    public void consume(@Payload byte[] payload,
                        @Header(value = KafkaHeaders.RECEIVED_TOPIC, required = false) String topic) {
        try {
            SpecificDatumReader<UserActionAvro> reader = new SpecificDatumReader<>(UserActionAvro.class);
            var decoder = DecoderFactory.get().binaryDecoder(payload, null);
            UserActionAvro action = reader.read(null, decoder);
            updateService.process(action);
        } catch (IOException e) {
            log.error("Failed to deserialize UserActionAvro from topic {}: {}", topic, e.getMessage(), e);
            throw new IllegalStateException("UserActionAvro deserialization failed", e);
        }
    }
}
