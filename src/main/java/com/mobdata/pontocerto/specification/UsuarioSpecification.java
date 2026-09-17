package com.mobdata.pontocerto.specification;

import com.mobdata.pontocerto.model.Perfil;
import com.mobdata.pontocerto.model.Usuario;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

/**
 * Um predicado por coluna filtrável da busca de usuários. Cada método
 * devolve Specification.unrestricted() quando o filtro não foi informado —
 * a partir do Spring Data JPA 4.0, .where(...)/.and(...) não aceitam mais
 * null (lançam IllegalArgumentException); unrestricted() é o substituto
 * oficial para "não participa da busca".
 */
public final class UsuarioSpecification {

    private UsuarioSpecification() {
    }

    public static Specification<Usuario> comNome(String nome) {
        return contendoIgnoreCase("nome", nome);
    }

    public static Specification<Usuario> comUsuario(String usuario) {
        return contendoIgnoreCase("usuario", usuario);
    }

    public static Specification<Usuario> comMatricula(String matricula) {
        return contendoIgnoreCase("matricula", matricula);
    }

    public static Specification<Usuario> comCargo(String cargo) {
        return contendoIgnoreCase("cargo", cargo);
    }

    public static Specification<Usuario> comPerfil(Perfil perfil) {
        if (perfil == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("perfil"), perfil);
    }

    public static Specification<Usuario> comEmpresaId(UUID empresaId) {
        if (empresaId == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("empresa").get("id"), empresaId);
    }

    private static Specification<Usuario> contendoIgnoreCase(String campo, String valor) {
        if (valor == null || valor.isBlank()) {
            return Specification.unrestricted();
        }
        return (root, query, cb) ->
                cb.like(cb.lower(root.get(campo)), "%" + valor.toLowerCase() + "%");
    }
}