package ewm.comment.service;

import ewm.comment.dto.CommentDto;
import ewm.comment.dto.CommentStatusUpdateDto;
import ewm.comment.dto.NewCommentDto;
import ewm.comment.dto.UpdateCommentDto;
import ewm.comment.model.CommentStatus;

import java.util.List;

public interface CommentService {

    CommentDto createComment(Long userId, NewCommentDto newCommentDto);

    CommentDto updateComment(Long userId, Long commentId, UpdateCommentDto updateCommentDto);

    void deleteCommentByOwner(Long userId, Long commentId);

    List<CommentDto> getOwnerComments(Long userId);

    CommentDto moderateComment(Long commentId, CommentStatusUpdateDto updateDto);

    void deleteCommentByAdmin(Long commentId);

    List<CommentDto> getCommentsForAdmin(CommentStatus status, Long eventId, Long userId);

    List<CommentDto> getPublishedEventComments(Long eventId);

    CommentDto getPublishedComment(Long commentId);
}