package com.tickettracker.dao;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.tickettracker.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class UserCache {

    private static final Logger logger = LoggerFactory.getLogger(UserCache.class);

    private final Cache<String, User> userByIdCache;
    private final Cache<String, User> userByEmailCache;

    public UserCache() {
        this.userByIdCache = Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats()
                .build();

        this.userByEmailCache = Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats()
                .build();

        logger.info("UserCache initialized (maxSize=500, TTL=5min)");
    }

    private String idKey(byte[] id) {
        return java.util.Base64.getEncoder().encodeToString(id);
    }

    public User getById(byte[] id) {
        return userByIdCache.getIfPresent(idKey(id));
    }

    public User getByEmail(String email) {
        return userByEmailCache.getIfPresent(email != null ? email.toLowerCase() : null);
    }

    public void put(User user) {
        if (user == null) return;
        if (user.getId() != null) {
            userByIdCache.put(idKey(user.getId()), user);
        }
        if (user.getEmail() != null) {
            userByEmailCache.put(user.getEmail().toLowerCase(), user);
        }
    }

    public void invalidate(User user) {
        if (user == null) return;
        if (user.getId() != null) {
            userByIdCache.invalidate(idKey(user.getId()));
        }
        if (user.getEmail() != null) {
            userByEmailCache.invalidate(user.getEmail().toLowerCase());
        }
    }

    public void invalidateById(byte[] id) {
        if (id != null) {
            userByIdCache.invalidate(idKey(id));
        }
    }

    public void invalidateByEmail(String email) {
        if (email != null) {
            userByEmailCache.invalidate(email.toLowerCase());
        }
    }

    public void invalidateAll() {
        userByIdCache.invalidateAll();
        userByEmailCache.invalidateAll();
        logger.info("UserCache fully invalidated");
    }

    public String getStats() {
        return String.format("UserCache Stats - byId: %s, byEmail: %s",
                userByIdCache.stats().toString(),
                userByEmailCache.stats().toString());
    }
}
