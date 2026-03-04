package ewm.request.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStatusRequestDtoResp {

    private List<UserRequestDto> confirmedRequests;
    private List<UserRequestDto> rejectedRequests;
}

