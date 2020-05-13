package br.com.wspot.testemsidp;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    protected void configure(HttpSecurity http) throws Exception {
        http
            .authorizeRequests(authorize -> authorize
                    .antMatchers("/hello").access("hasAuthority('SCOPE_banana') AND hasAuthority('ROLE_admin')")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                        .jwtAuthenticationConverter(new CustomAuthenticationConverter())
                )
            );
    }

    static class CustomAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

        @Override
        public AbstractAuthenticationToken convert(Jwt jwt) {
            String scope = jwt.getClaim("scope");
            List<GrantedAuthority> prefixedScope = Arrays.stream(scope.split(" "))
                .map(s -> new SimpleGrantedAuthority("SCOPE_" + s))
                .collect(Collectors.toList())
            ;

            JSONObject realmAccess = jwt.getClaim("realm_access");
            JSONArray roles = (JSONArray) realmAccess.getOrDefault("roles", new JSONArray());
            List<GrantedAuthority> prefixedRoles = roles
                .stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .collect(Collectors.toList())
            ;

            List<GrantedAuthority> authorities = new ArrayList<>();
            authorities.addAll(prefixedScope);
            authorities.addAll(prefixedRoles);

            return new JwtAuthenticationToken(jwt, authorities);
        }
    }
}
