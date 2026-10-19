package co.edu.udea.codefactory.tracking;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfiguration {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AuditService auditService) throws Exception {
        return http.csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/api/seguimiento/**", "/actuator/health").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/ciudades", "/api/envios", "/api/envios/*").hasAnyRole("OPERADOR", "ADMIN", "VIEWER")
                .requestMatchers("/api/**").hasAnyRole("OPERADOR", "ADMIN")
                .anyRequest().denyAll())
            .exceptionHandling(errors -> errors.accessDeniedHandler((request, response, exception) -> {
                String path = request.getRequestURI();
                java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("/api/envios/(\\d+)").matcher(path);
                Long shipmentId = matcher.find() ? Long.valueOf(matcher.group(1)) : null;
                String actor = request.getUserPrincipal() == null ? "desconocido" : request.getUserPrincipal().getName();
                try {
                    auditService.recordAction("INTENTO_NO_AUTORIZADO", actor, shipmentId,
                        request.getRemoteAddr(), request.getMethod() + " " + path);
                } catch (RuntimeException ignored) {
                    // A failed audit write must not grant access.
                }
                response.setStatus(403);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":\"FORBIDDEN\",\"message\":\"No tienes permisos para realizar esta acción\"}");
            }))
            .httpBasic(Customizer.withDefaults())
            .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    UserDetailsService users(@Value("${app.security.operator-username}") String username,
                             @Value("${app.security.operator-password}") String password,
                             @Value("${app.security.admin-username}") String adminUsername,
                             @Value("${app.security.admin-password}") String adminPassword,
                             @Value("${app.security.viewer-username}") String viewerUsername,
                             @Value("${app.security.viewer-password}") String viewerPassword,
                             PasswordEncoder encoder) {
        if (username.isBlank() || password.length() < 12)
            throw new IllegalStateException("Defina OPERATOR_USERNAME y OPERATOR_PASSWORD (12+ caracteres) antes de iniciar");
        java.util.List<org.springframework.security.core.userdetails.UserDetails> accounts = new java.util.ArrayList<>();
        accounts.add(User.withUsername(username).password(encoder.encode(password)).roles("OPERADOR").build());
        if (!adminPassword.isBlank()) {
            if (adminPassword.length() < 12 || adminUsername.isBlank())
                throw new IllegalStateException("ADMIN_PASSWORD requiere 12+ caracteres y ADMIN_USERNAME");
            accounts.add(User.withUsername(adminUsername).password(encoder.encode(adminPassword))
                .roles("ADMIN", "OPERADOR").build());
        }
        if (!viewerPassword.isBlank()) {
            if (viewerPassword.length() < 12 || viewerUsername.isBlank())
                throw new IllegalStateException("VIEWER_PASSWORD requiere 12+ caracteres y VIEWER_USERNAME");
            accounts.add(User.withUsername(viewerUsername).password(encoder.encode(viewerPassword))
                .roles("VIEWER").build());
        }
        return new InMemoryUserDetailsManager(accounts);
    }

    @Bean
    CorsConfigurationSource cors(@Value("${app.cors.allowed-origin}") String origin) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(origin));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
