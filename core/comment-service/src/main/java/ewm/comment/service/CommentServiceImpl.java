package ewm.comment.service;

import ewm.comment.dto.CommentDto;
import ewm.comment.dto.CommentStatusUpdateDto;
import ewm.comment.dto.NewCommentDto;
import ewm.comment.dto.UpdateCommentDto;
import ewm.comment.client.ResilientEventClient;
import ewm.comment.client.ResilientUserClient;
import ewm.common.exception.AccessDeniedException;
import ewm.common.exception.NotFoundException;
import ewm.comment.mapper.CommentMapper;
import ewm.comment.model.Comment;
import ewm.comment.model.CommentStatus;
import ewm.comment.repository.CommentRepository;
import ewm.user.client.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final ResilientUserClient userClient;
    private final ResilientEventClient eventClient;
    private final CommentMapper commentMapper;

    @Override
    public CommentDto createComment(Long userId, NewCommentDto newCommentDto) {
        UserDto user = userClient.getUserById(userId);
        eventClient.getEventById(newCommentDto.getEventId());
        Comment comment = commentMapper.toEntity(newCommentDto, userId);
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
        eventClient.getEventById(eventId);
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
        List<UserDto> users = userClient.getUsersByIds(authorIds);
        if (users == null) users = Collections.emptyList();
        var userMap = users.stream()
                .collect(Collectors.toMap(UserDto::getId, UserDto::getName));
        return comments.stream()
                .map(c -> {
                    CommentDto dto = commentMapper.toDto(c);
                    dto.setAuthorName(userMap.get(c.getAuthorId()));
                    return dto;
                })
                .collect(Collectors.toList());
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

