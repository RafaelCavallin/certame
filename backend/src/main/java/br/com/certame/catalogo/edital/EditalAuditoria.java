package br.com.certame.catalogo.edital;

import br.com.certame.catalogo.edital.dto.RequisicaoEdital;
import java.util.LinkedHashMap;
import java.util.Map;

final class EditalAuditoria {
    private EditalAuditoria() {}

    static Map<String, Object> payload(RequisicaoEdital requisicao) {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("nome", requisicao.nome());
        payload.put("status", requisicao.status());
        payload.put("vagas", requisicao.vagas());
        payload.put("bancaId", requisicao.bancaId());
        payload.put("orgaoId", requisicao.orgaoId());
        payload.put("prazoInscricao",
                requisicao.prazoInscricao() == null ? null : requisicao.prazoInscricao().toString());
        payload.put("urlOficial", requisicao.urlOficial());
        payload.put("disciplinaIds", requisicao.disciplinaIds());
        return payload;
    }
}
