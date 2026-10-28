package com.ofimarck.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuración de Spring Security para la aplicación Mueblería Ofimarck.
 *
 * <p>Decisiones de seguridad adoptadas (documentadas para la sustentación):
 *
 * <ul>
 *   <li><b>Encriptación de contraseñas:</b> Se utiliza {@link BCryptPasswordEncoder}
 *       como {@link PasswordEncoder}. BCrypt es adecuado porque incluye sal
 *       automática y es computacionalmente costoso, dificultando ataques de
 *       fuerza bruta y tablas rainbow.</li>
 *
 *   <li><b>CSRF (Cross-Site Request Forgery):</b> Se habilita CSRF para las
 *       peticiones que provienen de formularios Thymeleaf (vistas MVC), lo que
 *       obliga al cliente a enviar un token válido en cada {@code POST},
 *       {@code PUT}, {@code PATCH} o {@code DELETE}. Se <b>deshabilita CSRF
 *       exclusivamente para {@code /api/**}</b> porque esos endpoints son
 *       consumidos por clientes REST/JavaScript que manejan autenticación
 *       mediante sesión o token propio.</li>
 *
 *   <li><b>Autorización por roles:</b>
 *       <ul>
 *         <li>{@code /api/auth/**}: acceso público (login, registro, recuperación).</li>
 *         <li>{@code /api/admin/**}: solo rol {@code ADMIN}. Cualquier otro rol
 *         recibe HTTP 403.</li>
 *         <li>{@code /api/**}: acceso autenticado para clientes (carrito, pedidos, catálogo).</li>
 *         <li>Vistas Thymeleaf ({@code /}, {@code /login}, {@code /carrito}, {@code /catalogo}, etc.):
 *         acceso público para renderizar; las rutas de sesión manejan su propia redirección.</li>
 *       </ul>
 *   </li>
 *
 *   <li><b>Sesión:</b> {@code IF_REQUIRED} — se crea sesión solo cuando es
 *       necesario (login), evitando sesiones innecesarias en llamadas de API
 *       stateless. El login es manual (guarda {@code USUARIO_LOGUEADO} en la
 *       sesión), por lo que {@link SessionAuthenticationFilter} traduce esa
 *       sesión al {@code SecurityContext} para que las reglas
 *       {@code authenticated()} y {@code hasRole("ADMIN")} funcionen.</li>
 *
 *   <li><b>Respuestas de API:</b> para {@code /api/**} los errores 401 y 403
 *       se devuelven como JSON (no como redirección HTML), de modo que el
 *       frontend pueda diagnosticar la causa real con {@code response.json()}.</li>
 *
 *   <li><b>No se loguean datos sensibles:</b> los controladores y servicios
 *     registran eventos de auditoría (éxito/fallo de login, cambios de estado)
 *     sin exponer contraseñas, tarjetas ni DNI.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Bean de codificador de contraseñas basado en BCrypt.
     *
     * @return instancia de {@link BCryptPasswordEncoder}
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Cadena de filtros de seguridad.
     *
     * @param http configuración base de HttpSecurity
     * @return {@link SecurityFilterChain} construida
     * @throws Exception si falla la construcción
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // CSRF: habilitado para formularios Thymeleaf, deshabilitado solo para /api/**
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/**")
            )

            .headers(headers -> headers
                .frameOptions(frame -> frame.disable())
                .httpStrictTransportSecurity(hsts -> hsts.disable())
            )

            // Puente sesión manual -> SecurityContext (antes de las reglas de autorización)
            .addFilterBefore(new SessionAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)

            // Errores de API en JSON para que el frontend no reciba HTML inesperado
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write(
                            "{\"error\":\"No autenticado\",\"message\":\"Inicie sesión para continuar\"}");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write(
                            "{\"error\":\"Acceso denegado\",\"message\":\"No tiene permisos para este recurso\"}");
                })
            )

            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            )

            .authorizeHttpRequests(auth -> auth
                // API de autenticación: pública
                .requestMatchers("/api/auth/**").permitAll()

                // Catálogo público: visible sin iniciar sesión (igual que la vista /catalogo)
                .requestMatchers(HttpMethod.GET, "/api/productos", "/api/productos/**").permitAll()

                // API de administración: SOLO rol ADMIN
                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                // Resto de la API: accesible para usuarios autenticados (clientes)
                .requestMatchers("/api/**").authenticated()

                // Vistas Thymeleaf y recursos estáticos: públicas
                .requestMatchers("/", "/login", "/registro", "/catalogo", "/carrito", "/usuario",
                    "/admin", "/admin/**", "/css/**", "/js/**", "/img/**", "/*.html", "/favicon.ico")
                .permitAll()

                // Cualquier otra petición: autenticada
                .anyRequest().authenticated()
            );

        return http.build();
    }

    /**
     * Configuración CORS para permitir peticiones desde el frontend.
     *
     * @return fuente de configuración CORS
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
