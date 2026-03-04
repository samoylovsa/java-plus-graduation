package ewm.request.client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CountConfirmedRequestsByEventId {

    private Long eventId;
    private int countConfirmedRequests;

    public CountConfirmedRequestsByEventId(Long eventId, Long countConfirmedRequestsLong) {
        this.eventId = eventId;
        this.countConfirmedRequests = (countConfirmedRequestsLong == null) ? 0 : countConfirmedRequestsLong.intValue();
    }
}

