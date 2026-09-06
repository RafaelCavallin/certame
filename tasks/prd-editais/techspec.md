# Especificação técnica

> **Funcionalidade:** Editais acompanhados · **PRD:** `tasks/prd-editais/prd.md` · **Fase:** F4 — Simulados
> Deriva de `TECH-SPEC.md` v1.0 (§3–§6, recortadas para RF-17 e RF-22).
> Depende de `tasks/prd-acesso-e-conta/techspec.md` (segurança, `perfilGuard`, shell, `styles.css`) e de `tasks/prd-banco-de-questoes/techspec.md`, que funda `disciplina`, `banca`, `orgao`, `RegistradorAuditoria` e os tipos de exceção reutilizados aqui.

## Resumo

Esta é a menor funcionalidade do produto e a que mais depende do que já existe: um agregado (`edital`), um vínculo N:N com a árvore de matérias e duas telas. Não há fluxo de aprovação, não há cache e não há estado intermediário — quem edita, publica (CA-05). O desenho inteiro decorre dessa decisão: a escrita administrativa e a leitura do estudante atacam a mesma tabela, na mesma transação lógica, e a resposta de leitura sai com `Cache-Control: no-store` para que nenhuma camada intermediária reintroduza a defasagem que o PRD proíbe.

O agrupamento por status é resolvido **no servidor**, não no cliente: `GET /api/v1/editais` devolve `{abertos, previstos, encerrados}` já ordenados — abertos pelo prazo mais próximo, previstos por nome, encerrados pelo prazo mais recente. Isso torna CA-01 e CA-06 propriedades observáveis do contrato (um `PUT` que muda o status faz o edital trocar de lista na resposta seguinte, sem uma linha de lógica no Angular) e faz CA-09 cair sozinho: três listas vazias é o estado vazio, não um caso especial.

`status` é sempre manual. O caso de borda 2 do PRD pede explicitamente que um edital aberto com prazo vencido seja **sinalizado para correção**, não corrigido sozinho — então `VerificadorPrazoEdital` é uma regra pura, aplicada apenas na listagem administrativa, e nada no sistema reescreve o status de ninguém. A contrapartida é uma invariante no banco: um edital `ABERTO` obriga `prazo_inscricao`, o que dá ao sinal de inconsistência um significado único, sem caso nulo.

As disciplinas exigidas são um `@ManyToMany` com `disciplina`, sem texto livre em lugar algum (CA-08) — a mesma consistência taxonômica vale para banca e órgão, que são FKs para o catálogo de `prd-banco-de-questoes` (RF-37), decisão do usuário registrada em *Principais decisões*. Esse vínculo tem um efeito de volta: excluir uma disciplina da árvore passa a ser recusado quando um edital a referencia, o que exige uma alteração pontual no `TaxonomiaService` entregue na F1 — registrada como componente modificado.

No frontend são duas telas novas em rota própria (`/editais` para o estudante, `/admin/editais` para o administrador), ambas ausentes do protótipo aprovado: hoje `Certame.dc.html` só tem um card de editais dentro de `ADMIN: MATÉRIAS & EDITAIS`, com descrição em texto livre e disciplinas separadas por vírgula. Pela regra 1 do `AGENTS.md`, as telas e os campos estruturados entram primeiro no protótipo, no passo 1 da ordem de construção, com componentes Nocturne existentes.

## Arquitetura do sistema

### Visão dos componentes

```
Angular 22 (SPA)                            Spring Boot 4.1                          PostgreSQL 18
────────────────                            ───────────────                          ─────────────
features/editais/                                                                    ┌── edital
  EditaisPage        ────▶  GET    /api/v1/editais                                   ├── edital_disciplina
  EditalCard                                                                         │
                                                                                     └─ referenciadas
features/admin/editais/                                                                 (V2, F1)
  EditaisAdminPage   ────▶  GET    /api/v1/admin/editais                             ┌── disciplina
                     ────▶  DELETE /api/v1/admin/editais/{id}                        ├── banca
  EditalFormPage     ────▶  GET    /api/v1/admin/editais/{id}                        ├── orgao
                     ────▶  POST   /api/v1/admin/editais                             └── auditoria
                     ────▶  PUT    /api/v1/admin/editais/{id}
  DisciplinasSeletor ────▶  GET    /api/v1/admin/disciplinas   (RF-20, F1)
                     ────▶  GET    /api/v1/admin/bancas · /admin/orgaos  (RF-37, F1)
```

**Backend — pacote `br.com.certame.catalogo.edital`**

| Componente | Tipo | Responsabilidade |
| --- | --- | --- |
| `catalogo/edital/Edital` | novo | Entidade raiz do agregado: nome, status, vagas, prazo, URL oficial, FKs de banca e órgão e o `@ManyToMany` com `Disciplina`. Exclusão lógica por `excluidoEm`. |
| `catalogo/edital/StatusEdital` | novo | Enum `ABERTO`, `PREVISTO`, `ENCERRADO`, persistido como texto com `check` equivalente na migração. |
| `catalogo/edital/EditalRepository` | novo | Consultas por status com `@EntityGraph` de banca, órgão e disciplinas (uma consulta, sem N+1); unicidade de nome entre não excluídos; verificação de uso de disciplina. |
| `catalogo/edital/EditalService` | novo | RF-22: criar, atualizar e excluir logicamente. Concentra `@Transactional`, o `@PreAuthorize` de administrador (CA-10) e a chamada de auditoria. |
| `catalogo/edital/ConsultaEditaisService` | novo | RF-17: monta `EditaisAgrupados` com os três grupos já ordenados (CA-01, CA-09). Somente leitura, sem autorização por perfil além da autenticação. |
| `catalogo/edital/ResolvedorDisciplinasEdital` | novo | CA-08: converte a lista de `UUID` recebida em entidades `Disciplina` existentes e não excluídas; recusa id desconhecido com erro campo a campo. Isola a única regra de integridade referencial do formulário. |
| `catalogo/edital/VerificadorPrazoEdital` | novo | Borda 2: regra pura `vencido(status, prazoInscricao)` sobre um `Clock` injetado. Sem Spring, sem repositório — é o que torna o sinal da tela administrativa testável em milissegundos. |
| `catalogo/edital/EditalConsultaVinculos` | novo | Porta de leitura consumida pela taxonomia: `disciplinaEmUso(UUID) -> boolean`. Existe para que `TaxonomiaService` não alcance `EditalRepository` diretamente (`code-standards.md` §3). |
| `catalogo/edital/EditalController` | novo | Rota de leitura `/api/v1/editais`, autenticada. Só orquestra e devolve o `ResponseEntity` com `Cache-Control: no-store`. |
| `catalogo/edital/EditalAdminController` | novo | Rotas de `/api/v1/admin/editais` (listar, obter, criar, atualizar, excluir), administrador ou superior. |
| `catalogo/edital/dto/` | novo | `EditaisAgrupados`, `EditalResumo`, `EditalAdminResumo`, `EditalDetalhe`, `RequisicaoEdital`, `DisciplinaExigida`, `ItemCatalogoResumo`. |
| `catalogo/TaxonomiaService` | **modificado** | `excluirDisciplina` passa a consultar `EditalConsultaVinculos` e recusar a exclusão de disciplina referenciada por edital, com `DISCIPLINA_EM_USO`. Sem essa alteração, a integridade de CA-08 dependeria só da FK e devolveria erro cru. |
| `comum/auditoria/RegistradorAuditoria` | reutilizado | Criar, editar e excluir edital são ações administrativas auditadas (restrição do PRD). |
| `comum/excecao/ConflitoException`, `RecursoNaoEncontradoException`, `DadosInvalidosException` | reutilizados | Nenhum tipo de exceção novo: os três criados em `prd-banco-de-questoes` cobrem nome duplicado, edital inexistente e referência inválida. `ManipuladorErrosGlobal` **não muda**. |

**Frontend — `frontend/src/app`**

| Componente | Tipo | Responsabilidade |
| --- | --- | --- |
| `core/api/edital-api.ts` | novo | Cliente HTTP das seis rotas; único ponto que fala com a API de editais. |
| `core/api/modelos-edital.ts` | novo | Tipagem dos contratos desta especificação. |
| `features/editais/editais-page/` | novo | RF-17: três seções por status, estado vazio explicativo (CA-09) e seleção de um edital por *deep link* `/editais/:id`. |
| `features/editais/edital-card/` | novo | Card de um edital: rótulo textual de status, banca, órgão, vagas, prazo em formato brasileiro e as disciplinas exigidas como `.tag` (CA-02, CA-03). |
| `features/editais/editais-store.ts` | novo | Signals da consulta: `agrupados`, `carregando`, `vazio` e `selecionado`. |
| `features/admin/editais/editais-admin-page/` | novo | RF-22: tabela administrativa com o aviso de prazo vencido (borda 2) e exclusão com confirmação (CA-07). |
| `features/admin/editais/edital-form-page/` | novo | Formulário de página cheia: nome, status, vagas, banca, órgão, prazo, URL oficial e disciplinas. |
| `features/admin/editais/editais-admin-store.ts` | novo | Signals da lista e do formulário; mapeia 422 campo a campo e 409 para o campo `nome`. |
| `features/admin/editais/disciplinas-seletor/` | novo | CA-08: multisseleção alimentada por `GET /admin/disciplinas`, sem entrada de texto livre. |
| `shared/ui/estado-vazio/` | reutilizado | Criado por `prd-cadernos-e-desempenho` (F3); se ainda não existir na entrega, nasce aqui. |
| `shared/ui/confirmacao-dialog/` | reutilizado | Criado por `prd-banco-de-questoes` (F1); usado na exclusão de edital. |
| `layout/shell/shell.html` | **modificado** | Novo item "Editais" (`ph ph-scroll`) na seção Estudo da navegação. |
| `app.routes.ts` | **modificado** | `/editais`, `/editais/:id` sob `authGuard`; `/admin/editais`, `/admin/editais/novo`, `/admin/editais/:id` sob `perfilGuard([ADMIN, SUPER_ADMIN])`. |
| `protótipo/Certame.dc.html` | **modificado** | Duas telas novas e a substituição do modal por formulário estruturado — detalhado em *Riscos conhecidos*. |

**Fluxo de dados.** O estudante abre `/editais`; a página faz **uma** requisição, recebe os três grupos já ordenados com as disciplinas embutidas e renderiza — não há segunda chamada para abrir um edital, porque o payload da lista já é o detalhe (decisão registrada). O administrador abre `/admin/editais`, recebe a lista plana com `prazoVencido` calculado no servidor e navega para o formulário, que carrega o edital e, em paralelo, a árvore de disciplinas e os catálogos de banca e órgão da F1. Salvar envia o agregado inteiro num único `POST`/`PUT`; a próxima leitura do estudante já reflete a alteração, sem etapa de publicação e sem cache no caminho.

## Design de implementação

### Principais interfaces

```
EditalService
  criar(RequisicaoEdital) -> EditalDetalhe            // admin+; audita
  atualizar(UUID id, RequisicaoEdital) -> EditalDetalhe // admin+; audita
  excluir(UUID id) -> void                            // admin+; logica (excluido_em); audita

ConsultaEditaisService
  agrupados() -> EditaisAgrupados                     // CA-01, CA-09
  listarParaAdministracao() -> List<EditalAdminResumo> // com prazoVencido (Borda 2)
  buscarParaEdicao(UUID id) -> EditalDetalhe

ResolvedorDisciplinasEdital
  resolver(Set<UUID> ids) -> Set<Disciplina>          // CA-08; lanca DadosInvalidosException

VerificadorPrazoEdital
  vencido(StatusEdital status, LocalDate prazo) -> boolean   // so ABERTO e prazo < hoje

EditalConsultaVinculos
  disciplinaEmUso(UUID disciplinaId) -> boolean       // consumida por TaxonomiaService
```

```
EditaisStore (signals)
  carregar() -> Promise<void>
  agrupados: Signal<EditaisAgrupados | null>
  vazio: Signal<boolean>                   // os tres grupos vazios (CA-09)
  selecionar(id: string) -> void

EditalFormStore (signals)
  carregar(id | null) -> Promise<void>
  salvar() -> Promise<void>
  exigePrazo: Signal<boolean>              // status ABERTO obriga prazo
  errosDeCampo: Signal<Record<string,string> | null>
```

### Modelos de dados

Contratos JSON entre backend e UI, prontos para exibição. Datas trafegam em ISO-8601 (`yyyy-MM-dd`) e são formatadas em pt-BR na interface pelo `DatePipe` com `LOCALE_ID` `pt-BR`; números seguem o `DecimalPipe` do mesmo locale (CA-03). Campo ausente na origem é normalizado para `null`, nunca omitido nem convertido em string vazia.

#### `EditaisAgrupados` — payload da tela do estudante (RF-17)

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `abertos` | `EditalResumo[]` | sim | Status `ABERTO`, ordenados pelo prazo mais próximo. Lista vazia quando não houver. |
| `previstos` | `EditalResumo[]` | sim | Status `PREVISTO`, ordenados por nome. |
| `encerrados` | `EditalResumo[]` | sim | Status `ENCERRADO`, ordenados pelo prazo mais recente primeiro; sem prazo vão ao fim. |

```text
{
  "abertos": [
    {
      "id": "8f2a1c6d-4b70-4a1e-9d33-2b0c5e91a7f4",
      "nome": "TRF 1ª Região — Analista Judiciário",
      "status": "ABERTO",
      "vagas": 320,
      "prazoInscricao": "2026-08-30",
      "urlOficial": "https://www.cebraspe.org.br/concursos/trf1_26_analista",
      "banca": { "id": "3f8e1d2c-9a44-4d15-b2e7-77c1a0b3e551", "nome": "Cebraspe", "sigla": "Cebraspe" },
      "orgao": { "id": "c41b7e90-2d68-4f0a-9c15-6e8b3a2d4477", "nome": "Tribunal Regional Federal da 1ª Região", "sigla": "TRF1" },
      "disciplinas": [
        { "id": "6a1c0f2e-1b93-4c07-8f2d-51a9c7e40b28", "nome": "Direito Constitucional" },
        { "id": "d20b4a75-8c31-4e6f-91a0-3f7d5b8c2e19", "nome": "Direito Administrativo" },
        { "id": "5e9f3b12-7d40-4a88-b6c3-90e2f1a7d5c6", "nome": "Português" }
      ]
    }
  ],
  "previstos": [
    {
      "id": "1d7c4b93-6e28-4f51-a90d-8b3e2c1f7a05",
      "nome": "SEFAZ-RS — Auditor Fiscal",
      "status": "PREVISTO",
      "vagas": null,
      "prazoInscricao": null,
      "urlOficial": null,
      "banca": { "id": "9b2d6f41-3a87-4c92-8e05-1f6a4d7b3c28", "nome": "Fundação Getulio Vargas", "sigla": "FGV" },
      "orgao": null,
      "disciplinas": [
        { "id": "72e5a90c-4f13-4b6d-8a27-c1905e3f8d64", "nome": "Direito Tributário" },
        { "id": "a83f10d5-6b29-4e74-95c8-2d0b7f4a1e93", "nome": "Contabilidade" }
      ]
    }
  ],
  "encerrados": []
}
```

> **Edital previsto:** `vagas`, `prazoInscricao`, `urlOficial` e `orgao` costumam ser `null` — o concurso ainda não foi publicado. A interface exibe "Vagas a definir" e "Data a definir" no lugar, nunca um traço mudo. Foi a decisão do usuário de **não** criar um campo de observação livre; o que o protótipo hoje escreve em prosa ("edital esperado para o 2º semestre") passa a ser lido do status.

> **Nenhum edital publicado (CA-09):** os três grupos vêm vazios e a tela mostra o estado vazio explicativo, não uma tela quebrada.

```text
{ "abertos": [], "previstos": [], "encerrados": [] }
```

#### `EditalResumo` — um edital na visão do estudante

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `id` | `uuid` | sim | Identificador do edital. |
| `nome` | `string` | sim | Nome completo, como cadastrado. 3 a 160 caracteres. |
| `status` | `string` | sim | `ABERTO`, `PREVISTO` ou `ENCERRADO`. Renderizado com rótulo textual além da cor. |
| `vagas` | `number \| null` | não | Inteiro ≥ 0. `null` quando ainda não divulgado. |
| `prazoInscricao` | `string \| null` | não | Data ISO `yyyy-MM-dd`. Sempre preenchida quando `status` é `ABERTO`. |
| `urlOficial` | `string \| null` | não | URL `http://` ou `https://` do edital na página da banca. |
| `banca` | `ItemCatalogoResumo \| null` | não | Referência ao catálogo de bancas (RF-37). |
| `orgao` | `ItemCatalogoResumo \| null` | não | Referência ao catálogo de órgãos (RF-37). |
| `disciplinas` | `DisciplinaExigida[]` | sim | Disciplinas exigidas, ordenadas por nome. Pode vir vazia. |

#### `DisciplinaExigida` e `ItemCatalogoResumo` — referências à taxonomia e ao catálogo

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `id` | `uuid` | sim | Id na árvore de matérias (`disciplina`) ou no catálogo (`banca`/`orgao`). |
| `nome` | `string` | sim | Nome corrente; renomear a disciplina reflete aqui sem migração de dados. |
| `sigla` | `string \| null` | só em `ItemCatalogoResumo` | Sigla da banca ou do órgão. |

```text
{ "id": "6a1c0f2e-1b93-4c07-8f2d-51a9c7e40b28", "nome": "Direito Constitucional" }
```

#### `EditalAdminResumo` — uma linha da tabela administrativa (RF-22)

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `id`, `nome`, `status`, `vagas`, `prazoInscricao`, `banca`, `orgao` | — | — | Iguais a `EditalResumo`. |
| `totalDisciplinas` | `number` | sim | Contagem, não a lista — a tabela administrativa não exibe as tags. |
| `prazoVencido` | `boolean` | sim | `true` apenas quando `status` é `ABERTO` e `prazoInscricao` é anterior a hoje (borda 2). Calculado no servidor sobre `America/Sao_Paulo`. |
| `atualizadoEm` | `string` | sim | Instante ISO-8601 UTC da última alteração. |

```text
[
  { "id": "3c9e...", "nome": "PF — Agente de Polícia Federal", "status": "ABERTO", "vagas": 1500,
    "prazoInscricao": "2026-07-12", "banca": { "id": "3f8e...", "nome": "Cebraspe", "sigla": "Cebraspe" },
    "orgao": null, "totalDisciplinas": 6, "prazoVencido": true, "atualizadoEm": "2026-06-02T11:20:37Z" },
  { "id": "1d7c...", "nome": "SEFAZ-RS — Auditor Fiscal", "status": "PREVISTO", "vagas": null,
    "prazoInscricao": null, "banca": { "id": "9b2d...", "nome": "Fundação Getulio Vargas", "sigla": "FGV" },
    "orgao": null, "totalDisciplinas": 2, "prazoVencido": false, "atualizadoEm": "2026-05-19T08:44:02Z" }
]
```

> `prazoVencido` **nunca** altera o `status`. O PRD determina que a inconsistência seja sinalizada para correção manual; nenhum job, gatilho ou serviço reescreve o status de um edital.

#### `EditalDetalhe` — payload do formulário administrativo

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `id`, `nome`, `status`, `vagas`, `prazoInscricao`, `urlOficial`, `banca`, `orgao`, `disciplinas` | — | — | Iguais a `EditalResumo`. |
| `criadoEm`, `atualizadoEm` | `string` | sim | Instantes ISO-8601 UTC. |

#### `RequisicaoEdital` — corpo de `POST` e `PUT`

| Campo | Tipo | Obrigatório | Regras |
| --- | --- | --- | --- |
| `nome` | `string` | sim | `@NotBlank` · 3 a 160 caracteres · único entre não excluídos, ignorando maiúsculas. |
| `status` | `string` | sim | `@NotNull` · `ABERTO`, `PREVISTO` ou `ENCERRADO`. |
| `vagas` | `number \| null` | não | `@Min(0)` · `@Max(999999)`. |
| `bancaId` | `uuid \| null` | não | Deve existir em `banca`. |
| `orgaoId` | `uuid \| null` | não | Deve existir em `orgao`. |
| `prazoInscricao` | `string \| null` | condicional | Data ISO. **Obrigatória quando `status` é `ABERTO`.** |
| `urlOficial` | `string \| null` | não | `@URL` · máx. 500 caracteres · esquema `http` ou `https`. |
| `disciplinaIds` | `uuid[]` | sim | Pode vir vazia. Cada id deve existir em `disciplina` e não estar excluído (CA-08). Repetidos são reduzidos a um. |

```text
{
  "nome": "TRF 1ª Região — Analista Judiciário",
  "status": "ABERTO",
  "vagas": 320,
  "bancaId": "3f8e1d2c-9a44-4d15-b2e7-77c1a0b3e551",
  "orgaoId": "c41b7e90-2d68-4f0a-9c15-6e8b3a2d4477",
  "prazoInscricao": "2026-08-30",
  "urlOficial": "https://www.cebraspe.org.br/concursos/trf1_26_analista",
  "disciplinaIds": [
    "6a1c0f2e-1b93-4c07-8f2d-51a9c7e40b28",
    "d20b4a75-8c31-4e6f-91a0-3f7d5b8c2e19",
    "5e9f3b12-7d40-4a88-b6c3-90e2f1a7d5c6"
  ]
}
```

#### `ErroApi` — envelope de erro (reutilizado, `application/problem+json`)

| Código | HTTP | Significado |
| --- | --- | --- |
| `DADOS_INVALIDOS` | `422` | Bean Validation, `prazoInscricao` ausente em edital aberto, ou id de disciplina, banca ou órgão inexistente. Sempre com `fieldErrors`. |
| `NOME_EM_USO` | `409` | Já existe edital não excluído com o mesmo nome, ignorando maiúsculas. |
| `EDITAL_NAO_ENCONTRADO` | `404` | Id inexistente ou já excluído logicamente. |
| `DISCIPLINA_EM_USO` | `409` | Devolvido por `DELETE /admin/disciplinas/{id}` quando um edital referencia a disciplina. |
| `ACESSO_NEGADO` | `403` | Perfil abaixo de administrador tentando escrever (CA-10). |

```text
{
  "code": "DADOS_INVALIDOS",
  "message": "Verifique os campos destacados.",
  "fieldErrors": {
    "prazoInscricao": "Informe o prazo de inscrição para um edital aberto.",
    "disciplinaIds": "Uma das disciplinas selecionadas não existe mais na árvore de matérias."
  }
}
```

#### Esquema de banco — `V7__editais.sql`

Migração versionada e imutável em `backend/src/main/resources/db/migration`. `V1__base_acesso.sql` criou `usuario`; `V2__banco_questoes.sql` criou `disciplina`, `banca`, `orgao` e `auditoria`. **O número final é confirmado na entrega**: `prd-simulados` pertence à mesma fase F4 e a numeração de F2 e F3 ainda não foi aplicada — esta migração assume a próxima versão livre, sem jamais editar as anteriores.

| Tabela | Colunas principais | Observações |
| --- | --- | --- |
| `edital` | `id`, `nome`, `status`, `vagas`, `banca_id`, `orgao_id`, `prazo_inscricao`, `url_oficial`, `excluido_em`, `criado_em`, `atualizado_em` | Nome único entre não excluídos, sem diferenciar maiúsculas. `check` de invariante para edital aberto. |
| `edital_disciplina` | `edital_id`, `disciplina_id` | PK composta — o par repetido é impossível por construção. `on delete cascade` a partir de `edital`; sem cascata a partir de `disciplina`, para que a FK seja a rede de segurança de CA-08. |

```sql
create table edital (
  id              uuid primary key default gen_random_uuid(),
  nome            text not null,
  status          text not null check (status in ('ABERTO','PREVISTO','ENCERRADO')),
  vagas           integer check (vagas >= 0 and vagas <= 999999),
  banca_id        uuid references banca(id),
  orgao_id        uuid references orgao(id),
  prazo_inscricao date,
  url_oficial     text check (url_oficial is null or url_oficial ~ '^https?://'),
  excluido_em     timestamptz,
  criado_em       timestamptz not null default now(),
  atualizado_em   timestamptz not null default now(),
  constraint edital_aberto_com_prazo check (
    status <> 'ABERTO' or prazo_inscricao is not null
  )
);
create unique index edital_nome_uk on edital (lower(nome)) where excluido_em is null;
create index edital_status_idx on edital (status, prazo_inscricao) where excluido_em is null;

create table edital_disciplina (
  edital_id     uuid not null references edital(id) on delete cascade,
  disciplina_id uuid not null references disciplina(id),
  primary key (edital_id, disciplina_id)
);
create index edital_disciplina_disciplina_idx on edital_disciplina (disciplina_id);
```

> **`edital_aberto_com_prazo`** é a rede de segurança no banco para o caso de borda 2: sem ela, `prazoVencido` teria um terceiro estado (aberto e sem prazo) que a tela administrativa não saberia representar. A regra vive no `EditalService`; o `check` impede que qualquer caminho futuro — script, migração, endpoint novo — crie a linha ambígua. Mesmo padrão de `questao_publicada_completa` em `prd-banco-de-questoes`.

> **Sementes.** `db/migration-dev/V905__seed_editais_dev.sql` cria quatro editais de exemplo — um por status mais um aberto com prazo vencido, para exercitar a borda 2 — e entra apenas no perfil `dev`, pelo `spring.flyway.locations` já configurado em `prd-acesso-e-conta`. Depende das disciplinas e bancas semeadas por `V901`. **O número da série `V900+` também é confirmado na entrega**, pelo mesmo motivo da migração principal. Não há semente de produção: os ao menos 10 editais do objetivo do PRD são cadastrados pela própria tela administrativa, que é o ponto da funcionalidade.

### Endpoints da API

Base `/api/v1`. Seis rotas. A §6 da `TECH-SPEC.md` prevê apenas a linha genérica `CRUD /admin/editais` com perfil "professor+ / admin+"; **as duas rotas de leitura do estudante (`GET /editais`) são acréscimo e o perfil administrativo é uma correção** — o PRD exige administrador ou superior para escrever, não professor. A §6 é atualizada na mesma entrega, conforme a regra 4 do `AGENTS.md`.

#### Visão geral

| Método | Rota | Perfil | Descrição |
| --- | --- | --- | --- |
| `GET` | `/api/v1/editais` | autenticado | Editais agrupados por status, com as disciplinas exigidas (RF-17). |
| `GET` | `/api/v1/admin/editais` | admin+ | Lista administrativa com `prazoVencido` e contagem de disciplinas (RF-22). |
| `GET` | `/api/v1/admin/editais/{id}` | admin+ | Edital completo para o formulário. |
| `POST` | `/api/v1/admin/editais` | admin+ | Cria o edital; passa a constar na visão do estudante (CA-04, CA-05). |
| `PUT` | `/api/v1/admin/editais/{id}` | admin+ | Atualiza o agregado inteiro, disciplinas incluídas (CA-06). |
| `DELETE` | `/api/v1/admin/editais/{id}` | admin+ | Exclusão lógica; some da visão do estudante (CA-07). |

Nenhuma rota é paginada: o universo é de dezenas de editais, e o envelope `{content, page, size, totalElements}` da §6 só se aplica a listagens que crescem sem limite — decisão registrada em *Principais decisões*.

---

#### `GET /api/v1/editais`

Consulta somente leitura do estudante, agrupada por status (RF-17).

**Parâmetros de consulta** — nenhum. A lista é a mesma para todos os usuários autenticados; "meus editais" por estudante está explicitamente fora do escopo do PRD.

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| `200` | `EditaisAgrupados` | Sempre, inclusive sem nenhum edital cadastrado. |
| `401` | `ErroApi` | Sem token ou token expirado. |

**Exemplo — sucesso (CA-01, CA-02, CA-03)**

```http
GET /api/v1/editais
```

Corpo conforme o exemplo de `EditaisAgrupados` em *Modelos de dados*.

**Exemplo — nenhum edital publicado (CA-09)**

```http
GET /api/v1/editais
```

```text
{ "abertos": [], "previstos": [], "encerrados": [] }
```

> A tela exibe o estado vazio explicativo — "Nenhum edital acompanhado no momento." — e não uma lista em branco. `EditaisStore.vazio()` é o `computed` sobre os três comprimentos, de modo que a decisão não se espalha pelo template.

> **Sem cache em lugar nenhum.** A resposta sai com `Cache-Control: no-store`, e não há `@Cacheable`, `ETag` nem CDN no caminho. É o que sustenta CA-05: recarregar a tela depois de uma edição administrativa mostra o dado novo, sem etapa de publicação.

---

#### `GET /api/v1/admin/editais`

Tabela de trabalho da administração (RF-22), com o sinal de inconsistência de prazo.

**Parâmetros de consulta** — nenhum. Ordenação fixa: `ABERTO`, `PREVISTO`, `ENCERRADO`; dentro de cada status, pelo prazo mais próximo e depois por nome.

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| `200` | `EditalAdminResumo[]` | Sempre; array vazio quando não houver editais. |
| `401` | `ErroApi` | Sem token. |
| `403` | `ErroApi` (`ACESSO_NEGADO`) | Perfil `ESTUDANTE` ou `PROFESSOR` (CA-10). |

**Exemplo — sucesso com um prazo vencido (Borda 2)**

```http
GET /api/v1/admin/editais
```

Corpo conforme o exemplo de `EditalAdminResumo` em *Modelos de dados*.

> A linha com `prazoVencido: true` recebe na tela o ícone `ph-warning-circle` e o texto "prazo vencido, revisar" — rótulo textual além da cor, como o PRD exige. A ação sugerida é abrir o formulário e mudar o status para `ENCERRADO`; nada acontece automaticamente.

---

#### `POST /api/v1/admin/editais`

Cria o edital. Não há rascunho nem etapa de publicação: criado é visível (CA-04, CA-05).

**Corpo** — `RequisicaoEdital`, conforme *Modelos de dados*.

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| `201` | `EditalDetalhe` | Criado; `Location` aponta para `/api/v1/admin/editais/{id}`. |
| `401` | `ErroApi` | Sem token. |
| `403` | `ErroApi` (`ACESSO_NEGADO`) | Perfil `ESTUDANTE` ou `PROFESSOR` (CA-10). |
| `409` | `ErroApi` (`NOME_EM_USO`) | Já existe edital não excluído com o mesmo nome. |
| `422` | `ErroApi` (`DADOS_INVALIDOS`) | Campo inválido, prazo ausente em edital aberto, ou referência inexistente. |

**Exemplo — edital aberto sem prazo**

```http
POST /api/v1/admin/editais
```

```text
{
  "code": "DADOS_INVALIDOS",
  "message": "Verifique os campos destacados.",
  "fieldErrors": { "prazoInscricao": "Informe o prazo de inscrição para um edital aberto." }
}
```

**Exemplo — disciplina que não está mais na árvore (CA-08)**

```http
POST /api/v1/admin/editais
```

```text
{
  "code": "DADOS_INVALIDOS",
  "message": "Verifique os campos destacados.",
  "fieldErrors": { "disciplinaIds": "Uma das disciplinas selecionadas não existe mais na árvore de matérias." }
}
```

> O seletor de disciplinas só oferece o que veio de `GET /admin/disciplinas`, então este erro só aparece em corrida — alguém arquivou ou excluiu a disciplina enquanto o formulário estava aberto. É por isso que a validação existe no servidor mesmo com a interface fechada: CA-08 é uma garantia de dados, não de tela.

---

#### `PUT /api/v1/admin/editais/{id}`

Atualiza o agregado inteiro. É o endpoint da migração de agrupamento do caso de borda 1 (CA-06): mudar `status` e `prazoInscricao` basta.

**Corpo** — `RequisicaoEdital`. O conjunto `disciplinaIds` **substitui** o anterior; enviar uma lista menor desvincula as ausentes.

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| `200` | `EditalDetalhe` | Atualizado. |
| `403` | `ErroApi` (`ACESSO_NEGADO`) | Perfil abaixo de administrador (CA-10). |
| `404` | `ErroApi` (`EDITAL_NAO_ENCONTRADO`) | Id inexistente ou já excluído. |
| `409` | `ErroApi` (`NOME_EM_USO`) | Nome já usado por outro edital. |
| `422` | `ErroApi` (`DADOS_INVALIDOS`) | Mesmas regras do `POST`. |

**Exemplo — previsto vira aberto (CA-06)**

```http
PUT /api/v1/admin/editais/1d7c4b93-6e28-4f51-a90d-8b3e2c1f7a05
```

```text
{
  "nome": "SEFAZ-RS — Auditor Fiscal",
  "status": "ABERTO",
  "vagas": 90,
  "bancaId": "9b2d6f41-3a87-4c92-8e05-1f6a4d7b3c28",
  "orgaoId": null,
  "prazoInscricao": "2026-10-15",
  "urlOficial": null,
  "disciplinaIds": ["72e5a90c-4f13-4b6d-8a27-c1905e3f8d64", "a83f10d5-6b29-4e74-95c8-2d0b7f4a1e93"]
}
```

> Na leitura seguinte de `GET /editais`, o edital sai de `previstos` e entra em `abertos`, ordenado pelo novo prazo. O cliente não faz nada: o agrupamento é do servidor.

---

#### `DELETE /api/v1/admin/editais/{id}`

Exclusão lógica (CA-07): `excluido_em` é preenchido e a linha permanece na base, com o vínculo em `edital_disciplina` intacto.

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| `204` | — | Excluído; some de `GET /editais` e de `GET /admin/editais`. |
| `403` | `ErroApi` (`ACESSO_NEGADO`) | Perfil abaixo de administrador (CA-10). |
| `404` | `ErroApi` (`EDITAL_NAO_ENCONTRADO`) | Id inexistente ou já excluído. |

> A exclusão é precedida do `confirmacao-dialog` na interface, como o PRD exige para ações destrutivas. O nome fica livre para reuso: o índice único é parcial, `where excluido_em is null`.

---

#### `GET /api/v1/admin/editais/{id}`

Carrega o edital para o formulário.

**Respostas**

| Status | Corpo | Quando |
| --- | --- | --- |
| `200` | `EditalDetalhe` | Encontrado. |
| `403` | `ErroApi` (`ACESSO_NEGADO`) | Perfil abaixo de administrador. |
| `404` | `ErroApi` (`EDITAL_NAO_ENCONTRADO`) | Id inexistente ou já excluído. |

---

## Pontos de integração

Nenhuma integração externa. A funcionalidade é inteiramente interna — PostgreSQL e o que as fases anteriores já entregaram. Não há chamada de rede a terceiros, e portanto não há timeout, retentativa ou idempotência de integração a especificar. A `urlOficial` é um dado exibido como link, nunca requisitado pelo servidor.

Integrações internas, todas já especificadas em outras TechSpecs:

| Ponto | Origem | Uso aqui |
| --- | --- | --- |
| `SegurancaConfig` e o claim `perfil` do JWT | `prd-acesso-e-conta` | `@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")` nas cinco rotas de escrita e na leitura administrativa (CA-10). |
| `ManipuladorErrosGlobal` e `ErroApi` | `prd-acesso-e-conta` | Envelope `problem+json` dos cinco códigos de erro. **Nenhum tipo novo é acrescentado** — os três de `prd-banco-de-questoes` bastam. |
| `disciplina` (RF-20) | `prd-banco-de-questoes` | Alvo do `@ManyToMany` de disciplinas exigidas (CA-08). |
| `banca` e `orgao` (RF-37) | `prd-banco-de-questoes` | FKs de banca e órgão; os combos do formulário consomem `GET /admin/bancas` e `/admin/orgaos`. |
| `RegistradorAuditoria` | `prd-banco-de-questoes` | Auditoria de criar, editar e excluir edital (restrição do PRD). |
| `TaxonomiaService.excluirDisciplina` | `prd-banco-de-questoes` | **Alteração de mão dupla:** passa a consultar `EditalConsultaVinculos` antes de excluir, e recusa com `DISCIPLINA_EM_USO` quando há edital vinculado. É o único ponto em que esta funcionalidade modifica código de outra. |
| `ZonaAplicacao` (`America/Sao_Paulo`) | `prd-cadernos-e-desempenho` | Fuso do "hoje" usado por `VerificadorPrazoEdital`. Se ainda não existir na entrega, nasce aqui com a mesma assinatura. |
| `shared/ui/confirmacao-dialog`, `shared/ui/estado-vazio` | `prd-banco-de-questoes`, `prd-cadernos-e-desempenho` | Exclusão com confirmação (CA-07) e estado vazio (CA-09). |

## Abordagem de testes

Meta de cobertura: **mínimo de 80%**, conforme `.agents/rules/tests.md` §1, que prevalece sobre o texto de 70% das TechSpecs de F1. Ferramentas: JUnit Jupiter, AssertJ, Mockito e Testcontainers 2.0.5 (versões gerenciadas pelo BOM do Spring Boot 4.1.1); Vitest e Playwright no frontend.

Pela tabela de criticidade de `tests.md` §1, esta funcionalidade é de **prioridade baixa** — "cadastros administrativos simples". O esforço vai para o que de fato tem regra: a autorização por perfil (CA-10), a invariante do edital aberto, o vínculo com a taxonomia (CA-08) e a propagação imediata (CA-05). Mapeamento DTO trivial recebe o essencial, não uma bateria.

### Testes de unidade

| ID | Nome do caso de teste | Critérios de aceitação | Resultado esperado |
| --- | --- | --- | --- |
| TU-01 | `VerificadorPrazoEdital` com edital aberto e prazo de ontem | Borda 2 | `true`; o `Clock` é fixo, não `LocalDate.now()`. |
| TU-02 | `VerificadorPrazoEdital` com edital aberto e prazo de hoje | Borda 2 | `false` — o último dia de inscrição ainda vale. |
| TU-03 | `VerificadorPrazoEdital` com edital aberto e prazo futuro | Borda 2 | `false`. |
| TU-04 | `VerificadorPrazoEdital` com edital encerrado e prazo de ontem | Borda 2 | `false` — só `ABERTO` é inconsistente; encerrado com prazo passado é o estado correto. |
| TU-05 | `EditalService.criar` com todos os campos preenchidos | CA-04 | Persiste com status, vagas, banca, órgão, prazo e as três disciplinas vinculadas; uma linha em `auditoria`. |
| TU-06 | `EditalService.criar` com status aberto e sem prazo | CA-04 | Lança `DadosInvalidosException` com `fieldErrors.prazoInscricao`; nada persistido. |
| TU-07 | `EditalService.criar` com nome já usado | CA-04 | Lança `ConflitoException` `NOME_EM_USO`. |
| TU-08 | `EditalService.criar` com `bancaId` inexistente | CA-04 | Lança `DadosInvalidosException` com `fieldErrors.bancaId`. |
| TU-09 | `EditalService.criar` com URL sem esquema `http` | CA-04 | Lança `DadosInvalidosException` com `fieldErrors.urlOficial`. |
| TU-10 | `EditalService.atualizar` mudando status previsto para aberto | CA-06 | Só `status` e `prazoInscricao` mudam; nome, vagas, banca e disciplinas permanecem; auditoria registrada. |
| TU-11 | `EditalService.atualizar` trocando o conjunto de disciplinas | CA-08 | O conjunto final é exatamente o enviado — as ausentes são desvinculadas, sem apagar `disciplina`. |
| TU-12 | `EditalService.excluir` de edital existente | CA-07 | Marca `excluidoEm`; nenhum `delete` físico; auditoria registrada. |
| TU-13 | `EditalService.excluir` de id inexistente | CA-07 | Lança `RecursoNaoEncontradoException`. |
| TU-14 | `ResolvedorDisciplinasEdital` com id que não existe | CA-08 | Lança `DadosInvalidosException` com `fieldErrors.disciplinaIds`. |
| TU-15 | `ResolvedorDisciplinasEdital` com disciplina excluída logicamente | CA-08 | Recusa igual ao id inexistente — a árvore corrente é a única fonte. |
| TU-16 | `ResolvedorDisciplinasEdital` com ids repetidos | CA-08 | Devolve o conjunto sem duplicata; uma única consulta ao repositório. |
| TU-17 | `ResolvedorDisciplinasEdital` com lista vazia | CA-08 | Conjunto vazio, sem erro e sem consulta ao repositório. |
| TU-18 | `ConsultaEditaisService.agrupados` distribui os três status | CA-01 | Cada edital cai no grupo do seu status; nenhum aparece em dois grupos. |
| TU-19 | `ConsultaEditaisService` ordena os abertos pelo prazo mais próximo | CA-01 | O de prazo mais próximo vem primeiro. |
| TU-20 | `ConsultaEditaisService` ordena previstos por nome e encerrados pelo prazo mais recente | CA-01 | Ordem determinística nos três grupos, com `ORDER BY` explícito. |
| TU-21 | `ConsultaEditaisService.agrupados` sem nenhum edital | CA-09 | Três listas vazias, nunca `null`. |
| TU-22 | `ConsultaEditaisService.listarParaAdministracao` marca o prazo vencido | Borda 2 | `prazoVencido` verdadeiro só na linha aberta com prazo passado. |
| TU-23 | `EditaisStore` expõe `vazio()` quando os três grupos vêm vazios (Vitest) | CA-09 | `vazio()` verdadeiro; a página renderiza o estado vazio. |
| TU-24 | `EditalCard` formata vagas e prazo em pt-BR (Vitest) | CA-03 | `1500` vira `1.500` e `2026-08-30` vira `30/08/2026`. |
| TU-25 | `EditalCard` com vagas e prazo nulos (Vitest) | CA-03 | Exibe "Vagas a definir" e "Data a definir", nunca `null` nem traço mudo. |
| TU-26 | `EditalFormStore` exige prazo ao escolher status aberto (Vitest) | CA-04 | `exigePrazo()` verdadeiro e o envio bloqueado enquanto o campo estiver vazio. |
| TU-27 | `EditalFormStore` mapeia 422 campo a campo (Vitest) | CA-04 | Cada `fieldError` marca seu `FormControl`; os demais campos ficam limpos e preenchidos. |
| TU-28 | `EditalFormStore` mapeia 409 para o campo nome (Vitest) | CA-04 | `NOME_EM_USO` vira erro inline em `nome`, não um toast genérico. |
| TU-29 | `DisciplinasSeletor` só emite ids vindos da árvore (Vitest) | CA-08 | Não há entrada de texto livre; digitar filtra a lista, nunca cria item. |
| TU-30 | `EditaisAdminStore` destaca a linha com prazo vencido (Vitest) | Borda 2 | A linha recebe o aviso textual; as demais, não. |

Componentes cobertos: `VerificadorPrazoEdital`, `ResolvedorDisciplinasEdital`, `EditalService`, `ConsultaEditaisService`, `EditaisStore`, `EditalCard`, `EditalFormStore`, `DisciplinasSeletor` e `EditaisAdminStore`. Serviços puros com JUnit + Mockito, sem contexto Spring. `VerificadorPrazoEdital` não tem dependência alguma além do `Clock` — é a razão de ele existir separado do serviço, e o `Clock.fixed` elimina a instabilidade que `LocalDate.now()` traria na virada do dia (`tests.md` §2, *Repeatable*). Mocks apenas na fronteira: `EditalRepository`, `DisciplinaRepository` e `RegistradorAuditoria`.

### Testes de integração

| ID | Nome do caso de teste | Critérios de aceitação | Resultado esperado |
| --- | --- | --- | --- |
| TI-01 | Migração em base limpa (`@DataJpaTest`) | — | Flyway aplica sem erro; `edital`, `edital_disciplina`, os índices e os três `check` criados. |
| TI-02 | `check edital_aberto_com_prazo` (`@DataJpaTest`) | Borda 2 | `update` direto para `ABERTO` sem prazo é rejeitado pelo banco. |
| TI-03 | Índice único parcial de nome (`@DataJpaTest`) | CA-04 | Nome duplicado é recusado; após exclusão lógica, o mesmo nome é aceito de novo. |
| TI-04 | PK composta de `edital_disciplina` (`@DataJpaTest`) | CA-08 | Vincular a mesma disciplina duas vezes é impossível. |
| TI-05 | `GET /editais` com editais nos três status | CA-01 | 200 com `abertos`, `previstos` e `encerrados` preenchidos e ordenados. |
| TI-06 | `GET /editais` carrega disciplinas, banca e órgão numa consulta | CA-02 | Cada edital traz suas disciplinas; o contador de consultas confirma ausência de N+1. |
| TI-07 | `GET /editais` devolve vagas, prazo e banca no payload | CA-03 | `vagas`, `prazoInscricao` ISO e `banca.nome` presentes; a formatação é do cliente. |
| TI-08 | `GET /editais` em base sem editais | CA-09 | 200 com os três grupos vazios, não 404. |
| TI-09 | `GET /editais` sem token | CA-10 | 401; leitura é aberta a autenticado, não a anônimo. |
| TI-10 | `GET /editais` responde `Cache-Control: no-store` | CA-05 | Cabeçalho presente — nenhuma camada intermediária pode servir dado velho. |
| TI-11 | `POST /admin/editais` como `ADMIN` | CA-04 | 201 com `Location`; o edital consta do `GET /editais` seguinte, sem etapa de publicação. |
| TI-12 | `POST /admin/editais` como `ESTUDANTE` | CA-10 | 403 `ACESSO_NEGADO` decidido no servidor. |
| TI-13 | `PUT /admin/editais/{id}` como `PROFESSOR` | CA-10 | 403 — professor edita questão, não edital. |
| TI-14 | `DELETE /admin/editais/{id}` como `ESTUDANTE` | CA-10 | 403; a linha permanece intacta na base. |
| TI-15 | `PUT` mudando previsto para aberto e novo `GET /editais` | CA-06 | O edital sai de `previstos` e entra em `abertos`, na posição do novo prazo. |
| TI-16 | `DELETE` seguido de `GET /editais` | CA-07 | 204; o edital some da visão do estudante e da administrativa, com `excluido_em` preenchido e a linha na base. |
| TI-17 | `POST` com `disciplinaIds` contendo id inexistente | CA-08 | 422 `DADOS_INVALIDOS` com `fieldErrors.disciplinaIds`; nada persistido. |
| TI-18 | `POST` com nome duplicado ignorando maiúsculas | CA-04 | 409 `NOME_EM_USO`; o índice único parcial fecha a corrida entre requisições simultâneas. |
| TI-19 | `GET /admin/editais` com um aberto de prazo vencido | Borda 2 | `prazoVencido` verdadeiro só nessa linha; o `status` no banco permanece `ABERTO`. |
| TI-20 | `GET /admin/editais/{id}` de edital excluído | CA-07 | 404 `EDITAL_NAO_ENCONTRADO`. |
| TI-21 | Auditoria das três ações administrativas | Auditoria (PRD) | Criar, editar e excluir geram linha em `auditoria` com ator, ação, entidade e `payload` do delta. |
| TI-22 | `DELETE /admin/disciplinas/{id}` com edital vinculado | CA-08 | 409 `DISCIPLINA_EM_USO`; a disciplina continua na árvore e o edital, íntegro. |
| TI-23 | `PUT /admin/disciplinas/{id}` renomeando disciplina vinculada | CA-08 | 200; `GET /editais` passa a exibir o novo nome, com `disciplina_id` inalterado. |

Componentes testados em conjunto: controller + segurança + serviço + repositório + Flyway sobre PostgreSQL 18 real via Testcontainers (`@ServiceConnection`), sem H2 — o índice único parcial, o `check` de invariante e a PK composta só existem no Postgres. Fatia mais estreita por cenário: `@DataJpaTest` em TI-01…TI-04, `@WebMvcTest` com `MockMvcTester` nos cenários de contrato e autorização, e `@SpringBootTest` apenas em TI-11, TI-15, TI-16 e TI-21, que atravessam escrita e leitura. Dados criados por *builders* no pacote de teste, com nome isolado por `UUID.randomUUID()` para não colidir no índice único. O contexto Spring é compartilhado entre as classes de integração.

### Testes E2E

| ID | Nome do caso de teste | Critérios de aceitação | Resultado esperado |
| --- | --- | --- | --- |
| E2E-01 | Estudante abre Editais pelo menu | CA-01 | Três seções com rótulo textual de status; cada edital no seu agrupamento. |
| E2E-02 | Abrir um edital e ver as disciplinas exigidas | CA-02 | As disciplinas aparecem como `.tag`, sem nova requisição de rede. |
| E2E-03 | Conferir banca, vagas e prazo de um edital aberto | CA-03 | `1.500` e `30/08/2026` — formato brasileiro de número e data. |
| E2E-04 | Administrador cadastra um edital completo | CA-04, CA-05 | Salvo pelo formulário; ao abrir a tela do estudante em seguida, o edital já está lá. |
| E2E-05 | Administrador corrige um dado e o estudante recarrega | CA-05 | O valor novo aparece na recarga, sem qualquer ação de publicação. |
| E2E-06 | Administrador muda um edital de previsto para aberto | CA-06 | Na tela do estudante o edital migra de agrupamento. |
| E2E-07 | Administrador exclui um edital com confirmação | CA-07 | Diálogo explícito; depois de confirmar, o edital some da lista do estudante. |
| E2E-08 | Selecionar disciplinas no formulário | CA-08 | A seleção vem da árvore de matérias; não há campo que aceite texto digitado como disciplina. |
| E2E-09 | Base sem nenhum edital | CA-09 | Estado vazio explicativo na tela do estudante, sem erro no console. |
| E2E-10 | Estudante tenta alcançar a administração | CA-10 | O menu administrativo não aparece e `/admin/editais` redireciona; a chamada direta à API devolve 403, verificada na mesma suíte. |
| E2E-11 | Lista administrativa com prazo vencido | Borda 2 | A linha exibe o aviso textual de inconsistência e o status continua "Aberto" até correção manual. |
| E2E-12 | Percurso por teclado e contraste dos rótulos de status | Experiência do usuário | `Tab` e `Enter` operam as duas telas com foco visível; os rótulos de status têm texto além da cor e contraste ≥ 4,5:1. |

Execução com Playwright contra o SPA e a API reais, com PostgreSQL em contêiner e a semente de `dev`. Os seletores saem da marcação do protótipo traduzida 1:1 (`.card`, `.tag`, `.table`, `.field`, `.input`, `.btn-primary`, `.dialog`). E2E-01…E2E-03 e E2E-09 dependem da tela do estudante, e E2E-04…E2E-08 e E2E-11 da tela administrativa — ambas desenhadas no passo 1 da ordem de construção. A verificação automatizada de acessibilidade e contraste (E2E-12) roda sobre as duas telas.

## Sequenciamento do desenvolvimento

### Ordem de construção

1. **Design: desenhar as duas telas no protótipo** — acrescentar ao `Certame.dc.html`, com componentes Nocturne existentes, a tela `EDITAIS` do estudante (três seções por status, card com rótulo textual, banca, órgão, vagas, prazo e tags de disciplina, mais o estado vazio), o item "Editais" na navegação de estudo, e a tela `ADMIN: EDITAIS` (tabela com aviso de prazo vencido e formulário de página cheia com campos estruturados e multisseleção de disciplinas), substituindo o card e o modal atuais de `ADMIN: MATÉRIAS & EDITAIS`. Primeiro porque a regra 1 do `AGENTS.md` impede que qualquer marcação nasça direto no Angular, e a aprovação visual libera o passo 8.
2. **Migração e persistência** — `V7__editais.sql`, a semente de `dev`, a entidade `Edital`, o enum `StatusEdital` e o `EditalRepository`, com TI-01…TI-04. Antes de qualquer regra, porque o modelo sustenta o resto.
3. **Regras puras** — `VerificadorPrazoEdital` e `ResolvedorDisciplinasEdital`, com TU-01…TU-04 e TU-14…TU-17. São as duas únicas regras de verdade da funcionalidade e não dependem de Spring.
4. **Serviço administrativo** (RF-22) — `EditalService` com as três operações, a invariante do edital aberto e a auditoria, com TU-05…TU-13.
5. **Consulta do estudante** (RF-17) — `ConsultaEditaisService` e o agrupamento ordenado, com TU-18…TU-22.
6. **Controllers** — `EditalController` e `EditalAdminController`, autorização por `@PreAuthorize` e `Cache-Control: no-store`, com TI-05…TI-21.
7. **Vínculo de volta com a taxonomia** — `EditalConsultaVinculos` e o ajuste em `TaxonomiaService.excluirDisciplina`, com TI-22 e TI-23. Depois dos passos 2 e 4 porque precisa da tabela e do serviço prontos, e antes do frontend porque muda uma mensagem que a tela de matérias já exibe.
8. **Frontend** — `edital-api`, `modelos-edital`, `EditaisStore`, `EditaisPage`, `EditalCard`, `EditaisAdminStore`, `EditaisAdminPage`, `EditalFormPage`, `DisciplinasSeletor`, o item de menu no shell e as rotas sob `authGuard` e `perfilGuard`, tudo em tradução 1:1 do protótipo aprovado no passo 1, com TU-23…TU-30.
9. **E2E e endurecimento** — E2E-01…E2E-12, verificação da cobertura de 80% por `./mvnw verify` e `npm run test:coverage`, e revisão de acessibilidade e contraste.
10. **Atualização dos documentos** — §6 da `TECH-SPEC.md` com as duas rotas de leitura e a correção do perfil administrativo, e §5 com as duas tabelas. Regra 4 do `AGENTS.md`; entra na mesma entrega, não depois.

### Dependências técnicas

| Dependência | Situação | Impacto |
| --- | --- | --- |
| `prd-acesso-e-conta` (`SegurancaConfig`, `perfilGuard`, shell, `styles.css`) | **Implementado** | Nenhum bloqueio. O shell recebe apenas o item de menu novo. |
| `prd-banco-de-questoes` — tabelas `disciplina`, `banca`, `orgao` e `auditoria` | Especificado, ainda não implementado | **Bloqueia tudo.** Sem `disciplina` não há FK de `edital_disciplina`, e CA-08 é o coração da funcionalidade. |
| `prd-banco-de-questoes` — `RegistradorAuditoria`, `ConflitoException`, `RecursoNaoEncontradoException`, `DadosInvalidosException`, `TaxonomiaService` | Especificado, ainda não implementado | Bloqueia os passos 4 e 7. Nenhum deles é reimplementado aqui. |
| `prd-banco-de-questoes` — `GET /admin/disciplinas`, `/admin/bancas`, `/admin/orgaos` e `shared/ui/confirmacao-dialog` | Especificado | Bloqueia o passo 8: o formulário não tem de onde carregar os combos nem o diálogo de exclusão. |
| `prd-cadernos-e-desempenho` — `shared/ui/estado-vazio` e `ZonaAplicacao` | Especificado (F3, anterior a esta) | Não bloqueia: se não estiverem prontos na entrega, nascem aqui com a mesma assinatura. |
| Numeração de migração compartilhada com `prd-simulados` (mesma fase F4) | Em aberto | Não bloqueia: a versão livre é conferida no passo 2 e nenhuma migração aplicada é editada. |
| PostgreSQL 18 e Docker | Necessários para Testcontainers e para o Compose de `dev` | Bloqueiam a suíte de integração; o índice único parcial e os `check` não têm equivalente em banco em memória. |
| Playwright | Ausente do `package.json`; previsto em `prd-acesso-e-conta` | Bloqueia o passo 9. |
| Aprovação visual das duas telas novas no protótipo | A obter no passo 1 | Bloqueia o passo 8. É a dependência mais provável de atrasar, porque são telas inéditas, não lacunas de telas existentes. |

## Monitoramento e observabilidade

Sobre Actuator + Micrometer/Prometheus e logs JSON com `traceId`, conforme a §9 da `TECH-SPEC.md`.

| Sinal | Tipo | Uso |
| --- | --- | --- |
| `certame.edital.consulta` | *timer* (p95), sem tag de usuário | Volume e latência de `GET /editais`. A meta do PRD de ≥ 25% dos estudantes ativos por mês é de **estudantes distintos**, que o Micrometer não mede sem explodir a cardinalidade: ela sai do log estruturado com `usuarioId`, agregado no produto. |
| `certame.edital.total` | *gauge*, tag `status` | Acompanha o objetivo de ao menos 10 editais cadastrados e classificados antes do lançamento, e mostra a distribuição entre aberto, previsto e encerrado. |
| `certame.edital.prazo.inconsistente` | *gauge* | Quantos editais estão `ABERTO` com prazo vencido. É a métrica do objetivo "zero informação obsoleta visível"; qualquer valor acima de zero é trabalho pendente da administração, e um alerta simples cobre o caso de borda 2 sem depender de alguém abrir a tela. |
| `certame.edital.alteracao` | contador, tag `acao` (`criar`, `editar`, `excluir`) | Ritmo de manutenção da base — é o sinal de que a informação está sendo mantida atual, que é a promessa da funcionalidade. |
| `certame.edital.escrita.recusada` | contador, tag `motivo` (`nome_em_uso`, `prazo_ausente`, `disciplina_invalida`, `acesso_negado`) | Mostra o que mais trava o cadastro; `acesso_negado` recorrente indica interface exibindo ação que o perfil não tem. |
| `/actuator/health` com indicador de banco | *health check* | Prontidão do contêiner; já existente. |

**Eventos de log.** `INFO` para criação, edição e exclusão de edital, sempre com `id`, `nome`, ator e ação. `WARN` para escrita recusada por validação, nome duplicado ou perfil insuficiente, e para exclusão de disciplina recusada por vínculo com edital. `ERROR` apenas para falha de infraestrutura. A tabela `auditoria` é o registro autoritativo das ações administrativas exigido pelo PRD; o log é diagnóstico, não substituto.

## Considerações técnicas

### Principais decisões

- **Agrupamento por status resolvido no servidor**, e não no cliente. `GET /editais` devolve `{abertos, previstos, encerrados}` já ordenados. CA-01 e CA-06 viram propriedades do contrato, testáveis por TI-05 e TI-15 sem abrir um navegador, e CA-09 passa a ser "os três grupos vazios" em vez de um caso especial de tela. O custo é um envelope menos genérico que uma lista plana; o ganho é que a regra de ordenação existe uma vez, no lugar onde há um `ORDER BY`.
- **`status` é sempre manual; nada corrige ninguém automaticamente.** O caso de borda 2 do PRD pede que a inconsistência seja *sinalizada para correção*. Um job que encerrasse editais vencidos pareceria útil e seria errado: pausas, prorrogações e retificações de edital são rotina em concurso público, e o sistema não tem como distinguir "prazo vencido" de "prazo prorrogado ainda não cadastrado". `VerificadorPrazoEdital` só informa.
- **Edital `ABERTO` obriga `prazo_inscricao`**, no serviço e num `check` no banco. Sem essa invariante, `prazoVencido` teria três estados em vez de dois e a tela administrativa precisaria representar "aberto sem prazo" — que não é nem consistente nem inconsistente. A contrapartida está em *Riscos conhecidos*.
- **Banca e órgão como FK para o catálogo** (decisão do usuário), não texto livre. É a mesma consistência taxonômica que o PRD exige das disciplinas, e evita "Cebraspe", "CEBRASPE" e "Cespe" na mesma base. O administrador cadastra uma banca nova pela tela de catálogo (RF-37) antes de usá-la aqui — um passo a mais, pago uma vez por banca.
- **Órgão e URL oficial acrescentados ao modelo do PRD** (decisão do usuário). `orgao_id` separa "TRF 1ª Região" do nome livre do edital e permitirá agrupar por órgão depois; `url_oficial` é um link para o edital na página da banca, que não conflita com o "anexo do PDF" excluído pelo PRD — o documento continua fora da plataforma.
- **Sem campo de observação livre** (decisão do usuário). O texto em prosa do protótipo ("Inscrições até 30/08 · 320 vagas · Cebraspe") é exatamente o que os campos estruturados substituem; mantê-lo em paralelo criaria duas fontes para o mesmo dado. Editais previstos, que perdem esse texto, mostram "Vagas a definir" e "Data a definir".
- **O payload da lista já é o detalhe; não há `GET /editais/{id}`.** Um edital carrega uma dezena de nomes de disciplina — some ao envelope alguns kilobytes, não um problema. Em troca, CA-02 não custa nem uma requisição, o *deep link* `/editais/:id` funciona selecionando da lista já carregada, e não existe caminho pelo qual a lista e o detalhe divirjam. O escape, se o acervo crescer, está em *Riscos conhecidos*.
- **Sem paginação nas duas listagens.** O envelope `{content, page, size, totalElements}` da §6 vale para listagens que crescem sem limite; editais são dezenas, o objetivo do PRD fala em dez. Paginar aqui acrescentaria estado de tela e uma classe de bugs para resolver um problema que não existe.
- **`Cache-Control: no-store` na leitura, e nenhum `@Cacheable` no caminho.** É a tradução literal da restrição "nenhuma camada de cache pode atrasar a visão do estudante", verificada por TI-10 — um teste que existe justamente para falhar no dia em que alguém acrescentar cache por bons motivos de desempenho.
- **`edital_disciplina` como PK composta, sem coluna `id` própria.** O vínculo não tem atributo nenhum além dos dois lados; uma PK sintética só criaria a possibilidade de duplicata que a PK natural elimina por construção.
- **Exclusão lógica com índice único parcial**, como no resto do projeto. `where excluido_em is null` libera o nome para reuso depois da exclusão, e CA-07 é atendido sem apagar histórico.
- **Excluir disciplina em uso por edital passa a ser recusado**, com alteração no `TaxonomiaService` da F1. A alternativa — deixar a FK estourar — devolveria erro de integridade cru ao administrador de matérias. Como a exclusão de disciplina já é lógica, a checagem também impede o caso silencioso de um edital apontar para disciplina fora da árvore.
- **Nenhum tipo de exceção novo.** `ConflitoException`, `RecursoNaoEncontradoException` e `DadosInvalidosException`, criados em `prd-banco-de-questoes`, cobrem os cinco códigos desta funcionalidade. `ManipuladorErrosGlobal` não é tocado — sinal de que a fundação da F1 está sendo reutilizada como planejado, não contornada.
- **Duas telas em rotas próprias** (decisão do usuário), em vez de um bloco no Início e um modal na tela de matérias. A tela do estudante precisa comportar três agrupamentos e o estado vazio de CA-09; a administrativa precisa comportar multisseleção de disciplinas e o aviso de prazo vencido. Ambas as rotas já constam da §3 da `TECH-SPEC.md` e do `folder-structure.md`.
- **Reactive Forms, não Signal Forms**, mantendo a decisão de `prd-acesso-e-conta` e `prd-banco-de-questoes`. Revisar quando a §2 da `TECH-SPEC.md` for reavaliada, não numa funcionalidade de F4.

### Riscos conhecidos

| Risco | Mitigação |
| --- | --- |
| **O protótipo não tem tela alguma para o estudante (RF-17)** — não há item de menu, rota nem card fora da área administrativa. É a maior lacuna já enfrentada por uma TechSpec deste projeto: não é ajuste de tela existente, é tela inédita. | Desenhada no passo 1 com componentes Nocturne já usados em `CADERNOS` e `SIMULADOS` (`.card`, `.tag`, `.elev-sm`, `.btn`), reusando o padrão de seção com título e contagem. O item de menu usa `ph ph-scroll`, presente no conjunto Phosphor já embarcado. Nenhuma marcação nasce direto no Angular. |
| **O cadastro administrativo do protótipo não comporta o modelo:** o modal tem "Descrição" em texto livre e "Disciplinas cobradas (separadas por vírgula)", enquanto o PRD exige vagas, banca e prazo estruturados e disciplinas vindas da árvore (CA-08). | O modal é substituído no passo 1 por uma tela `ADMIN: EDITAIS` com tabela e formulário de página cheia, no mesmo padrão de `ADMIN: QUESTÕES`. O card em `ADMIN: MATÉRIAS & EDITAIS` vira um atalho para a rota nova. |
| **O aviso de prazo vencido e o estado vazio não existem no protótipo.** | Acrescentados no passo 1: o aviso reusa `ph-warning-circle` e o papel `--color-danger` já registrado por `prd-acesso-e-conta`; o estado vazio reusa `shared/ui/estado-vazio` de F3. |
| **A invariante "aberto exige prazo" pode travar um cadastro legítimo**: um concurso anunciado como aberto cuja data de inscrição ainda não saiu. | O caminho é cadastrar como `PREVISTO` até a data ser divulgada — que é a descrição correta do estado, e é exatamente o caso de borda 1 do PRD. Se o time editorial reportar atrito real, o escape é remover o `check` por migração aditiva e passar a tratar "aberto sem prazo" como uma terceira categoria do sinal administrativo. |
| **O payload de `GET /editais` embute todas as disciplinas de todos os editais.** Com dezenas de editais é irrelevante; com centenas, cresce. | Medido por `certame.edital.consulta`. O escape já mapeado: trocar `disciplinas` por `totalDisciplinas` no resumo e acrescentar `GET /editais/{id}` — mudança aditiva no contrato, sem alterar as telas administrativas. |
| **Editais encerrados acumulam indefinidamente na tela do estudante.** Em dois anos a seção "Encerrados" pode ser a maior das três, sem utilidade. | Aceito na v1: CA-01 exige o agrupamento e o PRD não define retenção. A ordenação já põe os mais recentes primeiro. O escape é limitar a seção aos N mais recentes com um "ver todos" — decisão de produto, não técnica, quando houver dado de uso. |
| **A meta de ≥ 25% dos estudantes ativos abrindo a tela por mês é premissa a calibrar**, como o próprio PRD admite. | Instrumentada desde o primeiro dia por `certame.edital.consulta` e pelo log com `usuarioId`; a distinção de estudantes únicos é agregada fora do Micrometer, para não estourar cardinalidade. |
| **A cobertura mínima de 10 editais cadastrados antes do lançamento depende do time editorial**, não da ferramenta. | Fora do controle técnico, mas instrumentado: `certame.edital.total` por status mostra o ritmo real e se a base está desequilibrada (só previstos, por exemplo). |
| **A numeração da migração colide com `prd-simulados`**, da mesma fase F4, e com F2 e F3, ainda não aplicadas. | O número é conferido contra o que já existe em `db/migration` no passo 2; nenhuma migração aplicada é editada. A migração é aditiva e não toca em tabela existente. |
| **`prazo_inscricao` é `date`, sem hora.** Um edital que encerra às 23h59 e um que encerra ao meio-dia são indistinguíveis, e a virada do dia depende do fuso. | Aceito: o produto é para concursos brasileiros e o PRD determina formatos brasileiros. `VerificadorPrazoEdital` usa `America/Sao_Paulo` por `ZonaAplicacao`, injetada — não `LocalDate.now()` do fuso do servidor —, e TU-02 fixa o comportamento no dia do prazo. |
| **A alteração no `TaxonomiaService` mexe em código entregue na F1** e muda uma mensagem que a tela de matérias já exibe. | Contida: um método a mais na checagem que já existe para questões, com o mesmo código de erro e o mesmo formato de mensagem. TI-22 e TI-23 são escritos junto da alteração, no passo 7, e a tela de matérias não muda. |

### Conformidade com o AGENTS.md e as rules

`AGENTS.md` lido na íntegra. `.agents/rules/code-standards.md` e `.agents/rules/tests.md` lidos na íntegra — são as duas únicas rules do diretório.

| Regra | Como esta especificação atende |
| --- | --- |
| 1 — Frontend é tradução 1:1 do protótipo, tokens Nocturne, sem UI de terceiros | As duas telas são desenhadas no `Certame.dc.html` no passo 1, com componentes Nocturne existentes, e só então traduzidas. Estilo exclusivamente por `var(--*)` de `styles.css`; nenhum valor cru de cor, fonte, espaçamento, raio ou sombra. Nenhuma dependência de UI é adicionada — o seletor de disciplinas é construído com `.input`, `.tag` e `.card`. |
| 2 — Autorização validada no servidor com `@PreAuthorize` | As cinco rotas administrativas exigem `ROLE_ADMIN` ou `ROLE_SUPER_ADMIN` no serviço, mesmo com o menu escondido; a leitura exige apenas autenticação. TI-12, TI-13, TI-14 e E2E-10 provam 403 no servidor com a interface já bloqueando. |
| 3 — Interface em pt-BR; código, tabelas e colunas em português sem acento | Classes `EditalService`, `ConsultaEditaisService`, `VerificadorPrazoEdital`, `ResolvedorDisciplinasEdital`; colunas `nome`, `status`, `vagas`, `prazo_inscricao`, `url_oficial`, `excluido_em`; mensagens de erro e rótulos em pt-BR com acentuação; datas e números em formato brasileiro na tela. |
| 4 — Nenhum endpoint fora do contrato da §6 sem atualizar o documento | `GET /editais` é acréscimo e o perfil de `/admin/editais` é corrigido de "professor+" para "admin+", como o PRD exige. **A §6 e a §5 da `TECH-SPEC.md` são atualizadas na mesma entrega** — passo 10 da ordem de construção e item de *Arquivos relevantes*. |
| 5 — Migração só por Flyway, versionada e imutável | `V7__editais.sql` em `db/migration`, aditiva; semente de `dev` isolada em `db/migration-dev`. Nenhuma migração aplicada é editada; os dois números são conferidos contra o que já existe no passo 2. |
| 6 — `resposta` imutável e Modo Prova | Não se aplica: nenhum caminho desta funcionalidade lê ou escreve em `resposta`. |
| 7, 8, 9 — Flashcards, diálogo na sessão e fila de revisão | Não se aplicam. O vínculo entre edital e estudo dirigido é explicitamente futuro no PRD, e o vínculo com simulado pertence a `prd-simulados`. |
| Toda tarefa cita o requisito | Cada componente, endpoint e caso de teste referencia RF-17, RF-22 ou os CA-01…CA-10 e os casos de borda do PRD. |
| Domínio + testes antes de controller e frontend | A ordem de construção segue exatamente isso: regras puras (3) → serviços (4, 5) → controllers (6) → frontend (8). |
| Cobertura mínima de 80% (`tests.md` §1, prevalece sobre o texto de 70% das TechSpecs de F1) | Declarada na abordagem de testes e verificada no passo 9, por `./mvnw verify` e `npm run test:coverage`. |
| `tests.md` §1 — priorizar pelo risco | A funcionalidade é classificada como prioridade **baixa** ("cadastros administrativos simples") pela própria tabela da rule. O esforço concentra-se onde há regra de verdade: autorização (CA-10), invariante do edital aberto, integridade da taxonomia (CA-08) e ausência de cache (CA-05). Mapeamento DTO recebe o essencial. |
| `tests.md` §2 — FIRST | *Fast*: as duas regras puras e os dois serviços rodam sem Spring e sem banco; Testcontainers só em `*IntegracaoTest`. *Independent*: dados por *builders*, nome isolado por `UUID.randomUUID()` para não colidir no índice único, sem estado estático. *Repeatable*: `Clock.fixed` em TU-01…TU-04 e TU-22, `ORDER BY` explícito nas três consultas de agrupamento, `vi.setSystemTime` no frontend. *Self-validating*: AssertJ e `expect` sobre valor e efeito — TU-04 falha se o verificador passar a sinalizar editais encerrados. |
| `tests.md` §3 — AAA/Given-When-Then e nomes | Três blocos visíveis em todo teste; nomes `TU04_naoSinalizaEditalEncerradoComPrazoPassado`, `TI15_mudarStatusMigraEditalDeAgrupamento`, `it('exibe Vagas a definir quando o edital não tem vagas')`. |
| `tests.md` §4 — pirâmide | 30 testes de unidade, 23 de integração e 12 E2E: ~46% / ~35% / ~19% dos casos nomeados. **Desvio registrado:** a proporção foge do alvo de 70/20/10 porque a funcionalidade é essencialmente um CRUD — quase toda a regra que existe (autorização por perfil, unicidade de nome, FK da taxonomia, ausência de cache) só se prova acima da unidade, e inventar testes de unidade para atingir a proporção seria exatamente o "teste decorativo" que a §1 proíbe. O peso de *execução* fica na base: dos 23 testes de integração, 4 usam `@DataJpaTest`, 15 usam `@WebMvcTest` e apenas 4 sobem `@SpringBootTest`. |
| `code-standards.md` §2 — arquivos ≤ 100 linhas, métodos ≤ 30, ≤ 3 parâmetros, ≤ 3 níveis de condicional, sem comentário, sem linha em branco no método, sem literal mágico | A separação entre `EditalService` (escrita), `ConsultaEditaisService` (leitura) e as duas regras puras é o que torna o limite alcançável; dois controllers em vez de um pelo mesmo motivo. Todos os serviços recebem no máximo um id mais um objeto de parâmetro (`RequisicaoEdital`). Os limites de nome (160), vagas (999999) e URL (500) são constantes nomeadas, nunca literais espalhados. No frontend, `EditaisPage` delega o card a `EditalCard` e o formulário delega a seleção a `DisciplinasSeletor`. |
| `code-standards.md` §3 — camadas, injeção por construtor, sem Lombok, DTO `record`, entidade fora do controller, erro por `ErroNegocioException` | Controllers só orquestram e montam a `ResponseEntity`; a regra, o `@Transactional` e o `@PreAuthorize` ficam no serviço. `Edital` escreve construtor, getters e setters à mão, com setters só no que muda depois de criado, e `criado_em`/`atualizado_em` por `@PrePersist`/`@PreUpdate`. Todos os DTOs são `record` com mensagem de validação em pt-BR. Nenhuma exceção nova; nenhum `try/catch` em controller. `TaxonomiaService` consulta `EditalConsultaVinculos`, não `EditalRepository` — um domínio não acessa o repositório de outro. |
| `code-standards.md` §4 — Angular standalone, zoneless, signals, `input()`/`output()`, template e estilo separados, HTTP só em `core/api` | Todos os componentes são standalone, com estado em `signal`/`computed` e membros de template `protected readonly`; os stores são `providedIn: 'root'`; nenhum componente injeta `HttpClient`. 422 e 409 são mapeados campo a campo (TU-27, TU-28). `LOCALE_ID` pt-BR para datas e números. |
| Perguntar em vez de inventar, e registrar a decisão | Quatro decisões abertas foram levadas ao usuário — superfície da tela do estudante, natureza do campo banca, superfície da tela administrativa e campos além dos listados pelo PRD — e estão registradas em *Principais decisões*. |

### Conformidade com skills

| Skill | Aplicável | Como é observada |
| --- | --- | --- |
| `313-frameworks-spring-db-migrations-flyway` | sim | SQL versionado em `src/main/resources/db/migration` no padrão `V{versão}__{descrição}.sql`, continuando a numeração; `validate-on-migrate` ligado e `baseline-on-migrate` desligado, como já configurado em `application.properties`; disciplina *forward-only* — nenhuma migração aplicada é reescrita; `spring.flyway.locations` separa produção e `dev`; a migração é puramente aditiva e não altera tabela existente. |
| `postgresql-table-design` | sim | PK `uuid` com `gen_random_uuid()` e PK composta natural no vínculo N:N; `timestamptz` em UTC para instantes e `date` para o prazo, que é um dia civil e não um instante; `check` para o enum textual, para a faixa de vagas, para o esquema da URL e para a invariante de negócio (`edital_aberto_com_prazo`); índice único parcial compatível com exclusão lógica; índice de filtro alinhado exatamente à consulta de agrupamento; `on delete cascade` deliberado a partir de `edital` e ausente a partir de `disciplina`, para que a FK sustente CA-08. |
| `spring-boot-testing` | sim | Fatia mais estreita por cenário: JUnit + Mockito puros nas regras e serviços, `@DataJpaTest` com Testcontainers nas *constraints*, `@WebMvcTest` com `MockMvcTester` no contrato e na autorização, `@SpringBootTest` só nos quatro fluxos que atravessam escrita e leitura; asserções em AssertJ; contexto compartilhado para manter a suíte rápida. **Desvio registrado:** o `AGENTS.md` fala em "JUnit 5", enquanto o BOM do Spring Boot 4.1.1 traz JUnit Jupiter 6 — a API é a mesma e prevalece a versão gerenciada pelo BOM. |
| `angular-developer` | sim | Componentes standalone, detecção *zoneless*, estado por signals em serviços `providedIn: 'root'`, `input()`/`output()` como funções, `computed` para o estado vazio e para o rótulo de status, guardas funcionais (`authGuard`, `perfilGuard`) e `LOCALE_ID` pt-BR para datas e números. |
| `impeccable` | não | O layout é congelado pela regra 1 do `AGENTS.md`. As duas telas novas são compostas com componentes Nocturne existentes e o padrão visual das telas irmãs, não redesenho; a acessibilidade exigida pelo PRD é coberta por E2E-12. |
| `find-skills`, `criar-*`, `executar-*` | não | São skills de processo, não de implementação. |

### Arquivos relevantes e dependentes

**Criados — backend** (`backend/src/main/java/br/com/certame/`)

`catalogo/edital/Edital.java` · `catalogo/edital/StatusEdital.java` · `catalogo/edital/EditalRepository.java` · `catalogo/edital/EditalService.java` · `catalogo/edital/ConsultaEditaisService.java` · `catalogo/edital/ResolvedorDisciplinasEdital.java` · `catalogo/edital/VerificadorPrazoEdital.java` · `catalogo/edital/EditalConsultaVinculos.java` · `catalogo/edital/EditalController.java` · `catalogo/edital/EditalAdminController.java` · `catalogo/edital/dto/EditaisAgrupados.java` · `catalogo/edital/dto/EditalResumo.java` · `catalogo/edital/dto/EditalAdminResumo.java` · `catalogo/edital/dto/EditalDetalhe.java` · `catalogo/edital/dto/RequisicaoEdital.java` · `catalogo/edital/dto/DisciplinaExigida.java` · `catalogo/edital/dto/ItemCatalogoResumo.java` · `backend/src/main/resources/db/migration/V7__editais.sql` · `backend/src/main/resources/db/migration-dev/V905__seed_editais_dev.sql`

**Criados — frontend** (`frontend/src/app/`)

`core/api/edital-api.ts` · `core/api/modelos-edital.ts` · `features/editais/editais-page/` · `features/editais/editais-store.ts` · `features/editais/edital-card/` · `features/admin/editais/editais-admin-page/` · `features/admin/editais/editais-admin-store.ts` · `features/admin/editais/edital-form-page/` · `features/admin/editais/disciplinas-seletor/` · `frontend/e2e/editais.spec.ts`

**Modificados**

`backend/src/main/java/br/com/certame/catalogo/TaxonomiaService.java` (recusa exclusão de disciplina vinculada a edital) · `frontend/src/app/app.routes.ts` (cinco rotas novas) · `frontend/src/app/layout/shell/shell.html` (item "Editais" na navegação de estudo) · `protótipo/Certame.dc.html` (telas `EDITAIS` e `ADMIN: EDITAIS`, item de menu, substituição do modal de edital) · `TECH-SPEC.md` §5 (tabelas `edital` e `edital_disciplina`) e §6 (rotas de `/editais` e correção do perfil de `/admin/editais`)

**Dependências — precisam existir antes** (de `prd-acesso-e-conta` e `prd-banco-de-questoes`)

`config/SegurancaConfig.java` · `usuario/PerfilUsuario.java` · `comum/ManipuladorErrosGlobal.java` · `comum/ErroApi.java` · `comum/excecao/ConflitoException.java` · `comum/excecao/RecursoNaoEncontradoException.java` · `comum/excecao/DadosInvalidosException.java` · `comum/auditoria/RegistradorAuditoria.java` · `catalogo/Disciplina.java` · `catalogo/DisciplinaRepository.java` · `catalogo/Banca.java` · `catalogo/Orgao.java` · `catalogo/CatalogoController.java` · `backend/src/main/resources/db/migration/V2__banco_questoes.sql` · `frontend/src/app/core/auth/auth-guard.ts` (`perfilGuard`) · `frontend/src/app/shared/ui/confirmacao-dialog/` · `frontend/src/app/shared/ui/estado-vazio/` · `frontend/src/styles.css`

**Consumidores futuros**

`prd-simulados` (vínculo entre edital e simulado, explicitamente fora deste escopo) · plano de estudo dirigido a partir do edital, previsto pelo PRD como evolução do vínculo com a taxonomia.
