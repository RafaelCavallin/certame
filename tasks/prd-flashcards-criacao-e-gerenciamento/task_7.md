# Tarefa 7.0: Infraestrutura de UI — `shared/ui/dialog` e `shared/ui/toast`

## Visão geral

Duas peças transversais que nascem aqui e serão usadas por toda a plataforma: o diálogo e o toast.

O `DialogService` é o que torna CA-02 uma propriedade estrutural em vez de um cuidado: a criação de flashcard é um **diálogo, nunca uma rota** (regra 8 do `AGENTS.md`), e é este serviço que abre um componente sobre a tela sem mexer na URL, sem desmontar nada e sem tocar em `Router`. Ele cuida de `aria-modal`, armadilha de foco, `Esc`, foco inicial configurável e devolução do foco ao elemento indicado no fechamento — os quatro comportamentos que CA-05 e a seção de acessibilidade do PRD exigem.

O `ToastStore` publica a mensagem de confirmação com `role="status"`, sem bloquear a tela, e a expira sozinho.

Vêm antes do modal porque o modal é o primeiro consumidor dos dois, e a API do diálogo se paga três vezes: F6b constrói o diálogo de deck sobre ela e o `confirmacao-dialog` da F1 tende a migrar para ela.

<skills>
### Conformidade com skills

- `angular-developer` — componentes standalone e *zoneless*; estado por signals em serviço `providedIn: 'root'` (`ToastStore`); `input()`/`output()` como funções; template e estilo em arquivos separados; acessibilidade por ARIA (`aria-modal`, `role="dialog"`, `role="status"`).
- `impeccable` — **não aplicável.** O visual do diálogo e do toast vem do protótipo Nocturne; nada é redesenhado.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

`AGENTS.md` e as três rules lidos na íntegra. Regras relevantes: regra 1 (marcação e tokens do protótipo, `var(--*)` de `styles.css`, **nenhuma biblioteca de UI de terceiros** — o diálogo e a armadilha de foco são escritos à mão, não trazidos de um CDK), regra 8 (o diálogo não é uma rota: nada aqui injeta `Router` nem toca em `location`) e regra 3 (textos em pt-BR). `code-standards.md` §4: componentes standalone, zoneless, estado em signals, template e estilo em arquivos separados. `folder-structure.md` §3.1 já prevê `shared/ui/toast`.

**Nota de reuso:** `shared/ui/confirmacao-dialog/` já existe (`prd-banco-de-questoes`). Ele é reutilizado como está na tarefa 9.0; uma eventual migração para cima do novo `dialog` é oportunidade, não requisito desta entrega.
</rules>

<requirements>
- `shared/ui/dialog/` com um `DialogService` e um contêiner `app-dialog`.
- `role="dialog"` e `aria-modal="true"` no contêiner, com rótulo acessível ligado ao título.
- **Armadilha de foco:** `Tab` e `Shift+Tab` circulam apenas entre os controles do diálogo e não alcançam a tela ao fundo.
- **Foco inicial configurável** ao abrir (no modal de flashcard, o campo Frente).
- **Devolução de foco** ao elemento indicado por quem abriu, no fechamento — por qualquer caminho (salvar, cancelar, `Esc`).
- `Esc` fecha pelo mesmo caminho de Cancelar; **não** é um atalho de descarte que pula a confirmação (a decisão de confirmar é do consumidor, na tarefa 8.0).
- A superfície pública do serviço é mínima e deliberada: abrir com um componente e um contexto, e observar o fechamento. Nada de `Router`, nada de `location`, nada de estado global de sessão.
- `shared/ui/toast/` com `ToastStore` (signals, `providedIn: 'root'`) e componente `app-toast` com `role="status"`, não bloqueante, que expira sozinho.
- Marcação e classes traduzidas 1:1 do protótipo (`.dialog`, `.dialog-backdrop`), preservando os seletores usados pelos E2E da tarefa 10.0.
- Testes com temporizador falso (`vi.useFakeTimers()`), sem espera real.
</requirements>

## Subtarefas

- [x] 7.1 Criar `shared/ui/dialog/` com o `DialogService` e o contêiner `app-dialog`, em tradução 1:1 do protótipo.
- [x] 7.2 Implementar a armadilha de foco, o foco inicial configurável e a devolução de foco no fechamento.
- [x] 7.3 Implementar o fechamento por `Esc` pelo mesmo caminho do cancelamento.
- [x] 7.4 Criar `shared/ui/toast/` com o `ToastStore` em signals e o componente `app-toast` com `role="status"`.
- [x] 7.5 Escrever TU-39, TU-40 e TU-45.

## Detalhes de implementação

Ver TechSpec, seção *Visão dos componentes* (linhas de `shared/ui/dialog/` e `shared/ui/toast/`), *Sequenciamento do desenvolvimento* (passo 9 e a razão de vir antes do modal) e *Riscos conhecidos* (a nota de que o `dialog` nasce aqui e será a base do diálogo de deck de F6b, e de que uma API mal desenhada agora se paga três vezes). O PRD, em *Experiência do usuário — UI/UX e acessibilidade*, define o comportamento de foco e o toast não bloqueante.

## Critérios de aceitação relacionados

- CA-01
- CA-05
- CA-07

## Testes da tarefa

### Testes de unidade

- [x] TU-39 — `DialogService` devolve o foco ao fechar (Vitest)
- [x] TU-40 — `DialogService` confina o `Tab` no diálogo (Vitest)
- [x] TU-45 — `ToastStore` publica e expira a mensagem (Vitest)

## Arquivos relevantes

- `frontend/src/app/shared/ui/dialog/dialog-service.ts`
- `frontend/src/app/shared/ui/dialog/dialog.ts`
- `frontend/src/app/shared/ui/dialog/dialog.html`
- `frontend/src/app/shared/ui/dialog/dialog.scss`
- `frontend/src/app/shared/ui/dialog/dialog-service.spec.ts`
- `frontend/src/app/shared/ui/toast/toast-store.ts`
- `frontend/src/app/shared/ui/toast/toast.ts`
- `frontend/src/app/shared/ui/toast/toast.html`
- `frontend/src/app/shared/ui/toast/toast.scss`
- `frontend/src/app/shared/ui/toast/toast-store.spec.ts`
