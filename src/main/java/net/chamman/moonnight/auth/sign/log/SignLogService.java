package net.chamman.moonnight.auth.sign.log;

import static net.chamman.moonnight.global.exception.HttpStatusCode.SIGNIN_FAILED_OUT;

import java.util.List;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import net.chamman.moonnight.auth.sign.log.SignLog.SignResult;
import net.chamman.moonnight.domain.user.User.UserProvider;
import net.chamman.moonnight.global.exception.sign.TooManySignFailException;

@Service
@RequiredArgsConstructor
public class SignLogService {
	
	private final SignLogRepository signLogRepository;
	
	/**
	 * @param userProvider
	 * @param email
	 * @param ip
	 * @param result
	 */
	@Transactional
	public void registerSignLog(UserProvider userProvider, String email, String ip, SignResult result) {
		signLogRepository.save(SignLog.builder()
				.userProvider(userProvider)
				.email(email)
				.requestIp(ip)
				.signResult(result)
				.build());
	}
	
	/**
	 * @param ip
	 * @param result
	 */
	@Transactional
	public void registerSignLog(String ip, SignResult result) {
		signLogRepository.save(SignLog.builder()
				.requestIp(ip)
				.signResult(result)
				.build());
	}
	
	/** 로그인 실패 횟수 검사
	 * @param userProvider
	 * @param email
	 * @throws TooManySignFailException {@link SignLogService#validSignFailCount}
	 */
	public int validSignFailCount(UserProvider userProvider, String email) {
		
		int signFailCount = signLogRepository.countUnresolvedWithResults(userProvider, email, List.of(SignResult.INVALID_EMAIL));
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
	public void signFailLogResolveByUpdatePassword(UserProvider userProvider, String email, String ip) {
		SignLog signLog = SignLog.builder()
				.userProvider(userProvider)
				.email(email)
				.requestIp(ip)
				.signResult(SignResult.UPDATE_PASSWORD)
				.build();
		signLogRepository.save(signLog);
		signLogRepository.resolveUnresolvedLogs(userProvider, email, signLog.getSignLogId());
	}
	
}
