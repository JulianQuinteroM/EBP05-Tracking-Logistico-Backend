package co.edu.udea.codefactory.tracking;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfiguration {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AuditService auditService, Accounts accounts) throws Exception {
        return http.csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/api/seguimiento/**", "/actuator/health").permitAll()
                .requestMatchers(HttpMethod.POST,"/api/auth/login","/api/reclamaciones/publicas","/api/reclamaciones/consulta","/api/reclamaciones/mensaje").permitAll()
                .requestMatchers("/api/auth/**").authenticated()
                .requestMatchers("/api/admin/usuarios/**").hasAuthority("USUARIOS")
                .requestMatchers("/api/admin/**").hasAuthority("AUDITORIA")
                .requestMatchers("/api/reportes/**").hasAuthority("REPORTES")
                .requestMatchers(HttpMethod.GET,"/api/envios/exportacion").hasAuthority("REPORTES")
                .requestMatchers(HttpMethod.GET,"/api/contactos/**").hasAuthority("CONTACTO_LEER")
                .requestMatchers("/api/contactos/**").hasAuthority("CONTACTO_EDITAR")
                .requestMatchers(HttpMethod.POST,"/api/reclamaciones/*/analisis").hasAuthority("IA")
                .requestMatchers("/api/reclamaciones/**").hasAuthority("RECLAMOS")
                .requestMatchers(HttpMethod.GET,"/api/envios/*/operacion","/api/envios/*/evidencia","/api/ciudades", "/api/envios", "/api/envios/*").hasAuthority("ENVIO_LEER")
                .requestMatchers(HttpMethod.POST,"/api/envios/*/entrega","/api/envios/*/intentos").hasAuthority("ENTREGA")
                .requestMatchers(HttpMethod.POST,"/api/envios/*/estado","/api/envios/*/ubicaciones","/api/envios/*/incidencia").hasAuthority("ENVIO_ESTADO")
                .requestMatchers(HttpMethod.PUT,"/api/envios/**").hasAuthority("ENVIO_EDITAR")
                .requestMatchers(HttpMethod.DELETE,"/api/envios/*").hasAuthority("ENVIO_EDITAR")
                .requestMatchers(HttpMethod.POST,"/api/envios/*/contactos").hasAuthority("ENVIO_EDITAR")
                .requestMatchers("/api/envios/*/cancelacion").hasAuthority("ENVIO_EDITAR")
                .requestMatchers(HttpMethod.POST,"/api/envios","/api/estimaciones").hasAuthority("ENVIO_CREAR")
                .requestMatchers("/api/**").denyAll()
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
            .addFilterBefore(new BearerAuthenticationFilter(accounts), org.springframework.security.web.authentication.www.BasicAuthenticationFilter.class)
            .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

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
