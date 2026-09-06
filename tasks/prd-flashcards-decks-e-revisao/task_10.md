# Tarefa 10.0: Testes E2E, endurecimento e atualização da documentação

## Visão geral

Fecha a entrega: os dezesseis cenários E2E sobre o backend real (perfil `dev`, sementes `V906`/`V907`), a verificação de cobertura mínima de 80% em backend e frontend, e a atualização da `TECH-SPEC.md` (§5 com `flashcard_sessao` e as seis colunas novas, §6 com `PUT`/`DELETE /flashcards/decks/{id}` e o corpo enriquecido de `GET /flashcards/decks`), conforme a regra 4 do `AGENTS.md` exige na mesma entrega.

<skills>
### Conformidade com skills

- `spring-boot-testing` / `angular-developer` — não introduzem código novo de produção nesta tarefa; verificam a suíte já escrita nas tarefas 1.0–9.0.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

Lidos `AGENTS.md`, `.agents/rules/code-standards.md` e `.agents/rules/tests.md`.

- **Regra 4 (AGENTS.md)** — nenhum endpoint fora do contrato; `TECH-SPEC.md` §5/§6 atualizada na mesma alteração que introduz `PUT`/`DELETE /flashcards/decks/{id}` e o `GET /flashcards/decks` enriquecido.
- `tests.md` §4 — um cenário por fluxo nos testes E2E; pirâmide ~70/20/10 (unidade/integração/E2E) respeitada pelo conjunto das dez tarefas.
- `tests.md` §1 — cobertura mínima 80%, que prevalece sobre os 70% da `TECH-SPEC.md` §9.
</rules>

<requirements>
- Todos os RFs e RNs da funcionalidade (RF-33 a RF-36, RN-08, RN-10, RN-11), verificados fim a fim.
</requirements>

## Subtarefas

- [x] 10.1 Escrever e rodar os dezesseis cenários E2E com Playwright sobre o backend real, perfil `dev`, sementes `V906`/`V907` (remapeadas para `V14`/`V909` — ver `techspec.md`).
- [x] 10.2 Verificar cobertura do backend com `./mvnw verify` (JaCoCo) e do frontend com `npm run test:coverage`; corrigir lacunas abaixo de 80% (componente `sessao-revisao` reforçado de ~39% para 98.64% statements/100% branches).
- [x] 10.3 Atualizar `TECH-SPEC.md` §5 (esquema: `flashcard_sessao`, as três colunas novas em `flashcard`, as três em `flashcard_revisao`) e §6 (rotas `PUT`/`DELETE /flashcards/decks/{id}` e o corpo enriquecido de `GET /flashcards/decks`).
- [x] 10.4 Registrar em E2E-07 que o critério depende da tela de Início de `prd-sessao-de-estudo` (ainda sem TechSpec) e permanece verificado por TI-29 até essa entrega existir.

## Detalhes de implementação

Ver `techspec.md`, seções "Testes E2E", "Como testar" e "Sequenciamento do desenvolvimento" (passo 13).

## Critérios de aceitação relacionados

- CA-01 a CA-20 (verificação fim a fim; a validação funcional plena é responsabilidade do QA)

## Testes da tarefa

### Testes E2E

- [x] E2E-01 — Abrir a sessão mostra só a frente
- [x] E2E-02 — Barra de espaço revela o verso e as classificações
- [x] E2E-03 — Cada classificação exibe o intervalo que agendaria
- [x] E2E-04 — O verso mostra estabilidade, dificuldade e retenção
- [x] E2E-05 — Teclas 1 a 4 classificam e avançam
- [x] E2E-06 — Resumo ao fim da sessão
- [ ] E2E-07 — O tempo da sessão entra no total do dia (pendente da tela de Início de F2 — `test.skip`, verificado por TI-29 enquanto isso)
- [x] E2E-08 — A tela de decks mostra tudo que RF-35 pede
- [x] E2E-09 — Criar deck com nome curto ou repetido
- [x] E2E-10 — Renomear deck preserva os cards
- [x] E2E-11 — Excluir deck envia os cards para "Sem deck"
- [x] E2E-12 — Configurar 500 revisões por dia
- [x] E2E-13 — Aviso de fila acumulada na tela de decks
- [x] E2E-14 — Estudar um deck específico
- [x] E2E-15 — Fila do dia terminada
- [x] E2E-16 — Sessão inteira operável por teclado, com foco visível

## Arquivos relevantes

- Especificações Playwright (E2E) da sessão de revisão e da tela de decks
- `TECH-SPEC.md` (modificado — §5 e §6)
