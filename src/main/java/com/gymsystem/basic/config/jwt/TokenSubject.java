package com.gymsystem.basic.config.jwt;

import java.util.List;

/** The data that goes inside a token (built once at login / refresh). */
public record TokenSubject(Long staffId, String email, String role, Long branchId,
                           Long sessionId, List<String> permissions) {
}
