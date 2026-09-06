# Tarefa 4.0 (P1-2): CA-11 de gestão de usuários — anonimização do histórico

## Visão geral

A exclusão de usuário faz apenas exclusão lógica (`excluidoEm`). A anonimização real do histórico foi
registrada como "risco aceito, adiado **até a tabela `resposta` existir**". **A tabela `resposta`
existe desde `V7__sessao_estudo.sql`.** A condição que suspendia o item foi satisfeita; o trabalho
não foi retomado.

Esta era a única tarefa deste conjunto com um passo bloqueante de decisão de produto antes de qualquer
código. A decisão foi obtida e registrada: placeholders para dados pessoais após sete dias,
preservando respostas, referências e agregados de desempenho.

<rules>
### Conformidade com o AGENTS.md e as rules

- `AGENTS.md` regra 5 — qualquer coluna nova ou alteração de esquema exigida pela decisão de
  anonimização entra por migração Flyway nova, versionada; não se edita migração já aplicada.
- `.agents/rules/code-standards.md` §"Migrações Flyway" — nomear a migração de forma descritiva
  (`V16__anonimizacao_historico_usuario.sql` ou próximo número livre) e documentar na TechSpec.
</rules>

<requirements>
- CA-11 (`prd-gestao-de-usuarios`, Borda 3, RF-26): "Dado um usuário excluído com histórico de
  respostas, quando consulto as estatísticas agregadas, então o histórico continua íntegro e sem
  identificação pessoal, e o acesso do usuário está encerrado."
</requirements>

## Subtarefas

- [x] 4.1 **Decisão de produto (bloqueante).** Definir com o dono do produto, e registrar na
  TechSpec de `prd-gestao-de-usuarios`:
  - quais colunas de quais tabelas contêm identificação pessoal do usuário excluído (`usuario.nome`,
    `usuario.email`, possivelmente campos em `resposta`, `sessao_estudo`, `flashcard` se guardarem
    texto livre identificável);
  - o que "anonimizar" significa tecnicamente para cada coluna (substituir por placeholder, hash
    irreversível, ou remover a linha mantendo só a chave estrangeira para o agregado estatístico);
  - política de retenção: a anonimização acontece imediatamente na exclusão, ou depois de um prazo?
  - confirmar que a anonimização não quebra `ReconciliadorAgregados`/`AtualizadorAgregados`, que
    dependem do histórico de respostas para os agregados de desempenho.
- [x] 4.2 Escrever a migração Flyway necessária (se a decisão 4.1 exigir coluna/índice novo — por
  exemplo, uma coluna de marca de anonimização, ou ajuste de constraint).
- [x] 4.3 Implementar a anonimização a partir do domínio de usuários, seguindo exatamente a decisão
  registrada em 4.1. A exclusão continua em `GestaoUsuariosService`; a rotina periódica foi isolada
  em `AnonimizadorHistoricoUsuario` para não executar a anonimização antes do prazo.
- [x] 4.4 Escrever teste de integração: excluir um usuário com histórico de respostas, consultar as
  estatísticas agregadas e confirmar que (a) o histórico permanece íntegro numericamente e (b)
  nenhuma coluna de identificação pessoal sobrevive legível.
- [x] 4.5 Atualizar `tasks/prd-gestao-de-usuarios/qa.md`: enquanto 4.1-4.4 não estiverem concluídas,
  manter CA-11 explicitamente como **pendência aberta**, não como "risco aceito" — a condição que
  justificava o adiamento já não existe.

## Detalhes de implementação

As subtarefas 4.2–4.4 dependiam da decisão 4.1; com a decisão registrada e os testes aprovados, a
tarefa está concluída.

## Critérios de aceitação relacionados

- CA-11 (`prd-gestao-de-usuarios`, Borda 3, RF-26)

## Testes da tarefa

- Teste de integração descrito em 4.4, novo em
  `backend/src/test/java/br/com/certame/usuario/`.

## Arquivos relevantes

- `tasks/prd-gestao-de-usuarios/techspec.md` (decisão registrada)
- `backend/src/main/resources/db/migration/` (migração nova, se aplicável)
- `backend/src/main/java/br/com/certame/usuario/GestaoUsuariosService.java` (exclusão lógica existente)
- `backend/src/main/java/br/com/certame/usuario/AnonimizadorHistoricoUsuario.java` (novo)
- `tasks/prd-gestao-de-usuarios/qa.md` (atualizado)
