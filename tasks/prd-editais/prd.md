# Documento de Requisitos do Produto (PRD)

> **Funcionalidade:** Editais acompanhados · **Fase:** F4 — Simulados
> Derivado de `PRD.md` (Certame v1.3). Cobre RF-17 e RF-22.

## Visão geral

O candidato organiza a vida de estudo em torno de editais: quantas vagas, qual banca, qual prazo e — sobretudo — quais disciplinas são cobradas. Hoje essa informação vive espalhada em sites de notícias e PDFs oficiais, e o estudante perde tempo garimpando o que deveria estar ao lado das questões que ele resolve.

Esta funcionalidade dá à administração um cadastro simples de editais (status, vagas, banca, prazo e disciplinas exigidas) e ao estudante uma visão sempre atualizada dos editais publicados. É a menor peça do produto e a de maior efeito de contexto: ela conecta o esforço diário de resolução ao concurso concreto que o candidato quer prestar.

## Objetivos

- **Contexto sempre atual:** alteração feita pela administração reflete imediatamente na visão do estudante, sem cache ou publicação em lote.
- **Cobertura mínima de lançamento:** ao menos 10 editais cadastrados e classificados por status antes do lançamento (premissa, a confirmar com o time editorial).
- **Utilidade comprovada:** ≥ 25% dos estudantes ativos abrem a tela de editais ao menos uma vez por mês (premissa de meta, a calibrar após o lançamento).
- **Consistência taxonômica:** 100% das disciplinas exigidas em um edital referenciam a árvore de matérias existente, sem texto livre.
- **Zero informação obsoleta visível:** editais encerrados são marcados como tal, nunca apresentados como abertos.

## Histórias de usuário

- **US1** — Como estudante, quero ver os editais publicados com seu status — aberto, previsto ou encerrado — para saber o que está valendo agora.
- **US2** — Como estudante, quero ver as disciplinas exigidas por um edital, para alinhar meu estudo ao que será cobrado.
- **US3** — Como estudante, quero ver vagas, banca e prazo de cada edital, para decidir em qual concurso me inscrever.
- **US4** — Como administrador, quero cadastrar e editar editais com status, vagas, banca, prazo e disciplinas, para manter a base informada.
- **US5** — Como administrador, quero que minha alteração apareça imediatamente para o estudante, para corrigir um dado errado sem esperar publicação.
- **Caso de borda 1** — Um edital previsto é aberto: mudo apenas o status e a data, e ele migra de agrupamento na visão do estudante.
- **Caso de borda 2** — Um edital tem prazo vencido mas ainda está marcado como aberto: a tela administrativa sinaliza a inconsistência para correção.
- **Caso de borda 3** — Nenhum edital publicado: o estudante vê estado vazio explicativo, não uma tela quebrada.

## Principais funcionalidades

### 1. Editais na visão do estudante (RF-17)

Consulta somente leitura, agrupada por status, com as disciplinas exigidas em destaque.

- **RF-17** — Editais acompanhados com status (aberto, previsto, encerrado) e disciplinas exigidas. O estudante vê os editais publicados pela administração.

### 2. CRUD de editais (RF-22)

Cadastro administrativo enxuto, sem fluxo de aprovação: quem edita, publica.

- **RF-22** — CRUD de editais com status, vagas, banca, prazo e disciplinas. Qualquer alteração reflete imediatamente na visão do estudante.

### 3. Vínculo com a taxonomia

As disciplinas exigidas apontam para a árvore de matérias mantida em `prd-banco-de-questoes` (RF-20) — é esse vínculo que permitirá, no futuro, ligar edital a estudo dirigido.

## Critérios de aceitação

- **CA-01** (US1, RF-17) — Dada a tela de editais do estudante, quando a abro, então vejo os editais publicados agrupados ou identificados por status aberto, previsto e encerrado.
- **CA-02** (US2, RF-17) — Dado um edital selecionado, quando o abro, então vejo a lista de disciplinas exigidas.
- **CA-03** (US3, RF-17) — Dado um edital, quando o vejo na lista, então banca, vagas e prazo são exibidos em formato brasileiro de data e número.
- **CA-04** (US4, RF-22) — Dado o formulário de edital, quando informo status, vagas, banca, prazo e disciplinas e salvo, então o edital é criado e passa a constar na visão do estudante.
- **CA-05** (US5, RF-22) — Dada uma alteração salva pelo administrador, quando o estudante abre ou recarrega a tela de editais, então vê o dado atualizado, sem etapa adicional de publicação.
- **CA-06** (Borda 1, RF-22) — Dado um edital com status previsto, quando o administrador o altera para aberto, então ele muda de agrupamento na visão do estudante.
- **CA-07** (US4, RF-22) — Dado um edital excluído pelo administrador, quando o estudante abre a tela, então ele não aparece mais na lista.
- **CA-08** (RF-22) — Dadas as disciplinas exigidas, quando as seleciono no formulário, então elas vêm da árvore de matérias cadastrada, sem digitação livre.
- **CA-09** (Borda 3, RF-17) — Dado que não há editais publicados, quando o estudante abre a tela, então vê um estado vazio explicativo.
- **CA-10** (RF-22) — Dado um usuário sem perfil de administrador, quando tenta criar, editar ou excluir um edital, então a operação é recusada no servidor.

## Experiência do usuário

**Perfis e necessidades.** O estudante consulta editais em momentos de decisão — escolher concurso, ajustar o plano de estudo. O administrador atualiza poucos registros, mas com frequência e sob pressão de atualidade.

**Fluxos principais.**
1. *Consultar* — Editais → lista por status → abrir edital → disciplinas exigidas.
2. *Cadastrar* — Administração → Editais → novo → dados e disciplinas → salvar.
3. *Atualizar status* — Administração → Editais → editar status e prazo → salvar (reflete imediatamente).

**UI/UX e acessibilidade.**
- Tradução 1:1 do protótipo aprovado sobre o design system Nocturne; status por rótulo textual além da cor.
- Estados vazios explicativos e datas em formato brasileiro.
- Navegação completa por teclado, foco visível e contraste mínimo de 4,5:1.
- Português do Brasil em toda a interface.

## Restrições técnicas de alto nível

- **Stack não negociável:** Angular 22 no frontend; Spring Boot 4.1 / Java 25, PostgreSQL 18 e Flyway no backend.
- **Autorização no servidor:** criação, edição e exclusão exigem perfil administrador ou superior; a leitura é aberta a qualquer usuário autenticado.
- **Vínculo N:N com disciplinas** da árvore de matérias existente — sem duplicar taxonomia.
- **Propagação imediata:** nenhuma camada de cache pode atrasar a visão do estudante em relação à edição administrativa.
- **Auditoria:** alterações de edital são ações administrativas auditadas.
- **Idioma:** português do Brasil.

## Fora do escopo

- Importação automática de editais a partir de fontes oficiais ou PDFs.
- Inscrição no concurso ou qualquer integração com a banca organizadora.
- Notificações de abertura de edital ou de prazo (push fora da v1).
- Plano de estudo gerado automaticamente a partir do edital.
- Anexo do PDF do edital e leitura do documento dentro da plataforma.
- Acompanhamento personalizado ("meus editais" por estudante) — na v1 a lista é a mesma para todos.
- Vínculo automático entre edital e simulado — em `prd-simulados`.
