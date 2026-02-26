package com.dooring.api.auth;

import com.dooring.common.dto.ApiResponse;
import com.dooring.domain.identity.dto.LoginRequest;
import com.dooring.domain.identity.dto.LoginResult;
import com.dooring.domain.identity.dto.SellerSignupRequest;
import com.dooring.domain.identity.dto.SignupResponse;
import com.dooring.domain.identity.dto.TokenResponse;
import com.dooring.domain.identity.service.SellerAuthService;
import com.dooring.infrastructure.security.SellerPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/seller")
@RequiredArgsConstructor
public class SellerAuthController {

    private static final String RT_COOKIE = "refresh_token";
    private static final String RT_PATH   = "/api/auth/seller/refresh";

    private final SellerAuthService sellerAuthService;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpirationMs;

    @Value("${dooring.cookie.secure}")
    private boolean cookieSecure;

    /** 셀러 회원가입 */
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SignupResponse> signup(@RequestBody @Valid SellerSignupRequest request) {
        return ApiResponse.ok(sellerAuthService.signup(request));
    }

    /** 셀러 로그인 — AT: body, RT: httpOnly Cookie */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(
            @RequestBody @Valid LoginRequest request) {

        LoginResult result = sellerAuthService.login(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildRtCookie(result.refreshToken()).toString())
                .body(ApiResponse.ok(new TokenResponse(result.accessToken())));
    }

    /** 셀러 로그아웃 — Redis RT 삭제 + Cookie 만료 */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal SellerPrincipal principal) {

        if (principal != null) {
            sellerAuthService.logout(principal.getId());
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, expireRtCookie().toString())
                .body(ApiResponse.ok(null));
    }

    /** 셀러 토큰 갱신 — RT Cookie → 새 AT + RT */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(
            @CookieValue(name = RT_COOKIE, required = false) String refreshToken) {

        LoginResult result = sellerAuthService.refresh(refreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildRtCookie(result.refreshToken()).toString())
                .body(ApiResponse.ok(new TokenResponse(result.accessToken())));
    }

    // ----------------------------------------------------------------

    private ResponseCookie buildRtCookie(String token) {
        return ResponseCookie.from(RT_COOKIE, token)
                .httpOnly(true)
                .secure(cookieSecure)
                .path(RT_PATH)
                .maxAge(refreshTokenExpirationMs / 1000)
                .sameSite("Lax")
                .build();
    }

    private ResponseCookie expireRtCookie() {
        return ResponseCookie.from(RT_COOKIE, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path(RT_PATH)
                .maxAge(0)
                .sameSite("Lax")
                .build();
    }
}
