package com.mobdata.pontocerto.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

/**
 * Guarda os anexos de afastamento em disco (pasta configurada em
 * pontocerto.uploads-dir). O arquivo é salvo com nome gerado pelo servidor
 * (UUID + extensão), nunca com o nome enviado pelo usuário — isso evita
 * sobrescrever arquivos e "path traversal" (nome tipo ../../algo).
 */
@Service
public class AnexoStorageService {

    private static final long TAMANHO_MAXIMO_BYTES = 5L * 1024 * 1024;

    private static final Map<String, String> EXTENSAO_POR_TIPO = Map.of(
            "application/pdf", "pdf",
            "image/png", "png",
            "image/jpeg", "jpg"
    );

    private final Path pasta;

    public AnexoStorageService(@Value("${pontocerto.uploads-dir:uploads}") String pastaBase) {
        this.pasta = Path.of(pastaBase, "afastamentos").toAbsolutePath().normalize();
    }

    public AnexoSalvo salvar(MultipartFile arquivo) {
        if (arquivo.getSize() > TAMANHO_MAXIMO_BYTES) {
            throw new IllegalArgumentException("O anexo deve ter no máximo 5 MB");
        }

        String tipo = arquivo.getContentType() == null ? "" : arquivo.getContentType().toLowerCase();
        String extensao = EXTENSAO_POR_TIPO.get(tipo);
        if (extensao == null) {
            throw new IllegalArgumentException("Anexo inválido: envie um arquivo PDF, PNG ou JPG");
        }

        byte[] conteudo;
        try {
            conteudo = arquivo.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível ler o anexo enviado", e);
        }

        // O tipo informado pelo navegador é só uma declaração; confere os
        // primeiros bytes do arquivo para não aceitar qualquer coisa renomeada.
        if (!assinaturaConfere(extensao, conteudo)) {
            throw new IllegalArgumentException("O conteúdo do arquivo não corresponde a um PDF, PNG ou JPG válido");
        }

        String nomeSalvo = UUID.randomUUID() + "." + extensao;
        try {
            Files.createDirectories(pasta);
            Files.write(pasta.resolve(nomeSalvo), conteudo);
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível gravar o anexo", e);
        }

        return new AnexoSalvo(nomeSalvo, nomeOriginalSeguro(arquivo.getOriginalFilename(), extensao), tipo);
    }

    public byte[] ler(String nomeSalvo) {
        Path caminho = pasta.resolve(nomeSalvo).normalize();
        if (!caminho.startsWith(pasta)) {
            throw new IllegalArgumentException("Anexo não encontrado");
        }
        try {
            return Files.readAllBytes(caminho);
        } catch (NoSuchFileException e) {
            throw new IllegalArgumentException("O arquivo do anexo não foi encontrado no servidor");
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível ler o anexo", e);
        }
    }

    public void remover(String nomeSalvo) {
        try {
            Path caminho = pasta.resolve(nomeSalvo).normalize();
            if (caminho.startsWith(pasta)) {
                Files.deleteIfExists(caminho);
            }
        } catch (IOException ignorado) {
            // Sobrar um arquivo solto em disco não deve impedir a operação principal.
        }
    }

    private boolean assinaturaConfere(String extensao, byte[] b) {
        return switch (extensao) {
            case "pdf" -> b.length > 4 && b[0] == '%' && b[1] == 'P' && b[2] == 'D' && b[3] == 'F';
            case "png" -> b.length > 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G';
            case "jpg" -> b.length > 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF;
            default -> false;
        };
    }

    // O nome original só é usado para exibir/baixar; tira qualquer caminho e limita o tamanho.
    private String nomeOriginalSeguro(String original, String extensao) {
        if (original == null || original.isBlank()) {
            return "anexo." + extensao;
        }
        String nome = original.substring(Math.max(original.lastIndexOf('/'), original.lastIndexOf('\\')) + 1).trim();
        if (nome.isEmpty()) {
            return "anexo." + extensao;
        }
        return nome.length() > 120 ? nome.substring(nome.length() - 120) : nome;
    }

    public record AnexoSalvo(String arquivo, String nomeOriginal, String tipoConteudo) {
    }
}
