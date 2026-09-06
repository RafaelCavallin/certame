# Tarefa 6.0: Serviços de sessão e revisão

## Visão geral

É onde o agendador (3.0) encontra a persistência (2.0) e a fila (5.0): abrir a sessão com a fila montada, registrar cada classificação aplicando o `AgendadorFsrs` e gravando a linha imutável do log, finalizar com o resumo consolidado, e expor a porta de leitura que o Início consumirá para somar o tempo de estudo (RF-06).

<skills>
### Conformidade com skills

- `spring-boot-testing` — JUnit + Mockito nos quatro serviços, mocks só na fronteira (repositórios e `Clock`); `AgendadorFsrs` real, nunca mockado, porque é ele que valida o resultado do `RegistroRevisaoService`.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

Lidos `AGENTS.md`, `.agents/rules/code-standards.md` e `.agents/rules/tests.md`.

- **Regra 7 (AGENTS.md)** — `@PreAuthorize("isAuthenticated()")` nos serviços de sessão; dono sempre do token.
- Restrição técnica do PRD — log imutável de revisões: nota, dias decorridos, estabilidade e dificuldade antes/depois, intervalo, tempo — é o que `RegistroRevisaoService` grava em toda chamada.
- `code-standards.md` — erros por subclasse de `ErroNegocioException` (`RecursoNaoEncontradoException`, `ConflitoException` para sessão finalizada), sem `try/catch` em controller.
</rules>

<requirements>
- RF-33, RF-34, RN-08.
- CA-01, CA-07, CA-08, CA-09, CA-10, CA-11, CA-19, CA-20.
</requirements>

## Subtarefas

- [x] 6.1 `flashcard/revisao/SessaoRevisaoService.java`: `abrir(usuarioId, RequisicaoSessaoRevisao)` monta a fila via `MontadorFila` e cria a sessão (CA-01, CA-19); `finalizar(sessaoId, usuarioId)` consolida e devolve o resumo (CA-10), recusando segunda finalização.
- [x] 6.2 `flashcard/revisao/RegistroRevisaoService.java`: `registrar(sessaoId, usuarioId, RequisicaoRevisao)` aplica `AgendadorFsrs.proximo`, escreve o bloco de memória do card via `agendarPara`, grava a linha imutável de `FlashcardRevisao`, trunca `tempoMs` acima do máximo (RF-34) e devolve `reenfileirar`/`card` quando o vencimento é ainda hoje (CA-07, CA-08, CA-09).
- [x] 6.3 `flashcard/revisao/ResumoRevisaoService.java`: `resumoDoUsuario` e `proximosVencimentos` — distribuição por nota, tempo total e as três faixas fixas de vencimento futuro, sempre presentes (CA-10).
- [x] 6.4 `flashcard/revisao/TempoEstudoFlashcards.java`: `segundosNoDia(usuarioId, dia)` somando apenas sessões finalizadas (CA-11).
- [x] 6.5 DTOs em `flashcard/revisao/dto/`: `RequisicaoSessaoRevisao`, `RespostaSessaoRevisao`, `CardRevisao`, `MemoriaCard`, `PrevisaoNota`, `RequisicaoRevisao`, `RespostaRevisao`, `ResumoSessao`, `FaixaVencimento`, `ResumoFlashcards`.

## Detalhes de implementação

Ver `techspec.md`, seções "Principais interfaces" (blocos `SessaoRevisaoService`/`RegistroRevisaoService`/`ResumoRevisaoService`/`TempoEstudoFlashcards`) e "Modelos de dados" (todos os contratos JSON desta tarefa, com exemplos de card em primeiro contato, reenfileiramento e sessão sem revisões).

## Critérios de aceitação relacionados

- CA-01, CA-07, CA-08, CA-09, CA-10, CA-11, CA-19, CA-20

## Testes da tarefa

### Testes de unidade

- [x] TU-40 — `RegistroRevisaoService` grava o log completo da revisão
- [x] TU-41 — `RegistroRevisaoService` atualiza a memória e as duas colunas de agenda
- [x] TU-42 — `RegistroRevisaoService` marca reenfileiramento em passo intradiário
- [x] TU-43 — `RegistroRevisaoService` trunca `tempoMs` acima do máximo
- [x] TU-44 — `RegistroRevisaoService` recusa card de outro usuário
- [x] TU-45 — `RegistroRevisaoService` recusa revisão em sessão finalizada
- [x] TU-46 — `SessaoRevisaoService.finalizar` consolida contagens e tempo
- [x] TU-47 — `SessaoRevisaoService.finalizar` recusa segunda finalização
- [x] TU-48 — `ResumoRevisaoService` monta as três faixas de vencimento
- [x] TU-54 — `TempoEstudoFlashcards` soma só sessões finalizadas do dia

## Arquivos relevantes

- `flashcard/revisao/SessaoRevisaoService.java`
- `flashcard/revisao/RegistroRevisaoService.java`
- `flashcard/revisao/ResumoRevisaoService.java`
- `flashcard/revisao/TempoEstudoFlashcards.java`
- `flashcard/revisao/dto/RequisicaoSessaoRevisao.java`
- `flashcard/revisao/dto/RespostaSessaoRevisao.java`
- `flashcard/revisao/dto/CardRevisao.java`
- `flashcard/revisao/dto/MemoriaCard.java`
- `flashcard/revisao/dto/PrevisaoNota.java`
- `flashcard/revisao/dto/RequisicaoRevisao.java`
- `flashcard/revisao/dto/RespostaRevisao.java`
- `flashcard/revisao/dto/ResumoSessao.java`
- `flashcard/revisao/dto/FaixaVencimento.java`
- `flashcard/revisao/dto/ResumoFlashcards.java`
