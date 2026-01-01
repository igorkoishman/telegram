package com.koishman.telegram.service;

import com.koishman.telegram.model.UserSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SessionManager {

    private final Map<Long, UserSession> sessions = new ConcurrentHashMap<>();

    public UserSession getOrCreateSession(Long chatId) {
        return sessions.computeIfAbsent(chatId, id -> {
            UserSession session = new UserSession();
            session.setChatId(id);
            session.setState(UserSession.SessionState.IDLE);
            return session;
        });
    }

    public UserSession getSession(Long chatId) {
        return sessions.get(chatId);
    }

    public void clearSession(Long chatId) {
        sessions.remove(chatId);
    }

    public void updateSession(Long chatId, UserSession session) {
        sessions.put(chatId, session);
    }
}
