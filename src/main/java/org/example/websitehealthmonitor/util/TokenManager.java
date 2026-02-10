package org.example.websitehealthmonitor.util;

import org.example.websitehealthmonitor.model.User;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class TokenManager {
    private static final Logger LOG = Logger.getLogger(TokenManager.class.getName());
    private static final ConcurrentHashMap<String, User> tokens = new ConcurrentHashMap<>();

    public static String createToken(User user) {
        String token = UUID.randomUUID().toString();
        user.setLoggedIn(System.currentTimeMillis());
        tokens.putIfAbsent(token, user);
        LOG.fine("Token created for user: " + user.getEmail());
        return token;
    }

    public static void removeToken(String token) {
        tokens.remove(token);
        LOG.fine("Token removed");
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
