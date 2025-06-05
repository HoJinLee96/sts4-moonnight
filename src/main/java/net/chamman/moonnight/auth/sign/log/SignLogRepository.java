package net.chamman.moonnight.auth.sign.log;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.transaction.Transactional;
import net.chamman.moonnight.auth.sign.log.SignLog.SignResult;
import net.chamman.moonnight.domain.user.User.UserProvider;

@Repository
public interface SignLogRepository extends JpaRepository<SignLog, Integer> {
	
	@Query("SELECT COUNT(l) FROM SignLog l WHERE l.requestId = :requestId AND l.userProvider = :userProvider AND l.resolveBy IS NULL AND l.signResult NOT IN :excludedResults")
	int countUnresolvedFailed(
			@Param("userProvider") UserProvider userProvider, 
			@Param("requestId") String requestId, 
			@Param("excludedResults") List<SignResult> excludedResults);
	
	@Query("SELECT COUNT(l) FROM SignLog l WHERE l.requestId = :requestId AND l.userProvider = :userProvider AND l.resolveBy IS NULL AND l.signResult IN :includedResults")
	int countUnresolvedWithResults(
			@Param("userProvider") UserProvider userProvider, 
			@Param("requestId") String requestId, 
			@Param("includedResults") List<SignResult> includedResults);
	
	@Transactional @Modifying
	@Query("UPDATE SignLog l SET l.resolveBy = :signLogId WHERE l.userProvider = :userProvider AND l.requestId = :requestId AND l.resolveBy IS NULL")
	int resolveUnresolvedLogs(
			@Param("userProvider") UserProvider userProvider,
			@Param("requestId") String requestId,
			@Param("signLogId") int signLogId
			);
}
