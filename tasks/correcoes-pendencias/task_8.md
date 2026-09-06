# Tarefa 8.0 (P3-2): Fazer valer o limite de 100 linhas e acabar com a linha comprimida

## Visão geral

`.agents/rules/code-standards.md` §2.2 impõe 100 linhas por arquivo. Dezesseis arquivos de backend
excedem o limite hoje:

| Arquivo | Linhas | Funcionalidade |
|---|---|---|
| `flashcard/Flashcard.java` | 199 | flashcards-criacao |
| `usuario/GestaoUsuariosService.java` | 190 | gestao-de-usuarios |
| `flashcard/FlashcardService.java` | 134 | flashcards-criacao |
| `questao/PublicacaoService.java` | 133 | banco-de-questoes |
| `catalogo/TaxonomiaService.java` | 130 | banco-de-questoes |
| `questao/QuestaoService.java` | 127 | banco-de-questoes |
| `simulado/SimuladoAdminService.java` | 125 | simulados |
| `auth/RedefinicaoSenhaService.java` | 121 | acesso-e-conta / gestao-de-usuarios |
| `flashcard/FlashcardRepository.java` | 119 | flashcards-criacao |
| `flashcard/revisao/FlashcardRevisao.java` | 116 | flashcards-decks-revisao |
| `estudo/SessaoEstudoService.java` | 111 | sessao-de-estudo |
| `usuario/UsuarioAdminController.java` | 108 | gestao-de-usuarios |
| `flashcard/revisao/SessaoRevisao.java` | 106 | flashcards-decks-revisao |
| `flashcard/revisao/RegistroRevisaoService.java` | 106 | flashcards-decks-revisao |
| `flashcard/revisao/SessaoRevisaoService.java` | 103 | flashcards-decks-revisao |
| `estudo/caderno/CadernoService.java` | 103 | cadernos-e-desempenho |

Além disso, vários relatórios apontam — e foi confirmado no código atual — que o limite vem sendo
"cumprido" comprimindo várias instruções por linha física (métodos inteiros escritos em uma única
linha em arquivos como `simulados-page.ts:16`, `simulado-form-page.ts:22`,
`questao-form-store.ts:32,38`, `catalogo-page.ts:13,15`, `materias-page.ts:14,15` e no método
`reconstruir` de `ReconciliadorAgregados`). Isso satisfaz a letra da regra e contraria o objetivo
dela (legibilidade e responsabilidade única por arquivo).

**Decisão tomada: o limite passa a valer de fato.** Os 16 arquivos são divididos por
responsabilidade, e a compressão de instruções por linha física é proibida e revertida.

<rules>
### Conformidade com o AGENTS.md e as rules

- `.agents/rules/code-standards.md` §2.2 — arquivos até 100 linhas, métodos até 30. Esta tarefa é a
  aplicação retroativa dessa regra.
- Dividir por **responsabilidade real** (extrair uma classe colaboradora coesa), nunca por corte
  arbitrário de linhas nem por compressão sintática.
</rules>

## Subtarefas

- [x] 8.1 Reverter o antipadrão de linha comprimida nos arquivos já identificados —
  `frontend/src/app/features/admin/simulados/simulados-page/simulados-page.ts:16`,
  `frontend/src/app/features/admin/simulados/simulado-form-page/simulado-form-page.ts:22`,
  `frontend/src/app/features/admin/questoes/questao-form-store.ts:32,38`,
  `frontend/src/app/features/admin/catalogo/catalogo-page/catalogo-page.ts:13,15`,
  `frontend/src/app/features/admin/materias/materias-page/materias-page.ts:14,15` e
  `backend/src/main/java/br/com/certame/desempenho/ReconciliadorAgregados.java` (método
  `reconstruir`) — reescrever cada método comprimido com uma instrução por linha. **A contagem de
  linhas dos arquivos de frontend vai subir**; é o resultado esperado e correto desta subtarefa.
  Os 5 arquivos de frontend já estavam decompostos (uma instrução por linha) após a tarefa 7.0
  (Prettier); confirmado com `npx prettier --check` nos 5 arquivos. Só `ReconciliadorAgregados.java`
  precisou de reescrita manual (construtor e `reconstruir` comprimidos).
- [x] 8.2 Dividir `flashcard/Flashcard.java` (199 linhas) por responsabilidade, começando pelo maior
  arquivo do backend. Extraídos `AgendamentoFsrs` (estado FSRS, `@Embeddable`), `OrigemFlashcard`
  (proveniência/classificação, `@Embeddable`) e `RegistroTemporalFlashcard` (timestamps de ciclo de
  vida, `@Embeddable`); `Flashcard.java` ficou com 100 linhas. Chamadores de `flashcard/revisao/*` e
  `FlashcardRepository`/`FlashcardService` atualizados para os novos getters (`getAgendamento()`,
  `getOrigem()`, `getRegistroTemporal()`).
- [x] 8.3 Dividir `usuario/GestaoUsuariosService.java` (190 linhas) por responsabilidade —
  coordenar com a tarefa 4.0, que também mexe neste arquivo (anonimização do histórico); fazer 8.3
  depois de 4.0 estar concluída para não conflitar. Extraídos `SalvaguardaUsuarioService`
  (auto-modificação/último super-admin), `LocalizadorUsuarioAdmin` (busca + unicidade de e-mail),
  `GeradorSenhaTemporaria` e `PaginadorUsuarios`; `redefinirSenha`/`reenviarConvite` viraram
  `AcessoUsuarioAdminService` + `AcessoUsuarioAdminController` (mesma fronteira de
  `prd-acesso-e-conta`). `GestaoUsuariosService.java` ficou com 100 linhas.
- [x] 8.4 Dividir `flashcard/FlashcardService.java` (134), `questao/PublicacaoService.java` (133) e
  `catalogo/TaxonomiaService.java` (130). `FlashcardService` (92 linhas): extraídos
  `FlashcardCicloVidaService` (arquivar/restaurar/excluir) e `AplicadorOrigemFlashcard`.
  `PublicacaoService` (90 linhas): extraída `MetricasPublicacao`; também removida a duplicação de
  `QuestaoService.detalhe()`/`id()` via `QuestaoDetalhe.de(Questao)`. `TaxonomiaService` (91 linhas,
  virou fachada fina): extraídos `ConsultaArvoreTaxonomia`, `DisciplinaTaxonomiaService` e
  `AssuntoTaxonomiaService`.
- [x] 8.5 Dividir os oito arquivos restantes da tabela (127 a 103 linhas), na ordem em que aparecem
  na tabela acima. `QuestaoService` (100): extraído `MontadorQuestao` +
  `ValidadorPublicacao.garantirPublicavel`. `SimuladoAdminService` (100): extraídos
  `ConsultaSimuladoAdmin`, `MontadorSimulado`, `ComposicaoSimuladoService`, `LocalizadorSimulado`.
  `RedefinicaoSenhaService` (91): extraído `EmissorTokenSenha`. `FlashcardRepository` (80): extraído
  `FlashcardContagemRepository` (contagens por deck) e removido overload morto de `buscarPorTermo`.
  `FlashcardRevisao` (86): extraído `AmostraFsrs` (`@Embeddable`). `SessaoEstudoService` (99):
  extraídos `AbridorSessaoEstudo`, `MontadorRespostaSessao`, `RetomadaSessaoEstudo`.
  `UsuarioAdminController` ficou em 96 linhas sem trabalho adicional (a extração de
  `AcessoUsuarioAdminController` na 8.3 já resolveu). `SessaoRevisao` (83): extraído
  `ResultadoSessaoRevisao` (`@Embeddable`). `RegistroRevisaoService` (99) e `SessaoRevisaoService`
  (100): extraído `LocalizadorSessaoRevisao` (duplicação real de "sessão aberta" entre os dois).
  `CadernoService` (100): extraídos `MutadorItensCaderno` e `ResumoCadernoService` (este último
  também usado por `ConsultaCadernosService`, que deixou de depender de `CadernoService`).
- [x] 8.6 Após cada divisão, rodar a suíte de testes do módulo afetado e confirmar que continua
  verde antes de passar ao próximo arquivo — a suíte verde é o critério de segurança da refatoração.
  Rodado por módulo a cada split (flashcard, usuario, questao, catalogo, simulado, auth, estudo) —
  todos verdes; suíte completa (`./mvnw test`) confirmada 100% verde ao final.
- [x] 8.7 Registrar em `.agents/rules/code-standards.md` §2.2 (ou em nota associada) a proibição
  explícita de comprimir múltiplas instruções em uma linha física como forma de "cumprir" o limite
  de 100 linhas, para a regra não ser burlada de novo. Adicionado parágrafo em §2.2 explicitando os
  padrões proibidos e a única exceção (getter/setter trivial de uma instrução).
- [x] 8.8 Rodar `find backend/src/main -name "*.java" | xargs wc -l | awk '$1>100'` e confirmar que
  nenhum arquivo listado na tabela acima ainda excede 100 linhas. Confirmado: o único arquivo listado
  pelo comando é `usuario/Usuario.java` (107 linhas), que não está na tabela desta tarefa — fora de
  escopo, não tocado.

## Detalhes de implementação

Executar **depois** da tarefa 7.0 (Prettier) — mesmos arquivos de frontend tocados por ambas.
Executar a subtarefa 8.3 **depois** da tarefa 4.0, pelo mesmo motivo (mesmo arquivo,
`GestaoUsuariosService.java`).

## Critérios de aceitação relacionados

- Nenhum CA de produto — é padrão de código; nenhuma divisão deve alterar comportamento observável.

## Testes da tarefa

- A suíte de testes existente de cada módulo dividido deve continuar 100% verde — nenhum teste novo
  é necessariamente exigido, mas se a divisão expuser um colaborador sem teste próprio, cobri-lo.

## Arquivos relevantes

- Os 16 arquivos de backend listados na tabela (divididos por responsabilidade).
- Os arquivos de frontend/backend com linha comprimida listados na subtarefa 8.1 (revertidos).
- `.agents/rules/code-standards.md` (nota sobre linha comprimida)
