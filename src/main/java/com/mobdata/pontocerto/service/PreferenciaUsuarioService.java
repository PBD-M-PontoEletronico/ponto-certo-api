package com.mobdata.pontocerto.service;

import com.mobdata.pontocerto.model.PreferenciaUsuario;
import com.mobdata.pontocerto.model.Tema;
import com.mobdata.pontocerto.model.Usuario;
import com.mobdata.pontocerto.repository.PreferenciaUsuarioRepository;
import com.mobdata.pontocerto.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PreferenciaUsuarioService {

    @Autowired
    private PreferenciaUsuarioRepository preferenciaUsuarioRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    public Tema obterTema(UUID usuarioId) {
        return preferenciaUsuarioRepository.findByUsuarioId(usuarioId)
                .map(PreferenciaUsuario::getTema)
                .orElse(Tema.CLARO);
    }

    public Tema atualizarTema(UUID usuarioId, Tema tema) {
        PreferenciaUsuario preferencia = preferenciaUsuarioRepository.findByUsuarioId(usuarioId)
                .orElseGet(() -> criar(usuarioId));

        preferencia.setTema(tema);
        preferenciaUsuarioRepository.save(preferencia);
        return tema;
    }

    private PreferenciaUsuario criar(UUID usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        PreferenciaUsuario preferencia = new PreferenciaUsuario();
        preferencia.setUsuario(usuario);
        return preferencia;
    }
}
