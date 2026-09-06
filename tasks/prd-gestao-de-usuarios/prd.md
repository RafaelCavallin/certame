# Documento de Requisitos do Produto (PRD)

> **Funcionalidade:** Gestão de usuários (super-admin) · **Fase:** F5 — Governança
> Derivado de `PRD.md` (Certame v1.3). Cobre RF-23…RF-27, apoiado em RN-05 e RN-06.

## Visão geral

O Certame tem quatro perfis hierárquicos — estudante, professor, administrador e super-admin — e o perfil determina o que cada pessoa vê e pode fazer. Estudantes entram por autosserviço; todo o resto do time entra por convite. Sem uma tela de gestão, cada novo professor ou administrador viraria um `INSERT` manual no banco, sem rastro e sem controle.

Esta funcionalidade dá ao super-admin o controle completo do acesso: listar, criar com convite, editar perfil e status, redefinir senha de terceiros e excluir. Ela é pequena em número de telas e grande em consequência — é o ponto do sistema onde um erro concede acesso indevido ou tira o acesso de quem opera a plataforma. Por isso traz salvaguardas explícitas: o super-admin não pode remover o próprio acesso, o último super-admin não pode ser rebaixado, nenhuma senha é definida manualmente por terceiros e toda ação é auditada.

## Objetivos

- **Autonomia operacional:** 100% das contas de professor, administrador e super-admin criadas pela própria plataforma, sem intervenção no banco.
- **Convite eficaz:** ≥ 90% dos convites enviados resultam em senha definida em até 7 dias (premissa de meta, a calibrar após o lançamento).
- **Nenhum bloqueio de governança:** 0 ocorrências de instância sem super-admin ativo.
- **Rastreabilidade:** 100% das ações de criação, alteração de perfil, alteração de status, redefinição de senha e exclusão registradas em auditoria com ator e data.
- **Segurança do processo:** 0 senhas exibidas, definidas ou transmitidas por um administrador em nome de outro usuário.

## Histórias de usuário

- **US1** — Como super-admin, quero ver todos os usuários com nome, e-mail, perfil, status e data de criação, para saber quem tem acesso à plataforma.
- **US2** — Como super-admin, quero ordenar por data de criação e filtrar por perfil e status, para encontrar rapidamente quem procuro.
- **US3** — Como super-admin, quero criar um usuário com nome, e-mail, perfil e status, opcionalmente enviando um convite, para dar acesso a um novo professor ou administrador.
- **US4** — Como super-admin, quero editar um usuário e alterar seu perfil e status, para promover, rebaixar ou desativar alguém.
- **US5** — Como super-admin, quero excluir um usuário com confirmação, para encerrar um acesso que não deve mais existir.
- **US6** — Como super-admin, quero acionar "Redefinir senha" e que o próprio usuário receba o link, para ajudá-lo sem nunca conhecer a senha dele.
- **US7** — Como usuário convidado, quero receber o convite e definir minha senha, para ativar meu acesso.
- **Caso de borda 1** — Tento rebaixar ou desativar a mim mesmo: a operação é bloqueada com explicação.
- **Caso de borda 2** — Tento rebaixar o último super-admin ativo: a operação é bloqueada.
- **Caso de borda 3** — Excluo um usuário com histórico de respostas: o acesso acaba, mas o histórico é preservado de forma anonimizada.
- **Caso de borda 4** — Cadastro um e-mail que já existe: a criação é recusada com mensagem clara.

## Principais funcionalidades

### 1. Tabela de usuários (RF-23)

Visão única de quem tem acesso, com os atributos que importam para governança.

- **RF-23** — Tabela de usuários com nome, e-mail, perfil, status e data de criação, com ordenação por criação e filtros por perfil e status.

### 2. Criação com convite (RF-24)

- **RF-24** — Criar usuário com nome, e-mail, perfil, status e envio opcional de convite. Nome e e-mail são validados, o e-mail é único e o usuário aparece na lista sem recarregar a página.
- **RN-05** — Um usuário com convite pendente só consegue entrar depois de definir a senha pelo link enviado.

### 3. Edição de perfil e status (RF-25)

- **RF-25** — Editar usuário, alterando perfil e status (ativo, inativo, convite pendente). O super-admin não pode remover o próprio acesso nem rebaixar o último super-admin.
- **RN-06** — Usuário inativo é bloqueado no login com mensagem genérica de credencial inválida.

### 4. Exclusão com preservação de histórico (RF-26)

- **RF-26** — Excluir usuário com confirmação explícita. A exclusão é lógica e o histórico de respostas é preservado de forma anonimizada.

### 5. Redefinição de senha administrativa (RF-27)

- **RF-27** — A ação "Redefinir senha" envia o link ao próprio usuário e confirma por notificação na tela. Nenhuma senha é exibida ou definida manualmente pelo administrador.

## Critérios de aceitação

- **CA-01** (US1, RF-23) — Dada a tela de usuários, quando a abro como super-admin, então vejo nome, e-mail, perfil, status e data de criação de cada usuário, com paginação.
- **CA-02** (US2, RF-23) — Dado um filtro por perfil e status e a ordenação por data de criação, quando os aplico, então a lista mostra apenas os usuários correspondentes na ordem pedida.
- **CA-03** (US3, RF-24) — Dado um e-mail inédito e nome válido, quando crio o usuário, então ele aparece na lista imediatamente, sem recarregar a página.
- **CA-04** (Borda 4, RF-24) — Dado um e-mail já cadastrado, quando tento criar o usuário, então a criação é recusada com mensagem clara e nada é gravado.
- **CA-05** (US3/US7, RF-24, RN-05) — Dado o envio de convite marcado, quando crio o usuário, então ele fica com status convite pendente, recebe o link e só consegue entrar após definir a senha.
- **CA-06** (US4, RF-25) — Dado um usuário existente, quando altero seu perfil de professor para administrador, então a nova capacidade vale imediatamente na próxima requisição autenticada dele.
- **CA-07** (Borda 1, RF-25) — Dado que sou super-admin, quando tento alterar meu próprio perfil para um menor ou meu status para inativo, então a operação é bloqueada com explicação.
- **CA-08** (Borda 2, RF-25) — Dado que existe apenas um super-admin ativo, quando tento rebaixá-lo ou desativá-lo, então a operação é bloqueada com explicação.
- **CA-09** (US4, RN-06) — Dado um usuário marcado como inativo, quando ele tenta entrar com a senha correta, então recebe a mensagem genérica de credencial inválida.
- **CA-10** (US5, RF-26) — Dada a ação de excluir, quando a aciono, então uma confirmação explícita é exigida antes de qualquer efeito.
- **CA-11** (Borda 3, RF-26) — Dado um usuário excluído com histórico de respostas, quando consulto as estatísticas agregadas, então o histórico continua íntegro e sem identificação pessoal, e o acesso do usuário está encerrado.
- **CA-12** (US6, RF-27) — Dada a ação "Redefinir senha", quando a aciono, então o link é enviado ao e-mail do usuário, vejo uma notificação de confirmação na tela e nenhuma senha é exibida.
- **CA-13** (RF-23…RF-27) — Dado um usuário que não é super-admin, quando tenta acessar qualquer operação de gestão de usuários, então a requisição é recusada no servidor, mesmo que a interface tenha sido manipulada.
- **CA-14** (Objetivo de rastreabilidade) — Dada qualquer criação, alteração de perfil ou status, redefinição de senha ou exclusão, quando a operação conclui, então há registro de auditoria com ator, ação, entidade afetada e data.

## Experiência do usuário

**Perfil e necessidade.** Apenas o super-admin acessa estas telas. São operações raras, de alto impacto e frequentemente feitas sob pressão (alguém precisa de acesso agora, alguém saiu da equipe hoje). A interface deve ser explícita sobre consequências.

**Fluxos principais.**
1. *Convidar* — Usuários → Novo usuário → dados e perfil → enviar convite → usuário na lista como convite pendente.
2. *Promover ou desativar* — Usuários → buscar → editar → alterar perfil ou status → salvar.
3. *Ajudar com senha* — Usuários → linha do usuário → Redefinir senha → notificação de confirmação.
4. *Encerrar acesso* — Usuários → linha do usuário → Excluir → confirmação → exclusão lógica.

**UI/UX e acessibilidade.**
- Tradução 1:1 do protótipo aprovado sobre o design system Nocturne; perfis e status por rótulo textual, não apenas por cor.
- Toda ação destrutiva ou irreversível exige confirmação explícita, com o nome do usuário afetado na mensagem.
- Bloqueios de salvaguarda (autoexclusão, último super-admin) explicam o motivo, em vez de apenas desabilitar o controle sem justificativa.
- Feedback imediato por notificação na tela para envio de convite e redefinição de senha.
- Navegação completa por teclado, foco visível, contraste mínimo de 4,5:1; textos e datas em português do Brasil.

## Restrições técnicas de alto nível

- **Stack não negociável:** Angular 22 no frontend; Spring Boot 4.1 / Java 25, PostgreSQL 18 e Flyway no backend.
- **Autorização no servidor:** todas as operações exigem perfil super-admin, validadas no servidor independentemente da interface.
- **Segurança de credenciais:** nenhuma senha é gerada, exibida ou definida por terceiros; a redefinição usa o mesmo token de uso único de `prd-acesso-e-conta` (RF-04).
- **Auditoria obrigatória:** ator, ação, entidade, identificador e data para toda operação administrativa.
- **Privacidade (LGPD):** exclusão lógica com anonimização do histórico; base legal registrada; exportação e exclusão de dados a pedido do titular.
- **Dependência externa:** provedor de e-mail transacional (SES ou SendGrid) para convites e redefinições.
- **Integridade referencial:** o usuário é referenciado por respostas, cadernos, flashcards e tentativas de simulado — a exclusão nunca pode ser física em cascata.
- **Idioma:** português do Brasil.

## Fora do escopo

- Gestão de equipes, grupos ou permissões granulares além dos quatro perfis.
- Autenticação em dois fatores e políticas de expiração de senha.
- Autosserviço de cadastro para perfis não estudante — em `prd-acesso-e-conta` (RF-02).
- Painel de auditoria navegável pela interface (o registro é gerado nesta fase; a tela de consulta fica fora da v1).
- Importação em massa de usuários por planilha.
- Suspensão temporária com prazo automático e reativação agendada.
- Impersonação ("entrar como" outro usuário) — incompatível com RN-07, que torna flashcards privados inclusive para o super-admin.
