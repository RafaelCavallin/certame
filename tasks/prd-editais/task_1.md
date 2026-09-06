# Tarefa 1.0: Desenhar as telas `EDITAIS` e `ADMIN: EDITAIS` no protótipo Nocturne

## Visão geral

Acrescentar ao `protótipo/Certame.dc.html` as duas telas inéditas desta funcionalidade — a consulta do estudante e o cadastro administrativo — usando apenas componentes Nocturne já existentes. É o passo 1 da ordem de construção: pela regra 1 do `AGENTS.md`, nenhuma marcação nasce direto no Angular, e a aprovação visual desta tarefa é o que libera as tarefas 8.0 e 9.0.

Hoje o protótipo só tem um card de editais dentro de `ADMIN: MATÉRIAS & EDITAIS`, com descrição em texto livre e "disciplinas cobradas (separadas por vírgula)" — incompatível com o modelo estruturado do PRD. O modal é substituído por tela própria; o card vira atalho para a rota nova.

<skills>
### Conformidade com skills

- `impeccable` — **não aplicável**: o layout é congelado pela regra 1 do `AGENTS.md`. As telas são compostas com componentes Nocturne existentes e o padrão visual das telas irmãs (`CADERNOS`, `SIMULADOS`, `ADMIN: QUESTÕES`), não redesenho.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

`AGENTS.md`, `.agents/rules/code-standards.md`, `.agents/rules/folder-structure.md` e `.agents/rules/tests.md` lidos na íntegra.

- **Regra 1** — é a razão de existir desta tarefa: o protótipo é a fonte da verdade visual e precede o Angular. Só `var(--*)` de `styles.css`; nenhum valor cru de cor, fonte, espaçamento, raio ou sombra; nenhuma biblioteca de UI de terceiros.
- **Regra 3** — toda a interface em português do Brasil, com acentuação; datas e números em formato brasileiro.
- **PRD, Experiência do usuário** — status por rótulo textual além da cor, foco visível, contraste mínimo de 4,5:1.
- `.agents/rules/tests.md` — sem casos de teste próprios: esta tarefa produz a marcação de onde os seletores de E2E-01…E2E-12 serão extraídos.
</rules>

<requirements>
- **RF-17** — tela do estudante com editais por status (aberto, previsto, encerrado) e disciplinas exigidas.
- **RF-22** — tela administrativa de CRUD com status, vagas, banca, prazo e disciplinas.
- Reusar exclusivamente componentes Nocturne já presentes: `.card`, `.tag`, `.table`, `.field`, `.input`, `.btn`, `.btn-primary`, `.dialog`, `.elev-sm`.
- Ícone do menu: `ph ph-scroll`, do conjunto Phosphor já embarcado. Aviso de prazo vencido: `ph-warning-circle` com o papel `--color-danger`.
</requirements>

## Subtarefas

- [x] 1.1 Acrescentar o item "Editais" (`ph ph-scroll`) à seção Estudo da navegação do protótipo.
- [x] 1.2 Desenhar a tela `EDITAIS` do estudante: três seções por status, cada uma com título e contagem, no padrão de seção já usado em `CADERNOS`.
- [x] 1.3 Desenhar o card de edital: rótulo textual de status, banca, órgão, vagas, prazo em formato brasileiro e disciplinas exigidas como `.tag`.
- [x] 1.4 Desenhar os textos de ausência do card — "Vagas a definir" e "Data a definir" —, nunca um traço mudo, para o edital previsto sem dados divulgados.
- [x] 1.5 Desenhar o estado vazio explicativo da tela do estudante ("Nenhum edital acompanhado no momento."), no padrão de `shared/ui/estado-vazio`.
- [x] 1.6 Desenhar a tela `ADMIN: EDITAIS`: tabela com nome, status, banca, vagas, prazo, contagem de disciplinas e ações, no padrão de `ADMIN: QUESTÕES`.
- [x] 1.7 Desenhar o aviso de prazo vencido na linha da tabela: ícone `ph-warning-circle` e o texto "prazo vencido, revisar", com o status permanecendo "Aberto".
- [x] 1.8 Desenhar o formulário de página cheia: nome, status, vagas, banca, órgão, prazo, URL oficial e multisseleção de disciplinas, sem nenhum campo de texto livre para disciplina.
- [x] 1.9 Desenhar o diálogo de confirmação de exclusão de edital, reusando `.dialog`.
- [x] 1.10 Substituir o card e o modal atuais de `ADMIN: MATÉRIAS & EDITAIS`: o card passa a ser atalho para a tela nova e o modal com descrição livre e disciplinas por vírgula é removido.
- [x] 1.11 Verificar percurso por teclado, foco visível e contraste ≥ 4,5:1 dos rótulos de status nas duas telas.
- [x] 1.12 Obter a aprovação visual das duas telas antes de iniciar as tarefas 8.0 e 9.0.

## Detalhes de implementação

Ver `techspec.md` §*Sequenciamento do desenvolvimento* → *Ordem de construção*, passo 1, e §*Considerações técnicas* → *Riscos conhecidos*, as três primeiras linhas (tela do estudante inexistente, cadastro administrativo incompatível, aviso de prazo vencido e estado vazio ausentes). Os campos exibidos vêm de §*Modelos de dados* (`EditalResumo` e `EditalAdminResumo`).

## Critérios de aceitação relacionados

- CA-01
- CA-02
- CA-03
- CA-07
- CA-08
- CA-09
- Caso de borda 2 do PRD

## Testes da tarefa

Nenhum caso de teste automatizado. Esta tarefa é pré-requisito de marcação para E2E-01…E2E-12 (tarefa 10.0), cujos seletores saem daqui: `.card`, `.tag`, `.table`, `.field`, `.input`, `.btn-primary`, `.dialog`.

## Arquivos relevantes

- `protótipo/Certame.dc.html` (**modificado**) — telas `EDITAIS` e `ADMIN: EDITAIS`, item de menu, substituição do modal de edital
- `protótipo/_ds/` — referência dos componentes e tokens Nocturne
- `frontend/src/styles.css` — tokens `var(--*)`, incluindo `--color-danger`
