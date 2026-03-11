package ru.practicum.ewm.stats.analyzer.grpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.ewm.stats.analyzer.service.RecommendationsQueryService;
import ru.practicum.ewm.stats.proto.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.proto.RecommendationsControllerGrpc;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;
import ru.practicum.ewm.stats.proto.SimilarEventsRequestProto;
import ru.practicum.ewm.stats.proto.UserPredictionsRequestProto;

import java.util.List;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class RecommendationsControllerService extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {

    private final RecommendationsQueryService queryService;

    @Override
    public void getRecommendationsForUser(UserPredictionsRequestProto request,
                                          StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            log.debug("GetRecommendationsForUser userId={}, maxResults={}", request.getUserId(), request.getMaxResults());
            List<RecommendationsQueryService.EventScore> results = queryService.getRecommendationsForUser(
                    request.getUserId(), (int) request.getMaxResults());
            for (RecommendationsQueryService.EventScore e : results) {
                responseObserver.onNext(toProto(e));
            }
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("GetRecommendationsForUser error: {}", e.getMessage(), e);
            responseObserver.onError(toStatusException(e));
        }
    }

    @Override
    public void getSimilarEvents(SimilarEventsRequestProto request,
                                 StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            log.debug("GetSimilarEvents eventId={}, userId={}, maxResults={}",
                    request.getEventId(), request.getUserId(), request.getMaxResults());
            List<RecommendationsQueryService.EventScore> results = queryService.getSimilarEvents(
                    request.getEventId(), request.getUserId(), (int) request.getMaxResults());
            for (RecommendationsQueryService.EventScore e : results) {
                responseObserver.onNext(toProto(e));
            }
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("GetSimilarEvents error: {}", e.getMessage(), e);
            responseObserver.onError(toStatusException(e));
        }
    }

    @Override
    public void getInteractionsCount(InteractionsCountRequestProto request,
                                     StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            log.debug("GetInteractionsCount eventIds count={}", request.getEventIdCount());
            List<RecommendationsQueryService.EventScore> results = queryService.getInteractionsCount(request.getEventIdList());
            for (RecommendationsQueryService.EventScore e : results) {
                responseObserver.onNext(toProto(e));
            }
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("GetInteractionsCount error: {}", e.getMessage(), e);
            responseObserver.onError(toStatusException(e));
        }
    }

    private static RecommendedEventProto toProto(RecommendationsQueryService.EventScore e) {
        return RecommendedEventProto.newBuilder()
                .setEventId(e.eventId())
                .setScore(e.score())
                .build();
    }

    private static StatusRuntimeException toStatusException(Throwable e) {
        return new StatusRuntimeException(
                Status.INTERNAL
                        .withDescription(e.getMessage() != null ? e.getMessage() : "Internal error")
                        .withCause(e));
    }
}
