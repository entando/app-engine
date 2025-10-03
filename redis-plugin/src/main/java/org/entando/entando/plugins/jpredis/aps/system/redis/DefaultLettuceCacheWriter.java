/*
 * Copyright 2020-Present Entando Inc. (http://www.entando.com) All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */
package org.entando.entando.plugins.jpredis.aps.system.redis;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.data.redis.cache.CacheStatistics;
import org.springframework.data.redis.cache.CacheStatisticsCollector;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStringCommands.SetOption;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link RedisCacheWriter} Copy (for custom redis connector) of implementation capable of reading/writing binary data 
 * from/to Redis in {@literal standalone} and {@literal cluster} environments. 
 * This copy is necessary to create a custom redis manager {@link LettuceCacheManager} that allows to create 
 * a custom Cache (LettuceCache) with Client-side caching support (provided by CacheFrontend instance).
 * Works upon a given {@link RedisConnectionFactory} to obtain the actual {@link RedisConnection}. <br />
 * {@link RedisCacheWriter} can be used in
 * {@link RedisCacheWriter#lockingRedisCacheWriter(RedisConnectionFactory) locking} or
 * {@link RedisCacheWriter#nonLockingRedisCacheWriter(RedisConnectionFactory) non-locking} mode. While
 * {@literal non-locking} aims for maximum performance it may result in overlapping, non atomic, command execution for
 * operations spanning multiple Redis interactions like {@code putIfAbsent}. The {@literal locking} counterpart prevents
 * command overlap by setting an explicit lock key and checking against presence of this key which leads to additional
 * requests and potential command wait times.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @author André Prata
 * @since 2.0
 */
class DefaultLettuceCacheWriter implements RedisCacheWriter {

	private final RedisConnectionFactory connectionFactory;
	private final Duration sleepTime;
	private final RedisCacheWriter delegate;

	/**
	 * @param connectionFactory must not be {@literal null}.
	 */
	DefaultLettuceCacheWriter(RedisConnectionFactory connectionFactory) {
		this(connectionFactory, Duration.ZERO);
	}

	/**
	 * @param connectionFactory must not be {@literal null}.
	 * @param sleepTime sleep time between lock request attempts. Must not be {@literal null}. Use {@link Duration#ZERO}
	 *          to disable locking.
	 */
	DefaultLettuceCacheWriter(RedisConnectionFactory connectionFactory, Duration sleepTime) {
		Assert.notNull(connectionFactory, "ConnectionFactory must not be null!");
		Assert.notNull(sleepTime, "SleepTime must not be null!");
		this.connectionFactory = connectionFactory;
		this.sleepTime = sleepTime;
		this.delegate = RedisCacheWriter.lockingRedisCacheWriter(connectionFactory);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.cache.RedisCacheWriter#put(java.lang.String, byte[], byte[], java.time.Duration)
	 */
	@Override
	public void put(String name, byte[] key, byte[] value, @Nullable Duration ttl) {
		Assert.notNull(name, "Name must not be null!");
		Assert.notNull(key, "Key must not be null!");
		Assert.notNull(value, "Value must not be null!");
		delegate.put(name, key, value, ttl);
//		execute(name, connection -> {
//			if (shouldExpireWithin(ttl)) {
//				connection.set(key, value, Expiration.from(ttl.toMillis(), TimeUnit.MILLISECONDS), SetOption.upsert());
//			} else {
//				connection.set(key, value);
//			}
//			return "OK";
//		});
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.cache.RedisCacheWriter#get(java.lang.String, byte[])
	 */
	@Override
	public byte[] get(String name, byte[] key) {
		Assert.notNull(name, "Name must not be null!");
		Assert.notNull(key, "Key must not be null!");
//		return execute(name, connection -> connection.get(key));
		return delegate.get(name, key);
	}

	// --- Asynchronous methods: Implementing store and retrieve ---

	@Override
	public CompletableFuture<Void> store(String name, byte[] key, byte[] value, @Nullable Duration ttl) {
		Assert.notNull(name, "Name must not be null!");
		Assert.notNull(key, "Key must not be null!");
		Assert.notNull(value, "Value must not be null!");

		// Delegate the asynchronous storage operation
		return delegate.store(name, key, value, ttl);
	}

	@Override
	public CompletableFuture<byte[]> retrieve(String name, byte[] key) {
		Assert.notNull(name, "Name must not be null!");
		Assert.notNull(key, "Key must not be null!");

		// Delegate the asynchronous retrieval operation
		return delegate.retrieve(name, key);
	}

    // --- Important: You should override the TTL-enabled retrieve as well ---
	@Override
	public CompletableFuture<byte[]> retrieve(String name, byte[] key, @Nullable Duration ttl) {
		Assert.notNull(name, "Name must not be null!");
		Assert.notNull(key, "Key must not be null!");

        // Delegate the asynchronous retrieval operation with TTL
		return delegate.retrieve(name, key, ttl);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.cache.RedisCacheWriter#putIfAbsent(java.lang.String, byte[], byte[], java.time.Duration)
	 */
	@Override
	public byte[] putIfAbsent(String name, byte[] key, byte[] value, @Nullable Duration ttl) {
		Assert.notNull(name, "Name must not be null!");
		Assert.notNull(key, "Key must not be null!");
		Assert.notNull(value, "Value must not be null!");
//		return execute(name, connection -> {
//			if (isLockingCacheWriter()) {
//				doLock(name, connection);
//			}
//			try {
//				if (connection.setNX(key, value)) {
//					if (shouldExpireWithin(ttl)) {
//						connection.pExpire(key, ttl.toMillis());
//					}
//					return null;
//				}
//				return connection.get(key);
//			} finally {
//				if (isLockingCacheWriter()) {
//					doUnlock(name, connection);
//				}
//			}
//		});
		return delegate.putIfAbsent(name, key, value, ttl);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.cache.RedisCacheWriter#remove(java.lang.String, byte[])
	 */
	@Override
	public void remove(String name, byte[] key) {
		Assert.notNull(name, "Name must not be null!");
		Assert.notNull(key, "Key must not be null!");
//		execute(name, connection -> connection.del(key));
		delegate.remove(name, key);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.redis.cache.RedisCacheWriter#clean(java.lang.String, byte[])
	 */
	@Override
	public void clean(String name, byte[] pattern) {
		Assert.notNull(name, "Name must not be null!");
		Assert.notNull(pattern, "Pattern must not be null!");
//		execute(name, connection -> {
//			boolean wasLocked = false;
//			try {
//				if (isLockingCacheWriter()) {
//					doLock(name, connection);
//					wasLocked = true;
//				}
//				byte[][] keys = Optional.ofNullable(connection.keys(pattern)).orElse(Collections.emptySet())
//						.toArray(new byte[0][]);
//				if (keys.length > 0) {
//					connection.del(keys);
//				}
//			} finally {
//				if (wasLocked && isLockingCacheWriter()) {
//					doUnlock(name, connection);
//				}
//			}
//			return "OK";
//		});
		delegate.clean(name, pattern);
	}
//
//	/**
//	 * Explicitly set a write lock on a cache.
//	 *
//	 * @param name the name of the cache to lock.
//	 */
//	void lock(String name) {
//		execute(name, connection -> doLock(name, connection));
//	}
//
//	/**
//	 * Explicitly remove a write lock from a cache.
//	 *
//	 * @param name the name of the cache to unlock.
//	 */
//	void unlock(String name) {
//		executeLockFree(connection -> doUnlock(name, connection));
//	}
//
//	private Boolean doLock(String name, RedisConnection connection) {
//		return connection.setNX(createCacheLockKey(name), new byte[0]);
//	}
//
//	private Long doUnlock(String name, RedisConnection connection) {
//		return connection.del(createCacheLockKey(name));
//	}
//
//	boolean doCheckLock(String name, RedisConnection connection) {
//		return connection.exists(createCacheLockKey(name));
//	}
//
//	/**
//	 * @return {@literal true} if {@link RedisCacheWriter} uses locks.
//	 */
//	private boolean isLockingCacheWriter() {
//		return !sleepTime.isZero() && !sleepTime.isNegative();
//	}
//
//	private <T> T execute(String name, Function<RedisConnection, T> callback) {
//		RedisConnection connection = connectionFactory.getConnection();
//		try {
//			checkAndPotentiallyWaitUntilUnlocked(name, connection);
//			return callback.apply(connection);
//		} finally {
//			connection.close();
//		}
//	}
//
//	private void executeLockFree(Consumer<RedisConnection> callback) {
//		RedisConnection connection = connectionFactory.getConnection();
//		try {
//			callback.accept(connection);
//		} finally {
//			connection.close();
//		}
//	}
//
//	private void checkAndPotentiallyWaitUntilUnlocked(String name, RedisConnection connection) {
//		if (!isLockingCacheWriter()) {
//			return;
//		}
//		try {
//			while (doCheckLock(name, connection)) {
//				Thread.sleep(sleepTime.toMillis());
//			}
//		} catch (InterruptedException ex) {
//			// Re-interrupt current thread, to allow other participants to react.
//			Thread.currentThread().interrupt();
//			throw new PessimisticLockingFailureException(String.format("Interrupted while waiting to unlock cache %s", name), ex);
//		}
//	}
//
//	private static boolean shouldExpireWithin(@Nullable Duration ttl) {
//		return ttl != null && !ttl.isZero() && !ttl.isNegative();
//	}
//
//	private static byte[] createCacheLockKey(String name) {
//		return (name + "~lock").getBytes(StandardCharsets.UTF_8);
//	}

	// ESB-678: Added withStatisticsCollector method for Spring Data Redis compatibility
	// Spring Data Redis 2.5.12+ requires RedisCacheWriter implementations to support
	// cache statistics collection. This method returns 'this' to maintain the current
	// instance while indicating statistics collection capability is available.
	// References:
	// - https://docs.spring.io/spring-data/redis/docs/current/api/org/springframework/data/redis/cache/RedisCacheWriter.html
	// - Spring Data Redis 2.5.12 API documentation
	@Override
	public RedisCacheWriter withStatisticsCollector(CacheStatisticsCollector cacheStatisticsCollector) {
		return this;
	}

	// ESB-678: Added clearStatistics method for Spring Data Redis cache statistics support
	// This method provides a no-op implementation for clearing cache statistics as required
	// by the RedisCacheWriter interface in Spring Data Redis 2.5.12+. Custom implementations
	// can override this to provide actual statistics clearing functionality if needed.
	// References:
	// - org.springframework.data.redis.cache.RedisCacheWriter interface
	// - Spring Data Redis cache statistics documentation
	@Override
	public void clearStatistics(String name) {
		// No-op implementation for statistics clearing
	}

	// ESB-678: Added getCacheStatistics method for Spring Data Redis statistics interface
	// Required by CacheStatisticsProvider interface in Spring Data Redis 2.5.12+.
	// Returns null to indicate no statistics are currently collected by this implementation.
	// Can be enhanced to return actual CacheStatistics if monitoring is needed.
	// References:
	// - org.springframework.data.redis.cache.CacheStatisticsProvider interface  
	// - https://docs.spring.io/spring-data/redis/docs/current/api/org/springframework/data/redis/cache/CacheStatistics.html
	@Override
	public CacheStatistics getCacheStatistics(String cacheName) {
		return null;
	}
    
}
