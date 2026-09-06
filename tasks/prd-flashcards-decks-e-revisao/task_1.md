# Tarefa 1.0: Design — fechar as lacunas do protótipo

## Visão geral

O protótipo aprovado já traz a tela de Decks, o diálogo de configuração e a sessão de revisão inteira, mas faltam três ajustes pontuais que esta funcionalidade introduz: o aviso de fila acumulada no card de deck, a linha virtual "Sem deck" na grade e o rótulo de intervalo em minutos nos botões de classificação (consequência do FSRS-5 completo com memória de curto prazo). Esta tarefa fecha as três lacunas em `protótipo/Certame.dc.html` antes de qualquer marcação nascer no Angular, conforme a regra 1 do `AGENTS.md`.

<skills>
### Conformidade com skills

Nenhuma skill de implementação de código se aplica diretamente — a `impeccable` não é usada porque o layout está congelado pela regra 1 do `AGENTS.md` (techspec, seção "Conformidade com skills"). Os três ajustes são compostos com componentes e tokens Nocturne já existentes no protótipo, seguindo o padrão visual das telas irmãs, não redesenho.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

Lidos `AGENTS.md` e `.agents/rules/code-standards.md`, `.agents/rules/tests.md` e `.agents/rules/folder-structure.md`.

- **Regra 1 (AGENTS.md)** — o frontend é tradução 1:1 do protótipo aprovado; esta tarefa existe justamente para que nenhuma cor, componente, raio, sombra ou espaçamento seja inventado depois, no Angular. Usar exclusivamente tokens `var(--*)` de `styles.css` e componentes Nocturne já presentes no protótipo.
- `folder-structure.md` §4 — `protótipo/Certame.dc.html` é a fonte da verdade visual; nada nele é editado para acomodar o código, é o código que se ajusta a ele.
</rules>

<requirements>
- RF-33 — rótulo de intervalo em minutos nos botões de classificação da sessão de revisão.
- RF-35 — linha virtual "Sem deck" na grade de decks.
- RF-36 / RN-10 — aviso de fila acumulada no card de deck.
- CA-18, CA-15 (consequência visual).
</requirements>

## Subtarefas

- [x] 1.1 Adicionar o aviso de fila acumulada no card de deck do protótipo (CA-18): mensagem informativa, não punitiva, sempre acompanhada da ação de ajustar limites.
- [x] 1.2 Adicionar a linha virtual "Sem deck" na grade de decks do protótipo, sem ação de renomear/excluir (CA-15).
- [x] 1.3 Adicionar o rótulo de intervalo em minutos nos quatro botões de classificação da sessão de revisão ("1 min", "10 min", além dos rótulos em dias já existentes).
- [x] 1.4 Obter aprovação visual das três lacunas antes de iniciar a tradução Angular (tarefas 8.0 e 9.0 dependem desta aprovação).

## Detalhes de implementação

Ver `techspec.md`, seção "Sequenciamento do desenvolvimento" (passo 1) e "Riscos conhecidos" (linha sobre a divergência visível do protótipo aprovado quanto aos rótulos em minutos).

## Critérios de aceitação relacionados

- CA-18
- CA-15

## Testes da tarefa

Tarefa de design visual, sem testes automatizados próprios; os três ajustes são exercitados pelos testes E2E da tarefa 10.0 (E2E-13, E2E-11, E2E-03).

## Arquivos relevantes

- `protótipo/Certame.dc.html` (modificado)
