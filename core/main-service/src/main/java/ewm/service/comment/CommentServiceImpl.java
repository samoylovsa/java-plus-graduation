package ewm.service.comment;

import ewm.dto.comment.CommentDto;
import ewm.dto.comment.CommentStatusUpdateDto;
import ewm.dto.comment.NewCommentDto;
import ewm.dto.comment.UpdateCommentDto;
import ewm.exception.AccessDeniedException;
import ewm.exception.NotFoundException;
import ewm.mapper.comment.CommentMapper;
import ewm.model.comment.Comment;
import ewm.model.comment.CommentStatus;
import ewm.model.event.Event;
import ewm.repository.comment.CommentRepository;
import ewm.repository.event.EventRepository;
import ewm.user.client.UserClient;
import ewm.user.client.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final UserClient userClient;
    private final EventRepository eventRepository;
    private final CommentMapper commentMapper;

    @Override
    public CommentDto createComment(Long userId, NewCommentDto newCommentDto) {
        UserDto user = userClient.getUserById(userId);
        Event event = getEvent(newCommentDto.getEventId());
        Comment comment = commentMapper.toEntity(newCommentDto, event, userId);
        comment = commentRepository.save(comment);
        CommentDto commentDto = commentMapper.toDto(comment);
        commentDto.setAuthorName(user.getName());
        return commentDto;
    }

    @Override
    public CommentDto updateComment(Long userId, Long commentId, UpdateCommentDto updateCommentDto) {
        Comment comment = getComment(commentId);
        UserDto user = userClient.getUserById(userId);
        validateCommentOwner(comment, userId);
        validateCommentCanBeUpdated(comment);
        comment.setText(updateCommentDto.getText());
        comment.setUpdated(LocalDateTime.now());
        comment = commentRepository.save(comment);
        CommentDto commentDto = commentMapper.toDto(comment);
        commentDto.setAuthorName(user.getName());
        return commentDto;
    }

    @Override
    public void deleteCommentByOwner(Long userId, Long commentId) {
        Comment comment = getComment(commentId);
        validateCommentOwner(comment, userId);
        markAsDeleted(comment);
    }

    @Override
    public List<CommentDto> getOwnerComments(Long userId) {
        UserDto user = userClient.getUserById(userId);
        List<Comment> comments = commentRepository.findByAuthorId(userId);
        String authorName = user != null ? user.getName() : null;
        return comments.stream()
                .map(c -> {
                    CommentDto dto = commentMapper.toDto(c);
                    dto.setAuthorName(authorName);
                    return dto;
                })
                .toList();
    }

    @Override
    public CommentDto moderateComment(Long commentId, CommentStatusUpdateDto updateDto) {
        Comment comment = getComment(commentId);
        validateCommentCanBeModerated(comment);
        validateModerationStatus(updateDto.getStatus());
        comment.setStatus(updateDto.getStatus());
        comment.setUpdated(LocalDateTime.now());
        comment = commentRepository.save(comment);
        CommentDto dto = commentMapper.toDto(comment);
        dto.setAuthorName(userClient.getUserById(comment.getAuthorId()).getName());
        return dto;
    }

    @Override
    public void deleteCommentByAdmin(Long commentId) {
        Comment comment = getComment(commentId);
        markAsDeleted(comment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentDto> getCommentsForAdmin(CommentStatus status, Long eventId, Long userId) {
        return fillAuthorNames(commentRepository.findCommentsByFilters(status, eventId, userId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentDto> getPublishedEventComments(Long eventId) {
        getEvent(eventId);
        return fillAuthorNames(commentRepository.findByEventIdAndStatus(eventId, CommentStatus.PUBLISHED));
    }

    @Override
    @Transactional(readOnly = true)
    public CommentDto getPublishedComment(Long commentId) {
        Comment comment = commentRepository.findByIdAndStatus(commentId, CommentStatus.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Comment with id=" + commentId + " not found or not published"));
        CommentDto dto = commentMapper.toDto(comment);
        dto.setAuthorName(userClient.getUserById(comment.getAuthorId()).getName());
        return dto;
    }

    private List<CommentDto> fillAuthorNames(List<Comment> comments) {
        if (comments.isEmpty()) return List.of();
        List<Long> authorIds = comments.stream().map(Comment::getAuthorId).distinct().toList();
        List<ewm.user.client.dto.UserDto> users = userClient.getUsersByIds(authorIds);
        if (users == null) users = Collections.emptyList();
        var userMap = users.stream()
                .collect(Collectors.toMap(ewm.user.client.dto.UserDto::getId, ewm.user.client.dto.UserDto::getName));
        return comments.stream()
                .map(c -> {
                    CommentDto dto = commentMapper.toDto(c);
                    dto.setAuthorName(userMap.get(c.getAuthorId()));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private Event getEvent(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " not found"));
    }

    private Comment getComment(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment with id=" + commentId + " not found"));
    }

    private void validateCommentOwner(Comment comment, Long userId) {
        if (!comment.getAuthorId().equals(userId)) {
            throw new AccessDeniedException("User with id=" + userId + " is not the owner of comment with id=" + comment.getId());
        }
    }

    private void validateCommentCanBeUpdated(Comment comment) {
        if (comment.getStatus() != CommentStatus.PENDING) {
            throw new IllegalStateException("Can only update PENDING comments. Current status: " + comment.getStatus());
        }
    }

    private void validateCommentCanBeModerated(Comment comment) {
        if (comment.getStatus() != CommentStatus.PENDING) {
            throw new IllegalStateException("Can only moderate PENDING comments. Current status: " + comment.getStatus());
        }
    }

    private void validateModerationStatus(CommentStatus status) {
        if (status != CommentStatus.PUBLISHED && status != CommentStatus.REJECTED) {
            throw new IllegalArgumentException("Invalid status for moderation. Only PUBLISHED or REJECTED allowed. Received: " + status);
        }
    }

    private void markAsDeleted(Comment comment) {
        if (comment.getStatus() != CommentStatus.DELETED) {
            comment.setStatus(CommentStatus.DELETED);
            comment.setUpdated(LocalDateTime.now());
            commentRepository.save(comment);
        }
    }
}
