package net.chamman.moonnight.auth.oauth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import net.chamman.moonnight.auth.oauth.OAuth.OAuthProvider;

@Repository
public interface OAuthRepository extends JpaRepository<OAuth, Integer>{
	Optional<OAuth> findByOauthProviderAndOauthProviderId(OAuthProvider oauthProvider, String id);
}
