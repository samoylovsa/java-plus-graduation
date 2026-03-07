package ewm.comment.dto;

import ewm.comment.model.CommentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentStatusUpdateDto {

    private CommentStatus status;
}

