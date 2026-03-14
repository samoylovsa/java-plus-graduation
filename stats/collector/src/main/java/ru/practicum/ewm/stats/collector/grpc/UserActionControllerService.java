package ru.practicum.ewm.stats.collector.grpc;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.ewm.stats.collector.kafka.UserActionKafkaProducer;
import ru.practicum.ewm.stats.proto.UserActionControllerGrpc;
import ru.practicum.ewm.stats.proto.UserActionProto;

@Slf4j
@GrpcService
public class UserActionControllerService extends UserActionControllerGrpc.UserActionControllerImplBase {

    private final UserActionKafkaProducer producer;

    public UserActionControllerService(UserActionKafkaProducer producer) {
        this.producer = producer;
    }

    @Override
    public void collectUserAction(UserActionProto request, StreamObserver<Empty> responseObserver) {
        try {
            log.debug("Получено UserAction: userId={}, eventId={}, actionType={}",
                    request.getUserId(), request.getEventId(), request.getActionType());
            producer.send(request);
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Ошибка обработки UserAction: {}", e.getMessage(), e);
            responseObserver.onError(new StatusRuntimeException(
                    Status.INTERNAL
                            .withDescription(e.getLocalizedMessage())
                            .withCause(e)
            ));
        }
    }
}