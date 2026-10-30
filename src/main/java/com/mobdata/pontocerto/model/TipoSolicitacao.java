package com.mobdata.pontocerto.model;

/**
 * W10 — os dois tipos de pedido que o funcionário faz para corrigir o mês.
 *
 * JUSTIFICATIVA: explica uma ocorrência (atraso, falta, saída antecipada),
 * com motivo e anexo opcional (ex.: atestado). Não cria marcação.
 *
 * AJUSTE: "esqueci de bater". O funcionário informa o horário pretendido;
 * se aprovado, nasce uma marcação NOVA ligada ao pedido — a original (ou a
 * ausência dela) nunca é apagada nem alterada.
 */
public enum TipoSolicitacao {
    JUSTIFICATIVA,
    AJUSTE
}
