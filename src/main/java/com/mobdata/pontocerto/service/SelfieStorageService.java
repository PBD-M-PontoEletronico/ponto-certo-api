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
 * Guarda as selfies de marcação de ponto em disco, no mesmo padrão do
 * AnexoStorageService (nome gerado por UUID, confere assinatura do arquivo) —
 * só que numa subpasta própria e aceitando apenas imagens (PNG/JPG), não PDF.
 */
@Service
public class SelfieStorageService {

    private static final long TAMANHO_MAXIMO_BYTES = 5L * 1024 * 1024;

    private static final Map<String, String> EXTENSAO_POR_TIPO = Map.of(
            "image/png", "png",
            "image/jpeg", "jpg"
    );

    private final Path pasta;

    public SelfieStorageService(@Value("${pontocerto.uploads-dir:uploads}") String pastaBase) {
        this.pasta = Path.of(pastaBase, "marcacoes").toAbsolutePath().normalize();
    }

    public SelfieSalva salvar(MultipartFile arquivo) {
        if (arquivo.getSize() > TAMANHO_MAXIMO_BYTES) {
            throw new IllegalArgumentException("A selfie deve ter no máximo 5 MB");
        }

        String tipo = arquivo.getContentType() == null ? "" : arquivo.getContentType().toLowerCase();
        String extensao = EXTENSAO_POR_TIPO.get(tipo);
        if (extensao == null) {
            throw new IllegalArgumentException("Selfie inválida: envie uma foto em PNG ou JPG");
        }

        byte[] conteudo;
        try {
            conteudo = arquivo.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível ler a selfie enviada", e);
        }

        if (!assinaturaConfere(extensao, conteudo)) {
            throw new IllegalArgumentException("O conteúdo do arquivo não corresponde a uma foto PNG ou JPG válida");
        }

        String nomeSalvo = UUID.randomUUID() + "." + extensao;
        try {
            Files.createDirectories(pasta);
            Files.write(pasta.resolve(nomeSalvo), conteudo);
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível gravar a selfie", e);
        }

        return new SelfieSalva(nomeSalvo, tipo);
    }

    public byte[] ler(String nomeSalvo) {
        Path caminho = pasta.resolve(nomeSalvo).normalize();
        if (!caminho.startsWith(pasta)) {
            throw new IllegalArgumentException("Selfie não encontrada");
        }
        try {
            return Files.readAllBytes(caminho);
        } catch (NoSuchFileException e) {
            throw new IllegalArgumentException("O arquivo da selfie não foi encontrado no servidor");
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível ler a selfie", e);
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
            case "png" -> b.length > 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G';
            case "jpg" -> b.length > 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF;
            default -> false;
        };
    }

    public record SelfieSalva(String arquivo, String tipoConteudo) {
    }
}