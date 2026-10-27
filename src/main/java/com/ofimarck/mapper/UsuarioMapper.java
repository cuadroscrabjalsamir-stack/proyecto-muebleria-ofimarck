package com.ofimarck.mapper;

import com.ofimarck.domain.Usuario;
import com.ofimarck.dto.response.LoginResponse;
import org.springframework.stereotype.Component;

@Component
public class UsuarioMapper {

    public LoginResponse toLoginResponse(Usuario usuario, boolean authenticated, String mensaje) {
        return new LoginResponse(authenticated, mensaje, usuario);
    }
}