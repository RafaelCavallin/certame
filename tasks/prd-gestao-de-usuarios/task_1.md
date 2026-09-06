# Tarefa 1.0: Revisão do protótipo (Certame.dc.html)

## Visão geral

O protótipo aprovado (`protótipo/Certame.dc.html`) ainda não cobre alguns elementos exigidos pela funcionalidade de gestão de usuários: opção "Super admin" nos filtros/segmented de perfil, confirmação de exclusão de usuário, handler de "Reenviar convite", explicação textual das salvaguardas (autoexclusão e último super-admin) e paginação/ordenação da tabela de usuários. Como o frontend é tradução 1:1 do protótipo (regra 1 do AGENTS.md), essas lacunas precisam ser corrigidas no protótipo antes de qualquer tela nova em Angular.

<skills>
### Conformidade com skills

- `impeccable` — aplicável para revisão de hierarquia visual, estados de confirmação/bloqueio e acessibilidade (contraste, foco, rótulos textuais para perfil/status) ao editar o protótipo.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

Lido `AGENTS.md` e as rules em `.agents/rules/` (`code-standards.md`, `tests.md`, `folder-structure.md`).

- **Regra 1 (AGENTS.md)** — o frontend é tradução 1:1 do protótipo aprovado; nenhuma cor, fonte, espaçamento, raio ou sombra pode ser inventado fora dos tokens Nocturne (`var(--*)`). Lacunas identificadas na TechSpec devem ser corrigidas aqui, no protótipo, antes da tarefa 6.0.
- **Regra 3 (AGENTS.md)** — toda a interface e mensagens (inclusive as explicações de salvaguarda) em português do Brasil.
- Perfis e status devem ser expressos por rótulo textual, não apenas por cor (PRD, seção "UI/UX e acessibilidade").
- Esta tarefa não envolve código de produção (backend ou Angular) nem migrações — não se aplicam as regras 2, 4 e 5 do AGENTS.md.
</rules>

<requirements>
- RF-23 — paginação e ordenação por data de criação na tabela de usuários.
- RF-24 — opção "Super admin" disponível na criação/edição (perfil já existe no domínio, mas falta no protótipo).
- RF-25 — explicações textuais para os bloqueios de autoexclusão e último super-admin (não apenas desabilitar o controle).
- RF-26 — modal de confirmação de exclusão de usuário, com nome do usuário afetado na mensagem.
- RF-27 — feedback de notificação na tela para "Redefinir senha" e "Reenviar convite".
</requirements>

## Subtarefas

- [x] 1.1 Adicionar a opção "Super admin" ao filtro de perfil e ao segmented/select de perfil no formulário de usuário.
- [x] 1.2 Adicionar o componente de confirmação de exclusão de usuário (`kind: 'usuario-delete'`), com nome do usuário na mensagem.
- [x] 1.3 Adicionar o handler/botão "Reenviar convite", visível apenas para usuários em convite pendente, e o botão "Redefinir senha" visível apenas para usuários ativos.
- [x] 1.4 Adicionar os textos de explicação exibidos quando uma salvaguarda bloqueia a ação (autoexclusão e último super-admin).
- [x] 1.5 Adicionar paginação e ordenação (asc/desc por data de criação) na tabela de usuários.

## Detalhes de implementação

Ver `techspec.md`, seção "Sequenciamento do desenvolvimento" (passo 1) e "Experiência do usuário" do `prd.md` para os fluxos principais e requisitos de UI/UX/acessibilidade.

## Critérios de aceitação relacionados

A validação desses critérios é responsabilidade do QA:

- CA-01
- CA-02
- CA-07
- CA-08
- CA-10
- CA-12

## Testes da tarefa

Não aplicável — o protótipo não é testado automaticamente; a TechSpec não define casos de teste para esta tarefa. A conformidade visual é revisada na tarefa 6.0 (tradução para Angular) e validada em QA.

## Arquivos relevantes

- `protótipo/Certame.dc.html`
- `protótipo/_ds/nocturne-<uuid>/` (tokens e design system, referência, não editar)
