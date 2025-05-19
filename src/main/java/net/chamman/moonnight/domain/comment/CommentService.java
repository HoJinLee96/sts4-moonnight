package net.chamman.moonnight.domain.comment;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.auth.crypto.Obfuscator;
import net.chamman.moonnight.domain.comment.Comment.CommentStatus;
import net.chamman.moonnight.domain.estimate.Estimate;
import net.chamman.moonnight.domain.estimate.EstimateService;
import net.chamman.moonnight.domain.user.User;
import net.chamman.moonnight.domain.user.User.UserProvider;
import net.chamman.moonnight.domain.user.UserService;

@Service
@Slf4j
@RequiredArgsConstructor
public class CommentService {

  private final CommentRepository commentRepository;
  private final UserService userService;
  private final EstimateService estimateService;
  private final Obfuscator obfuscator;

  @Transactional
  public CommentResponseDto registerComment(UserProvider userProvider, String email, CommentRequestDto commentRequestDto) {

    User user = userService.getUserByUserProviderAndEmail(userProvider, email);
    
    Estimate estimate = estimateService.getEstimateById(commentRequestDto.estimateId());
    
    Comment comment = commentRequestDto.toEntity(user, estimate);
    
    return CommentResponseDto.fromEntity(commentRequestDto.estimateId(), commentRepository.save(comment), user.getUserId(), obfuscator);
  }

  public List<CommentResponseDto> getCommentList(int estimateId, int userId) {
    
    User user = userService.getUserByUserId(userId);
    
    List<Comment> comments = commentRepository.findByEstimate_EstimateId(obfuscator.decode(estimateId));
    
    return comments.stream()
        .filter(e->e.getCommentStatus()!=CommentStatus.DELETE)
        .map(comment -> CommentResponseDto.fromEntity(estimateId, comment, user.getUserId(), obfuscator))
        .collect(Collectors.toList());
  }

  @Transactional
  public CommentResponseDto updateComment(UserProvider userProvider, String email, int estimateId, int commentId, String newText) {
    
    User user = userService.getUserByUserProviderAndEmail(userProvider, email);
    
    Comment comment = getAuthorizedComment(userProvider, email, obfuscator.decode(commentId));
    comment.setCommentText(newText);
    commentRepository.flush();  
    
    Comment updatedComment = commentRepository.getReferenceById(comment.getCommentId());

    return CommentResponseDto.fromEntity(estimateId, updatedComment, user.getUserId(), obfuscator);
  }

  @Transactional
  public void deleteComment(UserProvider userProvider, String email, int commentId) {
      Comment comment = getAuthorizedComment(userProvider, email, obfuscator.decode(commentId));
      comment.setCommentStatus(CommentStatus.DELETE);
  }
  
  private Comment getAuthorizedComment(UserProvider userProvider, String email, int commentId) {
      Comment comment = commentRepository.findById(commentId)
          .orElseThrow(() -> new NoSuchElementException("찾을 수 없는 댓글. commentId: " + commentId));

      if (comment.getCommentStatus() == Comment.CommentStatus.DELETE) {
        log.warn("삭제된 댓글을 수정 시도. commentId: {}", commentId);
        throw new AccessDeniedException("삭제된 댓글을 수정 시도.");
      }

      if (!comment.getUser().getEmail().equals(email) || !comment.getUser().getUserProvider().equals(userProvider)) {
        log.warn("댓글 권한 없음. commentId: {} by userProvider: {}, email:{}", commentId, userProvider, email);
        throw new AccessDeniedException("댓글 권한 없음.");
      }
      return comment;
  }

}
