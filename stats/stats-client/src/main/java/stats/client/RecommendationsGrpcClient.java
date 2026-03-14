package stats.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.proto.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Slf4j
@Component
public class RecommendationsGrpcClient {

    private final ManagedChannel channel;
    private final RecommendationsControllerGrpc.RecommendationsControllerBlockingStub blockingStub;

    public RecommendationsGrpcClient(
            @Value("${stats.grpc.recommendations.host:localhost}") String host,
            @Value("${stats.grpc.recommendations.port:9090}") int port
    ) {
        this.channel = ManagedChannelBuilder
                .forAddress(host, port)
                .usePlaintext()
                .build();

        this.blockingStub = RecommendationsControllerGrpc.newBlockingStub(channel);
    }

    public List<RecommendedEventProto> getRecommendationsForUser(long userId, long maxResults) {
        UserPredictionsRequestProto request = UserPredictionsRequestProto.newBuilder()
                .setUserId(userId)
                .setMaxResults(maxResults)
                .build();

        log.info("Calling gRPC GetRecommendationsForUser: {}", request);
        Iterator<RecommendedEventProto> iterator = blockingStub.getRecommendationsForUser(request);
        return collectStream(iterator);
    }

    public List<RecommendedEventProto> getSimilarEvents(long eventId, long userId, long maxResults) {
        SimilarEventsRequestProto request = SimilarEventsRequestProto.newBuilder()
                .setEventId(eventId)
                .setUserId(userId)
                .setMaxResults(maxResults)
                .build();

        log.info("Calling gRPC GetSimilarEvents: {}", request);
        Iterator<RecommendedEventProto> iterator = blockingStub.getSimilarEvents(request);
        return collectStream(iterator);
    }

    public List<RecommendedEventProto> getInteractionsCount(List<Long> eventIds) {
        InteractionsCountRequestProto request = InteractionsCountRequestProto.newBuilder()
                .addAllEventId(eventIds)
                .build();

        log.info("Calling gRPC GetInteractionsCount: {}", request);
        Iterator<RecommendedEventProto> iterator = blockingStub.getInteractionsCount(request);
        return collectStream(iterator);
    }

    private List<RecommendedEventProto> collectStream(Iterator<RecommendedEventProto> iterator) {
        List<RecommendedEventProto> result = new ArrayList<>();
        while (iterator.hasNext()) {
            result.add(iterator.next());
        }
        return result;
    }

    @PreDestroy
    public void shutdown() {
        if (channel != null && !channel.isShutdown()) {
            channel.shutdown();
        }
    }
}

