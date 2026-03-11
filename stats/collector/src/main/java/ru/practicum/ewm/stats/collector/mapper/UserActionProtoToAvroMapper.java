package ru.practicum.ewm.stats.collector.mapper;

import com.google.protobuf.Timestamp;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.ewm.stats.proto.UserActionProto;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class UserActionProtoToAvroMapper {

    public UserActionAvro toAvro(UserActionProto proto) {
        UserActionAvro avro = new UserActionAvro();
        avro.setUserId(proto.getUserId());
        avro.setEventId(proto.getEventId());
        avro.setActionType(mapActionType(proto.getActionType()));
        avro.setTimestamp(toInstant(proto.getTimestamp()));
        return avro;
    }

    public ActionTypeAvro mapActionType(ActionTypeProto actionTypeProto) {
        return switch (actionTypeProto) {
            case ACTION_VIEW -> ActionTypeAvro.VIEW;
            case ACTION_REGISTER -> ActionTypeAvro.REGISTER;
            case ACTION_LIKE -> ActionTypeAvro.LIKE;
            default -> throw new IllegalArgumentException("Unknown action type: " + actionTypeProto);
        };
    }

    private Instant toInstant(Timestamp timestamp) {
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    }
}