package com.gymsystem.basic.auth;

import com.gymsystem.basic.common.PageResponse;
import com.gymsystem.basic.config.jwt.JwtConfig;
import com.gymsystem.basic.config.jwt.JwtResponse;
import com.gymsystem.basic.config.security.AuthenticatedUser;
import com.gymsystem.basic.config.security.Authz;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private static final String REFRESH_COOKIE = "refreshToken";
    private static final String COOKIE_PATH = "/api/v1/auth";

    private final AuthService authService;
    private final JwtConfig jwtConfig;

    /**
     * POST /api/v1/auth/login        (public)
     * Opens a login session, returns the access token and sets the refresh token in an HttpOnly cookie.
     *
     * Request:
     * {
     *   "email": "owner@gym.local",
     *   "password": "ChangeMe123"
     * }
     *
     * Response 200:
     * { "token": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIxIiwidHlwZSI6ImFjY2VzcyIs..." }
     * Set-Cookie: refreshToken=...; HttpOnly; Path=/api/v1/auth
     *
     * Response 401:  { "error": "Invalid email or password" }
     * Response 401:  { "error": "Your account is not active, please contact the owner" }
     * Response 400:  { "email": "Email is not valid" }
     */
    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@RequestBody @Valid AuthDto.LoginRequest req,
                                             HttpServletRequest request, HttpServletResponse response) {
        var tokens = authService.login(req, request.getRemoteAddr(), request.getHeader("User-Agent"));
        setRefreshCookie(response, tokens.refresh().toString(), (int) jwtConfig.getRefreshTokenExpiration());
        return ResponseEntity.ok(new JwtResponse(tokens.access().toString()));
    }

    /**
     * POST /api/v1/auth/refresh        (public - needs the refreshToken cookie)
     *
     * Request: no body
     * Response 200: { "token": "a new access token" }
     * Response 401: { "error": "Invalid or expired refresh token, please log in again" }
     * Response 401: { "error": "Session is closed, please log in again" }
     */
    @PostMapping("/refresh")
    public ResponseEntity<JwtResponse> refresh(@CookieValue(value = REFRESH_COOKIE) String refreshToken) {
        return ResponseEntity.ok(new JwtResponse(authService.refresh(refreshToken).toString()));
    }

    /**
     * POST /api/v1/auth/logout        (any logged-in staff)
     * Closes the current login session (the "log off" record) and removes the cookie.
     *
     * Request: no body, header  Authorization: Bearer <token>
     * Response 204: (empty)
     */
    @PostMapping("/logout")
    @PreAuthorize(Authz.AUTHENTICATED)
    public ResponseEntity<Void> logout(@AuthenticationPrincipal AuthenticatedUser actor, HttpServletResponse response) {
        authService.logout(actor.sessionId(), actor.staffId());
        setRefreshCookie(response, "", 0);
        return ResponseEntity.noContent().build();
    }

    /**
     * PUT /api/v1/auth/password        (any logged-in staff)
     *
     * Request:
     * { "oldPassword": "ChangeMe123", "newPassword": "Strong@12345" }
     *
     * Response 204: (empty)
     * Response 401: { "error": "Old password is incorrect" }
     * Response 400: { "newPassword": "Password must be 8-72 characters" }
     */
    @PutMapping("/password")
    @PreAuthorize(Authz.AUTHENTICATED)
    public ResponseEntity<Void> changePassword(@RequestBody @Valid AuthDto.ChangePasswordRequest req,
                                               @AuthenticationPrincipal AuthenticatedUser actor) {
        authService.changePassword(actor.staffId(), req);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/v1/auth/sessions?staffId=5&from=2026-11-01&to=2026-11-30&page=0&size=20   (permission: STAFF_MANAGE)
     * Login / logout history of the employees (logoutAt = null means still logged in).
     *
     * Response 200:
     * {
     *   "content": [ { "id": 41, "staffId": 5, "staffName": "Ahmed Hassan",
     *                  "loginAt": "2026-11-13T07:58:10", "logoutAt": "2026-11-13T16:02:44",
     *                  "ipAddress": "192.168.1.20", "deviceInfo": "Mozilla/5.0 ..." } ],
     *   "page": 0, "size": 20, "totalElements": 1, "totalPages": 1
     * }
     */
    @GetMapping("/sessions")
    @PreAuthorize(Authz.STAFF_MANAGE)
    public PageResponse<AuthDto.SessionResponse> sessions(
            @RequestParam(required = false) Long staffId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "loginAt", direction = org.springframework.data.domain.Sort.Direction.DESC)
            Pageable pageable) {
        return authService.sessions(staffId, from, to, pageable);
    }

    private void setRefreshCookie(HttpServletResponse response, String value, int maxAgeSeconds) {
        var cookie = new Cookie(REFRESH_COOKIE, value);
        cookie.setHttpOnly(true);
        cookie.setPath(COOKIE_PATH);
        cookie.setMaxAge(maxAgeSeconds);
        cookie.setSecure(jwtConfig.isCookieSecure());
        response.addCookie(cookie);
    }
}
