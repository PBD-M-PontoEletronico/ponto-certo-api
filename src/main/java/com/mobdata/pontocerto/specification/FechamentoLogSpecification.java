package com.mobdata.pontocerto.specification;

import com.mobdata.pontocerto.model.AcaoFechamento;
import com.mobdata.pontocerto.model.FechamentoLog;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public class FechamentoLogSpecification {
    private FechamentoLogSpecification() {
    }

    public static Specification<FechamentoLog> comEmpresaId(UUID empresaId) {
        if (empresaId == null) return Specification.unrestricted();
        return (root, query, cb) -> cb.equal(root.get("empresa").get("id"), empresaId);
    }

    public static Specification<FechamentoLog> comAno(Integer ano) {
        if (ano == null) return Specification.unrestricted();
        return (root, query, cb) -> cb.equal(root.get("ano"), ano);
    }

    public static Specification<FechamentoLog> comMes(Integer mes) {
        if (mes == null) return Specification.unrestricted();
        return (root, query, cb) -> cb.equal(root.get("mes"), mes);
    }

    public static Specification<FechamentoLog> comAcao(AcaoFechamento acao) {
        if (acao == null) return Specification.unrestricted();
        return (root, query, cb) -> cb.equal(root.get("acao"), acao);
    }
}
