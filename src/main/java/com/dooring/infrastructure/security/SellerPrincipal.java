package com.dooring.infrastructure.security;

import com.dooring.domain.identity.entity.UserStatus;
import com.dooring.domain.identity.entity.UserType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Spring Security Principal — 셀러
 * JwtAuthenticationFilter가 AT 파싱 후 SecurityContext에 주입.
 * Controller에서 @AuthenticationPrincipal SellerPrincipal 로 꺼냄.
 */
public class SellerPrincipal implements UserDetails {

    private final Long id;
    private final UserStatus status;

    public SellerPrincipal(Long id, UserStatus status) {
        this.id = id;
        this.status = status;
    }

    public Long getId() { return id; }
    public UserType getUserType() { return UserType.SELLER; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_SELLER"));
    }

    // AT 기반 stateless 인증 — 패스워드 불필요
    @Override public String getPassword() { return null; }
    @Override public String getUsername() { return String.valueOf(id); }

    /** ACTIVE 상태일 때만 인증 통과 */
    @Override public boolean isEnabled() { return status == UserStatus.ACTIVE; }
}
