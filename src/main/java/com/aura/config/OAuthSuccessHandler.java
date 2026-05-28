package com.aura.config;

import com.aura.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuthSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserService userService;
    private final OAuth2AuthorizedClientService clientService;

    public OAuthSuccessHandler(UserService userService,
                                OAuth2AuthorizedClientService clientService) {
        this.userService = userService;
        this.clientService = clientService;
        setDefaultTargetUrl("/inbox.html");
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;

        OAuth2AuthorizedClient client = clientService.loadAuthorizedClient(
            token.getAuthorizedClientRegistrationId(),
            token.getName()
        );

        String accessToken = null;
        String refreshToken = null;

        if (client != null) {
            OAuth2AccessToken at = client.getAccessToken();
            OAuth2RefreshToken rt = client.getRefreshToken();
            if (at != null) accessToken = at.getTokenValue();
            if (rt != null) refreshToken = rt.getTokenValue();
        }

        userService.findOrCreate(token.getPrincipal(), accessToken, refreshToken);

        // Force redirect to inbox — no Outlook or saved request
        clearAuthenticationAttributes(request);
        response.sendRedirect("/inbox.html");
    }
}