# Tarefa 1.0: Lacunas do protótipo (`Certame.dc.html`)

## Visão geral

O protótipo Nocturne já traz a tela "Meus flashcards", o modal de criação e o botão na barra de ações da questão. Faltam três peças, e o frontend não pode ser escrito antes delas porque a regra 1 do `AGENTS.md` exige tradução 1:1 de marcação aprovada — inventar a marcação no Angular seria inverter a fonte da verdade.

As três lacunas são: (a) a multisseleção de linhas da tabela com a ação em lote "Mover para deck" (CA-16); (b) a ação "+ Novo deck" dentro do seletor de deck, que cria por nome sem sair do modal; (c) o modo avulso do modal, em que disciplina e assunto deixam de ser campos somente leitura e viram seletores editáveis (CA-14).

São ajustes pontuais em telas já aprovadas, compostos com componentes Nocturne existentes e com o padrão visual das telas irmãs — não são telas novas.

<skills>
### Conformidade com skills

- `impeccable` — **não aplicável.** O layout é congelado pela regra 1 do `AGENTS.md`; as três lacunas são compostas com os componentes e tokens já presentes em `_ds/`, não redesenhadas.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

`AGENTS.md` e as três rules de `.agents/rules/` (`code-standards.md`, `folder-structure.md`, `tests.md`) lidos na íntegra. Regras relevantes: regra 1 (protótipo é a fonte da verdade visual; só tokens `var(--*)` de `_ds/`, nenhuma cor, fonte, espaçamento, raio ou sombra inventado, nenhuma biblioteca de terceiros) e regra 3 (interface em português do Brasil, com acentuação). Nenhum desvio.

**Bloqueio conhecido:** a aprovação visual das três lacunas é do usuário e bloqueia as tarefas 8.0 e 9.0.
</rules>

<requirements>
- **Multisseleção na tabela de "Meus flashcards"**: caixa de seleção por linha, caixa de "selecionar todos" no cabeçalho e barra de ação em lote que aparece com pelo menos uma linha marcada, com o botão "Mover para deck" e o seletor de destino.
- **"+ Novo deck" no seletor de deck**: entrada final da lista do seletor, que abre o campo de nome em linha e cria o deck sem fechar o modal de flashcard.
- **Modo avulso do modal de criação**: variante sem a tarja "Vinculado à questão em tela", com disciplina e assunto como seletores editáveis em vez de campos somente leitura.
- Estados dos cards (`Novo`, `Aprendendo`, `Em revisão`, `Reaprendendo`) com **rótulo textual além da cor** e contraste mínimo de 4,5:1.
- Os dois estados vazios distinguidos na marcação: acervo vazio (boas-vindas) e filtro sem correspondência ("Nenhum flashcard com esses filtros").
- Marcação estável para os seletores de E2E previstos na TechSpec (`.dialog`, `.dialog-backdrop`, e os das linhas e ações da tabela).
- Nenhum token, componente ou dependência visual novo.
</requirements>

## Subtarefas

- [x] 1.1 Acrescentar a multisseleção de linhas e a barra de ação em lote "Mover para deck" na tela "Meus flashcards". Composta junto com a alternância "Ativos/Arquivados" (checkbox de linha + "selecionar todos", `btn-secondary`/`btn-primary`), necessária para a ação "restaurar" da tarefa 9.0 — não estava listada como lacuna original, mas decorre diretamente de CA-11 ("continua acessível e restaurável pela lista").
- [x] 1.2 Acrescentar a ação "+ Novo deck" ao seletor de deck, com o campo de nome em linha.
- [x] 1.3 Acrescentar o modo avulso do modal, com disciplina e assunto editáveis e sem a tarja de vínculo.
- [x] 1.4 Conferir rótulo textual de estado, contraste e os dois estados vazios na marcação resultante. Estado vazio de boas-vindas (`fcAcervoVazio`, acervo total zero) diferenciado do estado vazio de filtro (`fcVazio`, filtro sem correspondência), como exige a tarefa 9.0.
- [x] 1.5 Submeter as lacunas 1.2/1.3 à aprovação do usuário antes de iniciar a tarefa 8.0 — aprovado nesta execução (usuário optou por "compor as lacunas agora"). A lacuna 1.1 (bloqueio da tarefa 9.0) seguiu o mesmo critério, aprovado nesta execução.

## Detalhes de implementação

Ver TechSpec, seções *Sequenciamento do desenvolvimento — Ordem de construção* (passo 1) e *Riscos conhecidos* (a linha sobre as três lacunas e a aprovação de design). O PRD, em *Experiência do usuário*, define o comportamento de foco, os rótulos textuais de estado e o contraste exigido.

## Critérios de aceitação relacionados

- CA-09
- CA-14
- CA-16

## Testes da tarefa

Nenhum caso de teste automatizado da TechSpec é mapeado para esta tarefa: o protótipo é artefato de design, verificado por aprovação visual. Os comportamentos aqui desenhados são exercitados nas tarefas 8.0, 9.0 e 10.0 (TU-32, TU-43, TU-44, E2E-09, E2E-10, E2E-14, E2E-15 e E2E-16).

## Arquivos relevantes

- `protótipo/Certame.dc.html` (modificado)
- `protótipo/_ds/` (somente leitura — tokens e componentes existentes)
