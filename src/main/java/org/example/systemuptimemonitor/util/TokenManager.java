package org.example.systemuptimemonitor.util;

import org.example.systemuptimemonitor.model.User;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TokenManager {
    private static final ConcurrentHashMap<String, User> tokens = new ConcurrentHashMap<>();

    public static String createToken(User user) {
        String token = UUID.randomUUID().toString();
        user.setLoggedIn(System.currentTimeMillis());
        tokens.putIfAbsent(token, user);
        return token;
    }

    public static void removeToken(String token) {
        tokens.remove(token);
    }

    public static boolean isValid(String token) {
        if (tokens.containsKey(token)) {
            User user = tokens.get(token);
            long loggedIn = user.getLoggedIn();
            if (loggedIn + (1_000 * 3_600) > System.currentTimeMillis()) {
                return true;
            }
        }
        removeToken(token);
        return false;
    }

    public static User getUser(String token) {
        return tokens.get(token);
    }
}
