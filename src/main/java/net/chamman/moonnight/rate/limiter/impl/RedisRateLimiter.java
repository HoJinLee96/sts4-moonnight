package net.chamman.moonnight.rate.limiter.impl;

import static net.chamman.moonnight.global.exception.HttpStatusCode.TOO_MANY_REQUEST;

import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import net.chamman.moonnight.global.exception.TooManyRequestsException;
import net.chamman.moonnight.rate.limiter.RateLimiter;

@Component
@RequiredArgsConstructor
public class RedisRateLimiter implements RateLimiter {

	private final RedisTemplate<String, String> redisTemplate;

	private final long timeoutMinutes = 30;

	@Override
	public boolean isAllowed(String key) {
		return isAllowed(key, 20);
	}

	public boolean isAllowed(String key, int maxCount) {
		ValueOperations<String, String> ops = redisTemplate.opsForValue();
		Long reqCount = ops.increment(key, 1);

		if (reqCount == 1) {
			redisTemplate.expire(key, timeoutMinutes, TimeUnit.MINUTES);
		}

		if (reqCount > maxCount) {
			throw new TooManyRequestsException(TOO_MANY_REQUEST, "요청 횟수를 초과.");
		}

		return true;
	}

}
