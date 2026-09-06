# Documento de Requisitos do Produto (PRD)

> **Funcionalidade:** Acesso e conta · **Fase:** F1 — Base
> Derivado de `PRD.md` (Certame v1.3). Cobre RF-01…RF-05, RN-05 e RN-06.

## Visão geral

O Certame é uma plataforma de estudo para concursos públicos com quatro perfis de acesso (estudante, professor, administrador e super-admin). Esta funcionalidade é a porta de entrada do produto: cadastro de estudante, login por e-mail e senha, recuperação de senha por link de uso único e encerramento de sessão. É ela que estabelece a identidade do usuário e o perfil que determina toda a navegação e todas as operações permitidas nas demais funcionalidades.

O valor está em tirar o candidato da tela de acesso o mais rápido possível — a plataforma existe para ele resolver questões, não para gerenciar credenciais — sem abrir mão de segurança: senhas nunca trafegam ou repousam em texto claro, o fluxo de recuperação não revela quais e-mails existem na base e a autorização por perfil é sempre validada no servidor, mesmo quando a interface já esconde a ação.

## Objetivos

- **Acesso sem atrito:** cadastro de estudante concluído em uma única tela, com no máximo três campos (nome, e-mail, senha).
- **Fluxo de recuperação eficaz:** ≥ 80% dos usuários que solicitam redefinição concluem a definição da nova senha dentro da validade do token (premissa de meta, a revisar após o primeiro mês).
- **Zero vazamento de cadastro:** 100% das respostas de "Esqueci minha senha" são idênticas para e-mail existente e inexistente.
- **Autorização confiável:** 100% dos endpoints protegidos recusam perfil insuficiente no servidor, verificado por teste automatizado.
- **Base para a retenção do produto:** habilita a métrica de usuários ativos semanais / cadastrados ≥ 45% definida no PRD-mãe.

## Histórias de usuário

- **US1** — Como candidato sem conta, quero me cadastrar com nome, e-mail e senha na mesma tela do login, para começar a resolver questões imediatamente.
- **US2** — Como estudante cadastrado, quero entrar com e-mail e senha e cair direto no meu Início, para retomar o estudo sem navegar.
- **US3** — Como professor, administrador ou super-admin, quero que o login me leve ao Início do meu perfil, com a navegação correspondente, para acessar minhas ferramentas de trabalho.
- **US4** — Como usuário que esqueceu a senha, quero informar meu e-mail e receber um link de redefinição, para recuperar o acesso sozinho.
- **US5** — Como usuário convidado por um super-admin, quero definir minha senha pelo link recebido, para ativar meu acesso.
- **US6** — Como usuário em qualquer dispositivo compartilhado, quero encerrar a sessão pelo rodapé da navegação, para que ninguém continue estudando na minha conta.
- **Caso de borda 1** — Tento me cadastrar com e-mail já usado: recebo mensagem clara, sem perder o que digitei.
- **Caso de borda 2** — Minha conta foi inativada: o login falha com mensagem genérica, sem revelar o motivo.
- **Caso de borda 3** — Uso um link de redefinição já consumido ou expirado: recebo aviso e a opção de pedir outro.

## Principais funcionalidades

### 1. Login unificado com abas Entrar / Criar conta (RF-01)

Tela única de acesso, com alternância entre entrar e criar conta sem recarregar a página. É a primeira impressão do produto e precisa ser silenciosa como o resto da interface.

- **RF-01** — Login por e-mail e senha, com abas Entrar / Criar conta na mesma tela. Credencial válida abre o Início do perfil correspondente; credencial inválida exibe erro inline sem limpar o campo de e-mail.

### 2. Cadastro de estudante (RF-02)

Autosserviço apenas para o perfil estudante. Professores, administradores e super-admins são criados pela gestão de usuários (PRD `prd-gestao-de-usuarios`).

- **RF-02** — Cadastro de estudante com nome, e-mail e senha. A conta é criada com perfil `estudante` e status ativo; e-mail duplicado é recusado com mensagem.

### 3. Recuperação de senha (RF-03, RF-04)

Fluxo em duas etapas — pedido pelo e-mail e definição da nova senha por link — desenhado para não revelar quais e-mails existem na base.

- **RF-03** — Fluxo "Esqueci minha senha": informar e-mail e receber link de redefinição. A tela de confirmação é exibida sempre, mesmo para e-mail inexistente; e-mail malformado é barrado antes do envio.
- **RF-04** — Definição de nova senha por link com token de uso único. O token expira em 60 minutos ou imediatamente após o uso; a senha tem mínimo de 8 caracteres.

### 4. Encerramento de sessão (RF-05)

- **RF-05** — Encerrar sessão a partir do rodapé da navegação, disponível em todos os perfis. A sessão é invalidada no servidor e o usuário retorna à tela de login.

### 5. Regras de estado da conta (RN-05, RN-06)

- **RN-05** — Convite pendente permite login apenas depois da definição de senha pelo link enviado.
- **RN-06** — Usuário inativo é bloqueado no login com mensagem genérica de credencial inválida — nunca com a informação de que a conta existe e está inativa.

## Critérios de aceitação

- **CA-01** (US2, RF-01) — Dado um estudante ativo com credenciais corretas, quando envia o formulário de login, então é redirecionado ao Início do perfil estudante em até 2 s.
- **CA-02** (US2, RF-01) — Dado um e-mail correto e senha errada, quando envia o formulário, então vejo erro inline abaixo do campo de senha, o e-mail digitado permanece preenchido e o foco vai para o campo de senha.
- **CA-03** (US3, RF-01) — Dado um usuário professor, administrador ou super-admin, quando entra, então a navegação exibida é a do seu perfil e nenhuma ação de perfil superior aparece.
- **CA-04** (US1, RF-02) — Dado um e-mail inédito e senha com 8 ou mais caracteres, quando concluo o cadastro, então a conta é criada com perfil `estudante` e status ativo e já entro autenticado.
- **CA-05** (Borda 1, RF-02) — Dado um e-mail já cadastrado, quando tento criar conta, então recebo a mensagem de e-mail já em uso, nenhum registro novo é criado e os demais campos continuam preenchidos.
- **CA-06** (US4, RF-03) — Dado qualquer e-mail sintaticamente válido, quando solicito a redefinição, então vejo a mesma tela de confirmação e a mesma latência percebida, exista ou não a conta.
- **CA-07** (US4, RF-03) — Dado um e-mail malformado, quando solicito a redefinição, então o envio é barrado com validação inline e nenhuma mensagem é disparada.
- **CA-08** (US4, RF-04) — Dado um link de redefinição válido e uma senha com ao menos 8 caracteres, quando confirmo, então a senha é trocada, o token é marcado como usado e sou levado ao login com aviso de sucesso.
- **CA-09** (Borda 3, RF-04) — Dado um token expirado (mais de 60 min) ou já utilizado, quando abro o link, então vejo aviso de link inválido com a ação de solicitar um novo.
- **CA-10** (US5, RN-05) — Dado um usuário com status convite pendente, quando tenta entrar com qualquer senha, então o acesso é recusado até que ele defina a senha pelo link do convite.
- **CA-11** (Borda 2, RN-06) — Dado um usuário inativo com senha correta, quando tenta entrar, então recebe exatamente a mesma mensagem genérica de credencial inválida apresentada a uma senha errada.
- **CA-12** (US6, RF-05) — Dado um usuário autenticado, quando aciona "Sair" no rodapé da navegação, então a sessão é invalidada no servidor e voltar pelo histórico do navegador não restaura o acesso.

## Experiência do usuário

**Perfis e necessidades.** O estudante chega pelo autosserviço e quer entrar rápido; professores, administradores e super-admins chegam por convite e precisam apenas de login e recuperação.

**Fluxos principais.**
1. *Entrar* — tela de acesso → aba Entrar → Início do perfil.
2. *Criar conta* — tela de acesso → aba Criar conta → Início do estudante, já autenticado.
3. *Recuperar senha* — tela de acesso → "Esqueci minha senha" → confirmação → e-mail → tela de nova senha → login.
4. *Sair* — rodapé da navegação → tela de acesso.

**UI/UX e acessibilidade.**
- Interface implementada exatamente como o protótipo aprovado, sobre o design system Nocturne: nenhuma cor, fonte ou espaçamento novo.
- Erros sempre inline e próximos ao campo, nunca em alerta modal; texto em português do Brasil, incluindo mensagens de erro e datas.
- Navegação completa por teclado (`Tab` entre campos, `Enter` submete), foco visível em todos os controles e contraste mínimo de 4,5:1 em texto corrido.
- Campos de e-mail e senha com rótulos associados e atributos de autocompletar adequados, para funcionar com gerenciadores de senha.

## Restrições técnicas de alto nível

- **Stack não negociável:** Angular 22 (standalone, zoneless, signals) no frontend; Spring Boot 4.1 / Java 25 e PostgreSQL 18 com Flyway no backend.
- **Segurança:** senhas armazenadas com BCrypt; autorização por perfil validada no servidor (`ROLE_ESTUDANTE`, `ROLE_PROFESSOR`, `ROLE_ADMIN`, `ROLE_SUPER_ADMIN`), nunca apenas na interface.
- **Token de redefinição:** uso único, validade de 60 minutos, armazenado como hash.
- **Dependência externa:** provedor de e-mail transacional (SES ou SendGrid) a definir até o fim da fase F1 — risco herdado do PRD-mãe; sem ele, RF-03 e RF-04 não fecham.
- **Privacidade (LGPD):** base legal registrada para os dados de cadastro; exportação e exclusão a pedido do titular.
- **Disponibilidade e desempenho:** 99,5% mensal; resposta de login e cadastro dentro do orçamento geral de latência da plataforma.
- **Idioma:** português do Brasil em toda a interface.

## Fora do escopo

- Login social, SSO corporativo e autenticação em dois fatores.
- Troca de senha pelo usuário já autenticado e edição de dados do próprio perfil.
- Autosserviço de cadastro para perfis diferentes de estudante — criação de professor, administrador e super-admin pertence a `prd-gestao-de-usuarios` (RF-24).
- Verificação obrigatória de e-mail antes do primeiro acesso.
- Captcha e bloqueio de conta após tentativas sucessivas. O limite de taxa por e-mail no servidor (5 tentativas em 15 min) está **dentro** do escopo, por exigência da §4 da `TECH-SPEC.md`.
- Planos, cobrança e qualquer restrição de acesso por pagamento (sem monetização na v1).
