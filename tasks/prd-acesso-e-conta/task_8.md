# Tarefa 8.0: Fundação visual (design system Nocturne)

## Visão geral

Traz o design system Nocturne para o projeto Angular: o `styles.css` do protótipo passa a ser o `frontend/src/styles.css`, já com o papel `--color-danger-*` registrado, e as fontes Inter e os ícones Phosphor passam a ser servidos pelo projeto em vez de virem do Google Fonts e do unpkg. Nenhum valor de token existente é alterado.

**Aprovação visual do protótipo (passo 8c): concedida pelo usuário.** A extensão do `Certame.dc.html` — painel de definir nova senha nos três estados (formulário, concluído e link inválido), bloco de erro inline nas abas Entrar e Criar conta, e o papel `--color-danger-*` no `styles.css` e no `readme.md` do Nocturne — está aprovada. A tarefa 9.0 está liberada.

<skills>
### Conformidade com skills

- `angular-developer` — estilos globais e configuração de assets no `angular.json`; nenhuma biblioteca de UI de terceiros é introduzida.
- `impeccable` — **não aplicável**: o layout é congelado pela regra 1 do `AGENTS.md`, não há espaço para redesenho. Os aspectos de acessibilidade exigidos pelo PRD são cobertos em E2E-10 (tarefa 10.0).
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

`AGENTS.md` lido na íntegra. Não existe `.agents/rules/`. Regra 1 é a regra central desta tarefa: o frontend é tradução 1:1 do protótipo aprovado, com os mesmos tokens Nocturne; nenhuma cor, fonte, espaçamento, raio ou sombra é inventado, e nenhuma biblioteca de UI de terceiros é adicionada. **Adição registrada e aprovada:** o papel `--color-danger-*`, derivado de `#e0666e` e `#e0787f` — os hex que o próprio protótipo já usava no bloco de erro. Não é cor nova: é a formalização de uma cor existente como token, para que a validação inline exigida por CA-02, CA-05 e CA-07 não espalhe hex cru pelo código.
</rules>

<requirements>
- Copiar o `styles.css` do Nocturne (`protótipo/nocturne-7bd49ab8-.../styles.css`) na íntegra para `frontend/src/styles.css`, acrescido do papel `--color-danger-*`. Nenhum valor existente é alterado.
- Vendorizar a fonte Inter (hoje importada do Google Fonts pelo `@import` do Nocturne) e os ícones Phosphor (hoje carregados do unpkg pelo protótipo) como assets do projeto, removendo terceiros do caminho crítico de renderização sem alterar um único token.
- Acrescentar Playwright ao `package.json` — única dependência nova, e é de teste (bloqueia a tarefa 10.0).
</requirements>

## Subtarefas

- [x] 8.1 Copiar o `styles.css` do Nocturne para `frontend/src/styles.css`, preservando integralmente os valores existentes.
- [x] 8.2 Confirmar que o papel `--color-danger-*` está presente no `styles.css` do projeto, com os mesmos valores registrados no Nocturne.
- [x] 8.3 Vendorizar a fonte Inter como asset local e substituir o `@import` do Google Fonts por `@font-face` sobre os arquivos servidos pelo projeto.
- [x] 8.4 Vendorizar os ícones Phosphor como asset local e remover a referência ao unpkg.
- [x] 8.5 Declarar os assets em `angular.json` e verificar que a aplicação renderiza sem requisição a domínio de terceiro.
- [x] 8.6 Acrescentar Playwright ao `package.json` e à configuração de teste.

## Detalhes de implementação

Ver TechSpec, seções *Sequenciamento do desenvolvimento → Ordem de construção*, item 8 (8a, 8b e 8c), *Dependências técnicas* (Inter e Phosphor a vendorizar; Playwright ausente do `package.json`), *Principais decisões* (papel `--color-danger-*`; as lacunas de design entram pelo protótipo, não pelo código; Inter e Phosphor servidos pelo projeto) e *Riscos conhecidos* (as duas primeiras linhas, ambas já resolvidas).

## Critérios de aceitação relacionados

Nenhum critério de aceitação do PRD é validado diretamente por esta tarefa — é fundação visual. Ela habilita CA-02, CA-05 e CA-07, cuja validação inline depende do papel `--color-danger-*`.

## Testes da tarefa

Nenhum caso de teste da TechSpec é mapeado para esta tarefa; verificação por inspeção visual e pela ausência de requisições a domínios de terceiros no carregamento.

## Arquivos relevantes

- `frontend/src/styles.css`
- `frontend/src/assets/` (Inter e Phosphor vendorizados)
- `frontend/angular.json`
- `frontend/package.json`
- `protótipo/nocturne-7bd49ab8-9032-482f-a138-5e38a522a1be/styles.css` (origem, não alterada nesta tarefa)
- `protótipo/Certame.dc.html` (fonte visual aprovada)
