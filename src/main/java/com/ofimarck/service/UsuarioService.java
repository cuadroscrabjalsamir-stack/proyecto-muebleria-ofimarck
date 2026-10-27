package com.ofimarck.service;

import com.ofimarck.domain.Rol;
import com.ofimarck.domain.Usuario;
import com.ofimarck.exception.BadRequestException;
import com.ofimarck.exception.ResourceNotFoundException;
import com.ofimarck.repository.RolRepository;
import com.ofimarck.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, RolRepository rolRepository,
            PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Usuario> listarTodos() {
        return usuarioRepository.findByOrderByFechaRegistroDesc();
    }

    public Usuario buscarPorId(Integer id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));
    }

    public Optional<Usuario> buscarPorCorreo(String correo) {
        return usuarioRepository.findByCorreoIgnoreCase(correo);
    }

    @Transactional
    public Usuario registrarCliente(String nombres, String apellidos, String correo, String telefono, String password) {
        if (usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            throw new BadRequestException("El correo ya se encuentra registrado: " + correo);
        }
        Rol rolCliente = rolRepository.findByNombreRol("CLIENTE")
                .orElseThrow(() -> new ResourceNotFoundException("Rol CLIENTE no encontrado"));

        Usuario u = new Usuario();
        u.setNombres(nombres);
        u.setApellidos(apellidos);
        u.setCorreo(correo.toLowerCase().trim());
        u.setTelefono(telefono);
        u.setPasswordHash(passwordEncoder.encode(password));
        u.setRol(rolCliente);
        u.setEstado(true);
        return usuarioRepository.save(u);
    }

    @Transactional
    public Usuario cambiarEstado(Integer idUsuario, Boolean nuevoEstado) {
        Usuario u = buscarPorId(idUsuario);
        u.setEstado(nuevoEstado);
        return usuarioRepository.save(u);
    }

    @Transactional
    public void registrarAcceso(Integer idUsuario) {
        usuarioRepository.findById(idUsuario).ifPresent(u -> {
            u.setUltimoAcceso(OffsetDateTime.now());
            usuarioRepository.save(u);
        });
    }

    public boolean verificarPassword(String plainPassword, String hashedPassword) {
        return passwordEncoder.matches(plainPassword, hashedPassword);
    }
}
