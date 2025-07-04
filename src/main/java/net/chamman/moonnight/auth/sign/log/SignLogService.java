package net.chamman.moonnight.auth.sign.log;

import static net.chamman.moonnight.global.exception.HttpStatusCode.SIGNIN_FAILED_OUT;

import java.util.List;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.auth.sign.log.SignLog.SignResult;
import net.chamman.moonnight.domain.user.User.UserProvider;
import net.chamman.moonnight.global.exception.sign.TooManySignFailException;

@Service
@RequiredArgsConstructor
@Slf4j
public class SignLogService {
	
	private final SignLogRepository signLogRepository;
	
	/**
	 * @param userProvider
	 * @param email
	 * @param ip
	 * @param result
	 */
	public void registerSignLog(UserProvider userProvider, String requestId, String requestIp, SignResult result) {
		try {
			signLogRepository.save(SignLog.builder()
					.userProvider(userProvider)
					.requestId(requestId)
					.requestIp(requestIp)
					.signResult(result)
					.build());
		} catch (Exception e) {
			log.error("로그인 로그 기록중 익셉션 발생.",e);
		}
	}
	
	/**
	 * @param userProvider
	 * @param email
	 * @param ip
	 * @param result
	 */
	public void registerSignLog(UserProvider userProvider, String requestId, String requestIp, SignResult result, String reason) {
		try {
			signLogRepository.save(SignLog.builder()
					.userProvider(userProvider)
					.requestId(requestId)
					.requestIp(requestIp)
					.signResult(result)
					.reason(reason)
					.build());
		} catch (Exception e) {
			log.error("로그인 로그 기록중 익셉션 발생.",e);
		}

	}
	
	/**
	 * @param ip
	 * @param result
	 */
	public void registerSignLog(String requestIp, SignResult result) {
		try {
			signLogRepository.save(SignLog.builder()
					.requestIp(requestIp)
					.signResult(result)
					.build());
		} catch (Exception e) {
			log.error("로그인 로그 기록중 익셉션 발생.",e);
		}
	}
	
	/** 로그인 실패 횟수 검사
	 * @param userProvider
	 * @param email
	 * @throws TooManySignFailException {@link SignLogService#validSignFailCount}
	 */
	public int validSignFailCount(UserProvider userProvider, String requestId) {
		
		int signFailCount = signLogRepository.countUnresolvedWithResults(userProvider, requestId, List.of(SignResult.INVALID_EMAIL));
		if (signFailCount >= 10) {
			throw new TooManySignFailException(SIGNIN_FAILED_OUT,"로그인 실패 10회 이상하였습니다. 인증을 진행해 주세요.");
		}
		return signFailCount;
	}
	
	/** 비밀번호 변경으로 Sign Fail Count 초기화
	 * @param userProvider
	 * @param email
	 * @param ip
	 */
	@Transactional
	public void signFailLogResolveByUpdatePassword(UserProvider userProvider, String requestId, String requestIp) {
		SignLog signLog = SignLog.builder()
				.userProvider(userProvider)
				.requestId(requestId)
				.requestIp(requestIp)
				.signResult(SignResult.UPDATE_PASSWORD)
				.build();
		signLogRepository.save(signLog);
		signLogRepository.resolveUnresolvedLogs(userProvider, requestId, signLog.getSignLogId());
	}
	
}
