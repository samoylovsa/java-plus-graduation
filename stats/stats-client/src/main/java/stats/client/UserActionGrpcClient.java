package stats.client;

import com.google.protobuf.Timestamp;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.ewm.stats.proto.UserActionControllerGrpc;
import ru.practicum.ewm.stats.proto.UserActionProto;

import java.time.Instant;

@Slf4j
@Component
public class UserActionGrpcClient {

    private final ManagedChannel channel;
    private final UserActionControllerGrpc.UserActionControllerBlockingStub blockingStub;

    public UserActionGrpcClient(
            @Value("${stats.grpc.user-action.host:localhost}") String host,
            @Value("${stats.grpc.user-action.port:9091}") int port
    ) {
        this.channel = ManagedChannelBuilder
                .forAddress(host, port)
                .usePlaintext()
                .build();

        this.blockingStub = UserActionControllerGrpc.newBlockingStub(channel);
    }

    public void collectUserAction(long userId,
                                  long eventId,
                                  ActionTypeProto actionType,
                                  Instant timestamp) {
        Timestamp protoTimestamp = Timestamp.newBuilder()
                .setSeconds(timestamp.getEpochSecond())
                .setNanos(timestamp.getNano())
                .build();

        UserActionProto request = UserActionProto.newBuilder()
                .setUserId(userId)
                .setEventId(eventId)
                .setActionType(actionType)
                .setTimestamp(protoTimestamp)
                .build();

        collectUserAction(request);
    }

    public void collectUserAction(UserActionProto request) {
        log.info("Calling gRPC CollectUserAction: {}", request);
        blockingStub.collectUserAction(request);
    }

    @PreDestroy
    public void shutdown() {
        if (channel != null && !channel.isShutdown()) {
            channel.shutdown();
        }
    }
}

