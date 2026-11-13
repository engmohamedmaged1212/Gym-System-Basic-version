package com.gymsystem.basic.auth;

import com.gymsystem.basic.common.PageResponse;
import com.gymsystem.basic.config.jwt.Jwt;
import com.gymsystem.basic.config.jwt.JwtServices;
import com.gymsystem.basic.config.jwt.TokenSubject;
import com.gymsystem.basic.exception.InvalidCredentialsException;
import com.gymsystem.basic.exception.NotAuthException;
import com.gymsystem.basic.exception.ResourceNotFoundException;
import com.gymsystem.basic.staff.Staff;
import com.gymsystem.basic.staff.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final StaffUserDetailsService userDetailsService;
    private final StaffLoginSessionRepository sessionRepository;
    private final StaffRepository staffRepository;
    private final JwtServices jwtServices;
    private final PasswordEncoder passwordEncoder;

    public record Tokens(Jwt access, Jwt refresh) {
    }

    /** Checks the password, opens a login session (the "log on" record) and issues both tokens. */
    @Transactional
    public Tokens login(AuthDto.LoginRequest req, String ip, String device) {
        var auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email().trim().toLowerCase(), req.password()));
        var principal = (StaffPrincipal) auth.getPrincipal();

        var session = new StaffLoginSession();
        session.setStaff(staffRepository.getReferenceById(principal.getStaffId()));
        session.setIpAddress(truncate(ip, 45));
        session.setDeviceInfo(truncate(device, 255));
        sessionRepository.save(session);

        var subject = subjectOf(principal, session.getId());
        return new Tokens(jwtServices.generateAccessTokens(subject), jwtServices.generateRefreshTokens(subject));
    }

    /** New access token from a valid refresh token. Permissions are re-read from the DB. */
    @Transactional(readOnly = true)
    public Jwt refresh(String refreshToken) {
        var jwt = jwtServices.parseToken(refreshToken);
        if (jwt == null || !jwt.isValid() || !jwt.isRefreshToken()) {
            throw new NotAuthException("Invalid or expired refresh token, please log in again");
        }
        var session = sessionRepository
                .findByIdAndStaffIdAndLogoutAtIsNull(jwt.getSessionId(), jwt.getStaffId())
                .orElseThrow(() -> new NotAuthException("Session is closed, please log in again"));
        var principal = userDetailsService.loadById(jwt.getStaffId());
        if (!principal.isEnabled()) {
            throw new NotAuthException("Your account is not active, please contact the owner");
        }
        return jwtServices.generateAccessTokens(subjectOf(principal, session.getId()));
    }

    /** The "log off" record. */
    @Transactional
    public void logout(Long sessionId, Long staffId) {
        sessionRepository.findByIdAndStaffIdAndLogoutAtIsNull(sessionId, staffId)
                .ifPresent(s -> s.setLogoutAt(LocalDateTime.now()));
    }

    @Transactional
    public void changePassword(Long staffId, AuthDto.ChangePasswordRequest req) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff member not found"));
        if (!passwordEncoder.matches(req.oldPassword(), staff.getPasswordHash())) {
            throw new InvalidCredentialsException("Old password is incorrect");
        }
        staff.setPasswordHash(passwordEncoder.encode(req.newPassword()));
    }

    @Transactional(readOnly = true)
    public PageResponse<AuthDto.SessionResponse> sessions(Long staffId, LocalDate from, LocalDate to, Pageable pageable) {
        LocalDateTime start = from == null ? null : from.atStartOfDay();
        LocalDateTime end = to == null ? null : to.plusDays(1).atStartOfDay();
        return PageResponse.of(sessionRepository.search(staffId, start, end, pageable)
                .map(AuthDto.SessionResponse::from));
    }

    private TokenSubject subjectOf(StaffPrincipal p, Long sessionId) {
        return new TokenSubject(p.getStaffId(), p.getEmail(), p.getRoleCode(), p.getBranchId(),
                sessionId, p.getPermissions());
    }

    private String truncate(String s, int max) {
        return s == null ? null : (s.length() <= max ? s : s.substring(0, max));
    }
}
