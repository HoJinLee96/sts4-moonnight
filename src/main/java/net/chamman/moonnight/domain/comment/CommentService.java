package net.chamman.moonnight.domain.comment;

import static net.chamman.moonnight.global.exception.HttpStatusCode.AUTHORIZATION_FAILED;
import static net.chamman.moonnight.global.exception.HttpStatusCode.COMMENT_STATUS_DELETE;
import static net.chamman.moonnight.global.exception.HttpStatusCode.COMMENT_NOT_FOUND;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.auth.crypto.Obfuscator;
import net.chamman.moonnight.domain.comment.Comment.CommentStatus;
import net.chamman.moonnight.domain.estimate.Estimate;
import net.chamman.moonnight.domain.estimate.EstimateService;
import net.chamman.moonnight.domain.user.User;
import net.chamman.moonnight.domain.user.UserService;
import net.chamman.moonnight.global.exception.ForbiddenException;
import net.chamman.moonnight.global.exception.NoSuchDataException;
import net.chamman.moonnight.global.exception.StatusDeleteException;
import net.chamman.moonnight.global.exception.StatusStayException;
import net.chamman.moonnight.global.exception.StatusStopException;

@Service
@Slf4j
@RequiredArgsConstructor
public class CommentService {
	
	private final CommentRepository commentRepository;
	private final UserService userService;
	private final EstimateService estimateService;
	private final Obfuscator obfuscator;
	
	/** 댓글 등록
	 * @param userProvider
	 * @param email
	 * @param commentRequestDto
	 * 
	 * @throws NoSuchDataException {@link UserService#getUserByUserId}, {@link EstimateService#getEstimateById} 찾을 수 없는 데이터
	 * @throws StatusStayException {@link UserService#getUserByUserId} 일시정지 유저
	 * @throws StatusStopException {@link UserService#getUserByUserId} 중지 유저
	 * @throws StatusDeleteException {@link UserService#getUserByUserId} 탈퇴 유저
	 * 
	 * @return 댓글
	 */
	@Transactional
	public CommentResponseDto registerComment(int userId, CommentRequestDto commentRequestDto) {
		
		User user = userService.getUserByUserId(userId);
		
		Estimate estimate = estimateService.getEstimateById(commentRequestDto.encodedEstimateId());
		
		Comment comment = commentRequestDto.toEntity(user, estimate);
		commentRepository.save(comment);
		
		return CommentResponseDto.fromEntity(comment, user.getUserId(), obfuscator);
	}
	
	/** 견적서의 댓글 리스트 조회
	 * @param encodedEstimateId
	 * @param userId
	 * 
	 * @throws NoSuchDataException {@link UserService#getUserByUserId} 찾을 수 없는 데이터
	 * @throws StatusStayException {@link UserService#getUserByUserId} 일시정지 유저
	 * @throws StatusStopException {@link UserService#getUserByUserId} 중지 유저
	 * @throws StatusDeleteException {@link UserService#getUserByUserId} 탈퇴 유저
	 * 
	 * @return 댓글 리스트
	 */
	public List<CommentResponseDto> getCommentList(int encodedEstimateId, int userId) {
		
		User user = userService.getUserByUserId(userId);
		
		List<Comment> list = commentRepository.findByEstimate_EstimateId(obfuscator.decode(encodedEstimateId));
		
		if(list==null || list.isEmpty() || list.size()==0) {
			return null;
		}
		
		return list.stream()
				.filter(e->e.getCommentStatus()!=CommentStatus.DELETE)
				.map(comment -> CommentResponseDto.fromEntity(comment, user.getUserId(), obfuscator))
				.collect(Collectors.toList());
	}
	
	/** 댓글 수정
	 * @param userId
	 * @param encodedCommentId
	 * @param commentRequestDto
	 * 
	 * @throws NoSuchDataException {@link #getAuthorizedComment} 찾을 수 없는 데이터
	 * @throws StatusDeleteException {@link #getAuthorizedComment} 이미 삭제된 댓글
	 * @throws ForbiddenException {@link #getAuthorizedComment} 댓글 권한 없음
	 */
	@Transactional
	public void updateComment(int userId, int encodedCommentId, CommentRequestDto commentRequestDto) {
		
		Comment comment = getAuthorizedComment(userId, encodedCommentId);
		comment.setCommentText(commentRequestDto.commentText());
	}
	
	/**
	 * @param userId
	 * @param encodedCommentId
	 * 
	 * @throws NoSuchDataException {@link #getAuthorizedComment} 찾을 수 없는 데이터
	 * @throws StatusDeleteException {@link #getAuthorizedComment} 이미 삭제된 댓글
	 * @throws ForbiddenException {@link #getAuthorizedComment} 댓글 권한 없음
	 */
	@Transactional
	public void deleteComment(int userId, int encodedCommentId) {
		
		Comment comment = getAuthorizedComment(userId, encodedCommentId);
		comment.setCommentStatus(CommentStatus.DELETE);
	}
	
	/** 댓글 Get
	 * @param userId
	 * @param encodedCommentId
	 * @throws NoSuchDataException {@link #getAuthorizedComment} 찾을 수 없는 데이터
	 * @throws StatusDeleteException {@link #getAuthorizedComment} 이미 삭제된 댓글
	 * @throws ForbiddenException {@link #getAuthorizedComment} 댓글 권한 없음
	 * @return 댓글
	 */
	private Comment getAuthorizedComment(int userId, int encodedCommentId) {
		
		int commentId = obfuscator.decode(encodedCommentId);
		
		userService.getUserByUserId(userId);

		Comment comment = commentRepository.findById(commentId)
				.orElseThrow(() -> new NoSuchDataException(COMMENT_NOT_FOUND,"일치하는 데이터 없음. encodedCommentId: " + encodedCommentId));
		
		if (comment.getCommentStatus() == Comment.CommentStatus.DELETE) {
			throw new StatusDeleteException(COMMENT_STATUS_DELETE,"이미 삭제된 댓글.");
		}
		
		if (comment.getUser().getUserId() != userId) {
			throw new ForbiddenException(AUTHORIZATION_FAILED,"댓글 조회 권한 이상. address.getUser().getUserId(): "+comment.getUser().getUserId()+"!= userId: "+userId);
		}
		return comment;
	}

}
