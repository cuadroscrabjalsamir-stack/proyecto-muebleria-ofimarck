package com.ofimarck.service;

import com.ofimarck.domain.PasswordResetToken;
import com.ofimarck.domain.Usuario;
import com.ofimarck.dto.request.ResetPasswordRequest;
import com.ofimarck.exception.BadRequestException;
import com.ofimarck.exception.ResourceNotFoundException;
import com.ofimarck.repository.PasswordResetTokenRepository;
import com.ofimarck.repository.UsuarioRepository;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class PasswordResetService {

    private static final int TOKEN_EXPIRATION_HOURS = 1;

    private final UsuarioRepository usuarioRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final String fromEmail;

    public PasswordResetService(UsuarioRepository usuarioRepository,
                                PasswordResetTokenRepository tokenRepository,
                                PasswordEncoder passwordEncoder,
                                JavaMailSender mailSender,
                                @org.springframework.beans.factory.annotation.Value("${app.mail.from:noreply@ofimarck.pe}") String fromEmail) {
        this.usuarioRepository = usuarioRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
    }

    @Transactional
    public String solicitarRestablecimiento(String correo) {
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(correo)
                .orElseThrow(() -> new ResourceNotFoundException("No existe un usuario con el correo: " + correo));

        if (!usuario.getEstado()) {
            throw new BadRequestException("La cuenta se encuentra inactiva");
        }

        String token = UUID.randomUUID().toString();
        OffsetDateTime expiracion = OffsetDateTime.now().plusHours(TOKEN_EXPIRATION_HOURS);

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUsuario(usuario);
        resetToken.setToken(token);
        resetToken.setExpiracion(expiracion);
        resetToken.setUsado(false);
        tokenRepository.save(resetToken);

        enviarCorreoRestablecimiento(usuario.getCorreo(), token);

        return token;
    }

    @Transactional
    public void restablecerContrasena(ResetPasswordRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("La contraseña y su confirmación no coinciden");
        }

        PasswordResetToken token = tokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new BadRequestException("Token inválido"));

        if (!token.isValid()) {
            throw new BadRequestException("El token ha expirado o ya fue utilizado");
        }

        Usuario usuario = token.getUsuario();
        usuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        usuarioRepository.save(usuario);

        token.setUsado(true);
        tokenRepository.save(token);
    }

    private void enviarCorreoRestablecimiento(String destino, String token) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(fromEmail);
        mensaje.setTo(destino);
        mensaje.setSubject("Restablecimiento de contraseña - Mueblería Ofimarck");
        mensaje.setText("Hola,\n\nHas solicitado restablecer tu contraseña en Mueblería Ofimarck.\n"
                + "Utiliza el siguiente token para crear una nueva contraseña:\n\n"
                + token + "\n\n"
                + "El token expira en " + TOKEN_EXPIRATION_HOURS + " hora(s).\n\n"
                + "Si no solicitaste este cambio, ignora este mensaje.\n\n"
                + "Saludos,\nEquipo de Mueblería Ofimarck");
        mailSender.send(mensaje);
    }
}
