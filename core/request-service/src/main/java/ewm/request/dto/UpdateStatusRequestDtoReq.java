package ewm.request.dto;

import ewm.request.model.RequestStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStatusRequestDtoReq {

    private List<Long> requestIds;
    private RequestStatus status;
}

