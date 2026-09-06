# Tarefa 3.0: Serviço GestaoUsuariosService e ajustes em RedefinicaoSenhaService

## Visão geral

Novo `GestaoUsuariosService` no pacote `usuario/`, com `@PreAuthorize("hasRole('SUPER_ADMIN')")`, concentrando listagem, criação (com convite opcional), edição, exclusão, redefinição de senha e reenvio de convite. Inclui as salvaguardas de auto-modificação (CA-07) e último super-admin ativo (CA-08), a geração de hash descartável quando não há convite, e o registro de auditoria em cada operação. `RedefinicaoSenhaService` ganha `emitirConvite`/`emitirRedefinicao` sem o limitador anônimo de tentativas.

<skills>
### Conformidade com skills

- `spring-boot-testing` — estratégia de teste unitário sem Spring, com mocks manuais de `UsuarioRepository`, `RedefinicaoSenhaService`, `RefreshTokenService` e `RegistradorAuditoria`; `Clock.fixed` quando o tempo entra na regra.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

Lido `AGENTS.md` e as rules em `.agents/rules/` (`code-standards.md`, `tests.md`, `folder-structure.md`).

- **Regra 2 (AGENTS.md)** — autorização validada no servidor com `@PreAuthorize("hasRole('SUPER_ADMIN')")` no serviço, independentemente da interface (CA-13).
- **Regra 3 (AGENTS.md)** — mensagens de erro em português do Brasil; códigos de erro (`EMAIL_EM_USO`, `AUTO_MODIFICACAO`, `ULTIMO_SUPER_ADMIN`, `STATUS_INCOMPATIVEL`) em `SNAKE_CASE` sem acento.
- Camada de serviço concentra `@Transactional` e regra de negócio (`.agents/rules/folder-structure.md` §2.1); repositório e controller não implementam salvaguardas.
- Cobertura mínima 80% e prioridade **Alta** para autorização e salvaguardas — caminho feliz e bordas cobertos (`techspec.md`, "Abordagem de testes").
- Testes nomeados `TU-_-` com princípios FIRST e AAA/Given-When-Then (`.agents/rules/tests.md`).
</rules>

<requirements>
- RF-24, RN-05 — criação com convite opcional; com `enviarConvite=true`, força status `CONVITE_PENDENTE` e emite token `CONVITE`; sem convite, gera hash descartável (nunca transmitido).
- RF-25 — edição de perfil/status com salvaguardas de autoexclusão (CA-07) e último super-admin (CA-08); e-mail revalida unicidade.
- RF-26 — exclusão lógica (`excluidoEm`), revogando refresh tokens e invalidando tokens de senha pendentes, respeitando as mesmas salvaguardas.
- RF-27 — `redefinirSenha` exige status `ATIVO`; `reenviarConvite` exige `CONVITE_PENDENTE`; caso contrário, `STATUS_INCOMPATIVEL` (409).
- CA-14 — toda operação registra auditoria com ator, ação, entidade e payload, na mesma transação.
</requirements>

## Subtarefas

- [x] 3.1 Implementar `GestaoUsuariosService.listar(FiltroUsuarios, Paginacao)` usando a Specification da tarefa 2.0, com whitelist de `sort` limitada a `criadoEm,asc`/`criadoEm,desc`.
- [x] 3.2 Implementar `criar(RequisicaoCriarUsuario)`: validar unicidade de e-mail (`EMAIL_EM_USO`), forçar `CONVITE_PENDENTE` quando `enviarConvite=true`, gerar hash descartável quando `enviarConvite=false`, e chamar `emitirConvite` de forma assíncrona após o commit.
- [x] 3.3 Implementar `editar(UUID usuarioId, RequisicaoEditarUsuario, UUID atorId)` com as salvaguardas `AUTO_MODIFICACAO` (CA-07) e `ULTIMO_SUPER_ADMIN` (CA-08), revalidação de unicidade de e-mail e revogação de refresh tokens do usuário editado.
- [x] 3.4 Implementar `excluir(UUID usuarioId, UUID atorId)` com as mesmas salvaguardas, preenchendo `excluidoEm`, revogando refresh tokens e invalidando tokens de senha pendentes.
- [x] 3.5 Implementar `redefinirSenha`/`reenviarConvite` delegando a `RedefinicaoSenhaService.emitirRedefinicao`/`emitirConvite`, com `STATUS_INCOMPATIVEL` quando o status não corresponde.
- [x] 3.6 Adicionar `RedefinicaoSenhaService.emitirRedefinicao`/`emitirConvite` (sem `LimitadorTentativas`) e ajustar `TokenSenhaRepository.invalidarPendentes` para receber a finalidade.
- [x] 3.7 Registrar auditoria (`RegistradorAuditoria`) em cada uma das cinco operações, na mesma transação.

## Detalhes de implementação

Ver `techspec.md`, seções "Design de implementação" (interfaces `GestaoUsuariosService`/`RedefinicaoSenhaService`), "Modelos de dados" (`FiltroUsuarios`, `UsuarioGestaoResumo`, `RequisicaoCriarUsuario`, `RequisicaoEditarUsuario`, `ErroApi`) e "Auditoria — registro por operação".

## Critérios de aceitação relacionados

A validação desses critérios é responsabilidade do QA:

- CA-02
- CA-03
- CA-04
- CA-05
- CA-06
- CA-07
- CA-08
- CA-12
- CA-14

## Testes da tarefa

### Testes de unidade

- [x] TU-01 — `TU01_criaUsuarioComConviteForcandoStatusPendente`
- [x] TU-02 — `TU02_criaUsuarioSemConviteGerandoHashDescartavel`
- [x] TU-03 — `TU03_recusaEmailDuplicadoInformandoOCampo`
- [x] TU-04 — `TU04_editaPerfilPromovendoProfessorParaAdmin`
- [x] TU-05 — `TU05_bloqueiaAutoRebaixamentoOuDesativacao`
- [x] TU-06 — `TU06_bloqueiaUltimoSuperAdminNoRebaixarOuDesativar`
- [x] TU-07 — `TU07_excluiUsuarioLogicamenteRevogandoTokens`
- [x] TU-08 — `TU08_bloqueiaAutoexclusao`
- [x] TU-09 — `TU09_redefineSenhaApenasEmUsuarioAtivo`
- [x] TU-10 — `TU10_reenviaConviteApenasEmPendenteInvalidandoAnteriores`
- [x] TU-11 — `TU11_rejeitaSortForaDaListaPermitida`
- [x] TU-12 — `TU12_registraAuditoriaEmTodaOperacao`

Implementado em `GestaoUsuariosServiceTest` (13/13 passando, mocks manuais sem contexto Spring; inclui `TU11b_aceitaOrdenacaoPermitida` como complemento de borda).

## Arquivos relevantes

- `backend/src/main/java/br/com/certame/usuario/GestaoUsuariosService.java`
- `backend/src/main/java/br/com/certame/usuario/dto/UsuarioGestaoResumo.java`
- `backend/src/main/java/br/com/certame/usuario/dto/FiltroUsuarios.java`
- `backend/src/main/java/br/com/certame/usuario/dto/RequisicaoCriarUsuario.java`
- `backend/src/main/java/br/com/certame/usuario/dto/RequisicaoEditarUsuario.java`
- `backend/src/main/java/br/com/certame/auth/RedefinicaoSenhaService.java` (métodos `emitirConvite`/`emitirRedefinicao`)
- `backend/src/main/java/br/com/certame/auth/TokenSenhaRepository.java` (já ajustado na tarefa 2.0)
- `backend/src/main/java/br/com/certame/comum/excecao/StatusIncompativelException.java` (novo; reusa `ConflitoException`/`EmailEmUsoException` já existentes)
- `backend/src/test/java/br/com/certame/usuario/GestaoUsuariosServiceTest.java`
