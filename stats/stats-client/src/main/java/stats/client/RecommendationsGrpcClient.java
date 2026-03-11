package stats.client;

import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.proto.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Slf4j
@Component
public class RecommendationsGrpcClient {

    @GrpcClient("analyzer")
    private RecommendationsControllerGrpc.RecommendationsControllerBlockingStub blockingStub;

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
}

