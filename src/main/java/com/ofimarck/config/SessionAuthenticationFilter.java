package com.ofimarck.config;

import com.ofimarck.domain.Usuario;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtro puente entre la autenticación manual por sesión y Spring Security.
 *
 * <p>El sistema realiza el inicio de sesión de forma manual: el controlador
 * verifica la contraseña con BCrypt y guarda el {@link Usuario} en el atributo
 * de sesión {@code USUARIO_LOGUEADO}. Sin embargo, Spring Security no conoce
 * esa sesión y, por tanto, las reglas {@code authenticated()} y
 * {@code hasRole("ADMIN")} rechazaban las llamadas del frontend a
 * {@code /api/**} con HTTP 403 aunque el usuario ya hubiera iniciado sesión.
 *
 * <p>Este filtro lee el usuario de la sesión y construye un
 * {@link UsernamePasswordAuthenticationToken} con la autoridad
 * {@code ROLE_<rol>}, de modo que las reglas de autorización funcionan sin
 * reemplazar el flujo de login existente (principio de mínima invasión).
 *
 * <p>Se registra manualmente en {@link SecurityConfig} y no como
 * {@code @Component} para evitar que Spring Boot lo registre dos veces en la
 * cadena de servlets.
 */
public class SessionAuthenticationFilter extends OncePerRequestFilter {

    /** Nombre del atributo de sesión donde el login manual guarda al usuario. */
    public static final String SESSION_USER_ATTRIBUTE = "USUARIO_LOGUEADO";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                Object attribute = session.getAttribute(SESSION_USER_ATTRIBUTE);
                if (attribute instanceof Usuario usuario && Boolean.TRUE.equals(usuario.getEstado())) {
                    String rol = (usuario.getRol() != null && usuario.getRol().getNombreRol() != null)
                            ? usuario.getRol().getNombreRol().toUpperCase()
                            : "CLIENTE";

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    usuario.getCorreo(),
                                    null,
                                    List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
                    authentication.setDetails(usuario.getIdUsuario());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
