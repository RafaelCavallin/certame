# Tarefa 6.0: Redefinição de senha (RF-03, RF-04)

## Visão geral

Implementa o fluxo de recuperação em duas etapas: o pedido pelo e-mail — sempre 204, com envio assíncrono após o commit, para não revelar a existência da conta nem pelo corpo nem pela latência — e o consumo do token de uso único, que troca a senha e revoga todos os refresh tokens do usuário. Inclui a porta `EnviadorEmail` e o adaptador SMTP sobre MailHog em `dev`.

<skills>
### Conformidade com skills

- `spring-boot-testing` — JUnit + Mockito no `RedefinicaoSenhaService` com `Clock` injetado e `EnviadorEmail` mockado; nos testes de integração, o `EnviadorEmail` é substituído por um duplo que acumula as mensagens, permitindo asserção sobre destinatário e link sem SMTP real.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

`AGENTS.md` lido na íntegra. Não existe `.agents/rules/`. Regras relevantes: regra 3 (`RedefinicaoSenhaService`, modelo de mensagem em pt-BR), regra 4 (as duas rotas são exatamente as da §6) e "domínio + testes antes do controller". Nenhum desvio.
</rules>

<requirements>
- **RF-03** — `POST /auth/senha/esqueci` devolve 204 sempre que o e-mail for sintaticamente válido, exista ou não a conta, com corpo e cabeçalhos idênticos nos dois casos; e-mail malformado devolve 422 sem nenhuma interação com `EnviadorEmail`.
- Quando a conta não existe ou está `INATIVO`, nenhum token é gerado e nada é enviado. Um pedido novo invalida os tokens de redefinição pendentes do mesmo usuário.
- **RF-04** — `POST /auth/senha/redefinir` consome o token de uso único (TTL de 60 min, comparado por hash SHA-256), grava a nova senha, marca `usado_em` e revoga todos os refresh tokens do usuário; devolve 204.
- Token inexistente, expirado ou já consumido devolve 400 `TOKEN_INVALIDO`.
- **RN-05** — Quando a `finalidade` é `CONVITE`, o mesmo endpoint move o status de `CONVITE_PENDENTE` para `ATIVO`.
- Envio assíncrono (`@Async`) após o commit, com três tentativas em espera exponencial (1 s, 4 s, 16 s); falha de SMTP é logada em `ERROR` sem alterar a resposta 204 — o token permanece válido e o usuário pode pedir outro link.
- O link de redefinição nunca é registrado em log, em nenhum nível.

</requirements>

## Subtarefas

- [x] 6.1 Criar os DTOs `RequisicaoEsqueciSenha` e `RequisicaoRedefinirSenha` em `auth/dto/`.
- [x] 6.2 Definir a porta `email/EnviadorEmail` (`enviarRedefinicaoSenha`, `enviarConvite`).
- [x] 6.3 Implementar `email/EnviadorEmailSmtp` sobre `JavaMailSender`, assíncrono, com modelo de mensagem em pt-BR, timeouts de 5 s/10 s e as três tentativas com espera exponencial.
- [x] 6.4 Implementar `RedefinicaoSenhaService.solicitar` (geração do token de 32 bytes base64url, TTL de 60 min, persistência apenas do hash, invalidação dos pendentes) com TU-08 e TU-09.
- [x] 6.5 Implementar `RedefinicaoSenhaService.redefinir`, incluindo a ativação de conta na finalidade `CONVITE`, com TU-10 a TU-13.
- [x] 6.6 Acrescentar `POST /auth/senha/esqueci` e `POST /auth/senha/redefinir` ao `AutenticacaoController`.
- [x] 6.7 Aplicar o limitador de tentativas ao pedido de redefinição (5 por e-mail em 15 min).
- [x] 6.8 Implementar o expurgo agendado de tokens expirados.
- [x] 6.9 Escrever os testes de integração TI-06 a TI-10.
- [x] 6.10 Registrar as métricas `certame.auth.senha.pedido`, `certame.auth.senha.redefinida` e `certame.auth.email.falha`.

## Detalhes de implementação

Ver TechSpec, seções *Principais interfaces* (`RedefinicaoSenhaService`, `EnviadorEmail`), *Endpoints da API* (`POST /auth/senha/esqueci` e `/auth/senha/redefinir`, com as notas sobre latência idêntica e finalidade `CONVITE`), *Pontos de integração* (SMTP, tratamento de erro e timeouts) e *Principais decisões* (204 incondicional com envio assíncrono; porta `EnviadorEmail` em vez de SDK do provedor).

## Critérios de aceitação relacionados

- CA-06
- CA-07
- CA-08
- CA-09
- CA-10

## Testes da tarefa

### Testes de unidade

- [x] TU-08 — `solicitar` redefinição para e-mail inexistente
- [x] TU-09 — `solicitar` redefinição para e-mail existente
- [x] TU-10 — `redefinir` com token válido
- [x] TU-11 — `redefinir` com token expirado
- [x] TU-12 — `redefinir` com token já consumido
- [x] TU-13 — `redefinir` com token de finalidade `CONVITE`

### Testes de integração

- [x] TI-06 — `POST /auth/senha/esqueci` para e-mail existente e inexistente
- [x] TI-07 — `POST /auth/senha/esqueci` com e-mail malformado
- [x] TI-08 — Ciclo completo de redefinição
- [x] TI-09 — Reuso do token de redefinição
- [x] TI-10 — Refresh tokens revogados após redefinição

## Arquivos relevantes

- `backend/src/main/java/br/com/certame/auth/RedefinicaoSenhaService.java`
- `backend/src/main/java/br/com/certame/auth/AutenticacaoController.java`
- `backend/src/main/java/br/com/certame/auth/dto/RequisicaoEsqueciSenha.java`
- `backend/src/main/java/br/com/certame/auth/dto/RequisicaoRedefinirSenha.java`
- `backend/src/main/java/br/com/certame/email/EnviadorEmail.java`
- `backend/src/main/java/br/com/certame/email/EnviadorEmailSmtp.java`
