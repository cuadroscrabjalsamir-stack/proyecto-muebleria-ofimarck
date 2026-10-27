package com.ofimarck.controller;

import com.ofimarck.domain.DireccionEnvio;
import com.ofimarck.domain.Usuario;
import com.ofimarck.dto.request.DireccionEnvioRequest;
import com.ofimarck.repository.DireccionEnvioRepository;
import com.ofimarck.repository.UsuarioRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/direcciones")
public class DireccionEnvioController {

    private final DireccionEnvioRepository direccionRepository;
    private final UsuarioRepository usuarioRepository;

    public DireccionEnvioController(DireccionEnvioRepository direccionRepository,
                                    UsuarioRepository usuarioRepository) {
        this.direccionRepository = direccionRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    public ResponseEntity<List<DireccionEnvio>> listar(@RequestParam Integer idUsuario) {
        List<DireccionEnvio> direcciones = direccionRepository.findByUsuario_IdUsuarioAndEstadoTrue(idUsuario);
        return ResponseEntity.ok(direcciones);
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestParam Integer idUsuario,
                                   @Valid @RequestBody DireccionEnvioRequest request) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(idUsuario);
        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Usuario no encontrado"));
        }

        DireccionEnvio d = new DireccionEnvio();
        d.setUsuario(usuarioOpt.get());
        d.setAlias(request.getAlias());
        d.setDestinatario(request.getDestinatario());
        d.setTelefono(request.getTelefono());
        d.setDepartamento(request.getDepartamento());
        d.setProvincia(request.getProvincia());
        d.setDistrito(request.getDistrito());
        d.setLineaDireccion(request.getLineaDireccion());
        d.setReferencia(request.getReferencia());
        d.setCodigoPostal(request.getCodigoPostal());
        d.setPredeterminada(request.getPredeterminada() != null ? request.getPredeterminada() : false);
        d.setEstado(true);

        direccionRepository.save(d);

        return ResponseEntity.ok(Map.of("mensaje", "Dirección registrada correctamente", "id", d.getIdDireccion()));
    }
}
