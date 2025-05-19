package net.chamman.moonnight.domain.comment;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;
import net.chamman.moonnight.domain.user.User.UserProvider;
import net.chamman.moonnight.global.annotation.ValidId;
import net.chamman.moonnight.global.security.principal.CustomUserDetails;
import net.chamman.moonnight.global.util.ApiResponseUtil;

@RestController
@RequestMapping("/api/comment")
@RequiredArgsConstructor
public class CommentController {

  private final CommentService commentService;
  
//  댓글 등록
  @PreAuthorize("hasRole('OAUTH') or hasRole('LOCAL')")
  @PostMapping("/private/register")
  public ResponseEntity<ApiResponseUtil<CommentResponseDto>> registerComment(
      @AuthenticationPrincipal CustomUserDetails userDetails, 
      @RequestBody CommentRequestDto commentRequestDto) {
    
    String email = userDetails.getEmail();
    UserProvider userProvider = userDetails.getUserProvider();
    
    CommentResponseDto commentResponseDto = commentService.registerComment(userProvider, email, commentRequestDto);
    
    return ResponseEntity.ok(ApiResponseUtil.of(200, "댓글 목록 조회 성공", commentResponseDto));
  }
  
//  특정 견적의 댓글 목록 조회 
  @PreAuthorize("hasRole('OAUTH') or hasRole('LOCAL')")
  @GetMapping("/private/estimate/{estimateId}")
  public ResponseEntity<ApiResponseUtil<List<CommentResponseDto>>> getCommentList(
      @AuthenticationPrincipal CustomUserDetails userDetails, 
      @PathVariable int estimateId) {
    List<CommentResponseDto> responseDtoList = commentService.getCommentList(estimateId, userDetails.getUserId());
    return ResponseEntity.ok(ApiResponseUtil.of(200, "댓글 목록 조회 성공", responseDtoList));
  }
  
//  댓글 수정
  @PreAuthorize("hasRole('OAUTH') or hasRole('LOCAL')")
  @PutMapping("/private/{commentId}")
  public ResponseEntity<ApiResponseUtil<CommentResponseDto>> updateComment(
      @AuthenticationPrincipal CustomUserDetails userDetails, 
      @ValidId @PathVariable int commentId, 
      @RequestBody CommentRequestDto commentRequestDto) {
    
    CommentResponseDto commentResponseDto = commentService.updateComment(userDetails.getUserProvider(), userDetails.getEmail(), commentRequestDto.estimateId(), commentId, commentRequestDto.commentText());
    return ResponseEntity.ok(ApiResponseUtil.of(200, "댓글 수정 성공", commentResponseDto));
  }
  
//  댓글 삭제
  @PreAuthorize("hasRole('OAUTH') or hasRole('LOCAL')")
  @DeleteMapping("/private/{commentId}")
  public ResponseEntity<ApiResponseUtil<Void>> deleteComment(
      @AuthenticationPrincipal CustomUserDetails userDetails, 
      @PathVariable int commentId) {
    
    commentService.deleteComment(userDetails.getUserProvider(), userDetails.getEmail(), commentId);
    return ResponseEntity.ok(ApiResponseUtil.of(200, "댓글 삭제 성공", null));
  }
  
}
