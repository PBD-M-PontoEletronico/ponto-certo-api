package com.mobdata.pontocerto.specification;

import com.mobdata.pontocerto.model.PoliticaForaPerimetro;
import com.mobdata.pontocerto.model.Setor;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public class SetorSpecification {

    private SetorSpecification() {
    }

    public static Specification<Setor> comNome(String nome) {
        if (nome == null || nome.isBlank()) {
            return Specification.unrestricted();
        }
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("nome")), "%" + nome.toLowerCase() + "%");
    }

    public static Specification<Setor> comEmpresaId(UUID empresaId) {
        if (empresaId == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("empresa").get("id"), empresaId);
    }

    public static Specification<Setor> comExigirSelfie(Boolean exigirSelfie) {
        if (exigirSelfie == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("exigirSelfie"), exigirSelfie);
    }

    public static Specification<Setor> comIgnorarLocalizacao(Boolean ignorarLocalizacao) {
        if (ignorarLocalizacao == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("ignorarLocalizacao"), ignorarLocalizacao);
    }

    public static Specification<Setor> comPoliticaForaPerimetro(PoliticaForaPerimetro politica) {
        if (politica == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("politicaForaPerimetro"), politica);
    }
}
