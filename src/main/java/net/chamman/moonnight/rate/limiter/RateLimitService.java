package net.chamman.moonnight.rate.limiter;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import net.chamman.moonnight.rate.limiter.impl.RedisRateLimiter;

@Component
@RequiredArgsConstructor
public class RateLimitService {

    private final RedisRateLimiter redisRateLimiter;

    public void checkPhoneVerify(String phone) {
        redisRateLimiter.isAllowed(
            RateLimitKeyGenerator.VERIFY_PHONE.key(phone),
            RateLimitKeyGenerator.VERIFY_PHONE.getMaxRequest()
        );
    }

    public void checkEmailVerify(String email) {
        redisRateLimiter.isAllowed(
            RateLimitKeyGenerator.VERIFY_EMAIL.key(email),
            RateLimitKeyGenerator.VERIFY_EMAIL.getMaxRequest()
        );
    }

    public void checkEstimateByIp(String ip) {
        redisRateLimiter.isAllowed(
            RateLimitKeyGenerator.IP.key(ip),
            RateLimitKeyGenerator.IP.getMaxRequest()
        );
    }
}
