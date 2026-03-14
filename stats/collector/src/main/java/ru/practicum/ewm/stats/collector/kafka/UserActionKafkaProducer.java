package ru.practicum.ewm.stats.collector.kafka;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.collector.mapper.UserActionProtoToAvroMapper;
import ru.practicum.ewm.stats.proto.UserActionProto;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionKafkaProducer {

    private final KafkaTemplate<String, byte[]> kafkaTemplate;
    private final UserActionProtoToAvroMapper mapper;

    @Value("${app.kafka.topics.user-actions}")
    private String userActionsTopic;

    @PreDestroy
    public void cleanup() {
        try {
            kafkaTemplate.flush();
            log.debug("User Action Kafka Producer flushed on shutdown");
        } catch (Exception e) {
            log.warn("Error flushing User Action Kafka Producer on shutdown", e);
        }
    }

    public void send(UserActionProto proto) {
        UserActionAvro avro = mapper.toAvro(proto);
        byte[] payload = encode(avro);
        String key = String.valueOf(proto.getUserId());
        kafkaTemplate.send(userActionsTopic, key, payload);
        log.debug(
                "Sent UserActionAvro to topic {}: userId={}, eventId={}, actionType={}",
                userActionsTopic,
                avro.getUserId(),
                avro.getEventId(),
                avro.getActionType()
        );
    }

    private byte[] encode(UserActionAvro avro) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(out, null);
        SpecificDatumWriter<UserActionAvro> writer = new SpecificDatumWriter<>(UserActionAvro.class);
        try {
            writer.write(avro, encoder);
            encoder.flush();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to encode UserActionAvro", e);
        }
        return out.toByteArray();
    }
}