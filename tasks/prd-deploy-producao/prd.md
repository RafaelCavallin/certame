# Documento de Requisitos do Produto (PRD)

> **Funcionalidade:** Deploy em produção · **Fase:** F7 — Operação
> Derivado de `PRD.md` (Certame v1.3) §7 — Requisitos não funcionais (Disponibilidade, Segurança, Privacidade) e de `TECH-SPEC.md` §9 — Qualidade, observabilidade e entrega.
> Não cobre nenhum RF-01…RF-37: introduz requisitos próprios, numerados **RF-D01…RF-D12**.

## Visão geral

O Certame está funcionalmente completo até a F6 — 591 testes de backend e 522 de frontend passam, o CI está verde nos dois módulos — e mesmo assim **nenhum estudante consegue usá-lo**. O produto só existe na máquina do desenvolvedor: não há imagem de contêiner, não há servidor, não há domínio, e o perfil `prod` nunca foi executado. Todo o esforço das seis fases anteriores está represado atrás da falta de um caminho de publicação.

Esta entrega constrói esse caminho: empacota o backend em imagem de contêiner, provisiona dois ambientes (staging e produção) em VPS com Docker Compose, publica a SPA e a API sob um mesmo domínio com HTTPS, e liga tudo a pipelines que levam um commit aprovado no CI até produção sem etapa manual em servidor. O valor não é uma funcionalidade nova — é a diferença entre um produto que existe e um que não existe.

## Objetivos

- **Produto acessível:** a aplicação responde em um domínio público sobre HTTPS, com certificado válido e renovação automática.
- **Publicação sem toque manual:** 100% dos deploys partem de um commit em `main` com CI verde; nenhum passo exige `ssh` no servidor em operação normal.
- **Ciclo curto:** do merge em `main` até produção atualizada em **≤ 15 minutos** (premissa, a calibrar após as primeiras execuções).
- **Reversão rápida:** retornar à versão anterior em **≤ 5 minutos**, sem restaurar banco de dados.
- **Disponibilidade herdada:** sustentar os 99,5% mensais exigidos em `PRD.md` §7, com janela de manutenção fora do pico (19h–23h BRT).
- **Publicação sem queda:** deploys de rotina não retornam erro ao usuário; a versão nova só recebe tráfego depois de responder no *health check*.
- **Dado recuperável:** backup diário do Postgres com **restauração testada ao menos uma vez** antes do primeiro acesso público — backup não verificado não conta como backup.
- **Segredo fora do repositório:** nenhuma credencial de produção versionada; 100% delas injetadas por variável de ambiente a partir do cofre de segredos do GitHub e do servidor.
- **Migração segura:** toda migração do Flyway aplicada em staging antes de produção, sem exceção.

## Histórias de usuário

Os perfis aqui são o **operador** (hoje, o próprio desenvolvedor) e, indiretamente, o **estudante**, que só percebe esta entrega pelo fato de o produto existir.

- **US1** — Como estudante, quero acessar o Certame por um endereço público em HTTPS, para estudar de qualquer dispositivo sem aviso de site inseguro.
- **US2** — Como estudante, quero continuar autenticado enquanto uso o sistema, para não perder a sessão de estudo em andamento durante uma publicação.
- **US3** — Como operador, quero que um commit aprovado no CI chegue sozinho a produção, para não depender de uma sequência manual que eu possa errar às 23h.
- **US4** — Como operador, quero que a versão nova só receba tráfego depois de provar que sobe e responde, para uma build quebrada não derrubar o ar.
- **US5** — Como operador, quero validar cada mudança em staging antes de produção, para descobrir problemas de migração e configuração longe do usuário.
- **US6** — Como operador, quero reverter para a versão anterior com um comando, para reduzir o tempo de indisponibilidade quando algo passa pelos testes e quebra em produção.
- **US7** — Como operador, quero backup diário automático e restauração já testada, para que uma falha de disco não signifique perder o histórico de respostas dos estudantes.
- **US8** — Como operador, quero ver logs e a saúde da aplicação sem entrar no servidor, para diagnosticar um incidente rapidamente.
- **US9** — Como operador, quero que os segredos de produção fiquem fora do Git, para que abrir o repositório um dia não vaze credencial de banco nem chave de assinatura de token.
- **Caso de borda 1** — O deploy falha no meio: o ambiente permanece na versão anterior, íntegro, e o pipeline reporta a falha.
- **Caso de borda 2** — Uma migração do Flyway falha em staging: produção não é tocada e o pipeline interrompe a promoção.
- **Caso de borda 3** — O servidor reinicia (queda de energia, atualização de kernel): os serviços sobem sozinhos, na ordem correta, sem intervenção.
- **Caso de borda 4** — O certificado TLS se aproxima do vencimento: a renovação ocorre sem ação manual e sem queda.
- **Caso de borda 5** — Dois merges em `main` em sequência rápida: os deploys não se atropelam; o mais recente prevalece e o ambiente não fica em estado misto.

## Principais funcionalidades

### 1. Empacotamento do backend (RF-D01, RF-D02)

Hoje não existe nenhum `Dockerfile` no repositório. O backend precisa virar um artefato imutável, idêntico entre staging e produção, para que "funcionou em staging" tenha significado.

- **RF-D01** — O backend é empacotado em imagem de contêiner versionada pelo SHA do commit, construída uma única vez por commit e promovida sem reconstrução entre ambientes.
- **RF-D02** — A imagem executa a aplicação com um usuário sem privilégios de root e expõe o *health check* do Actuator já habilitado em `management.endpoints.web.exposure.include`.

### 2. Publicação da SPA sob a mesma origem da API (RF-D03, RF-D04)

O frontend chama a API por caminhos **relativos** (`/api/v1/...`) e o refresh token trafega em cookie `SameSite=Strict`. Servir SPA e API em origens diferentes exigiria reescrever todas as chamadas e afrouxar o cookie — perdendo a proteção CSRF atual. A mesma origem não é preferência de arquitetura: é requisito do código existente.

- **RF-D03** — SPA e API são servidas sob o **mesmo domínio**: `/api/*` roteado para o backend e todo o restante para os arquivos estáticos da SPA.
- **RF-D04** — Rotas do Angular acessadas diretamente ou recarregadas (F5) devolvem a aplicação, não 404 — o servidor faz *fallback* para o documento raiz da SPA.

### 3. Ambientes de staging e produção (RF-D05, RF-D06)

- **RF-D05** — Existem dois ambientes isolados, cada um com seu banco de dados, seus segredos e seu domínio; nenhum recurso é compartilhado entre eles.
- **RF-D06** — Staging usa o perfil Spring `staging` e produção o perfil `prod`; **nenhum dos dois carrega as migrações de semente de desenvolvimento** (`db/migration-dev`), hoje restritas ao perfil `dev`.

### 4. Configuração completa do perfil de produção (RF-D07)

O perfil `prod` está incompleto: `certame.seguranca.frontend-url` não é definido em `application-prod.properties`, então cai no padrão `http://localhost:4200` e os e-mails de redefinição de senha sairiam com link para a máquina do próprio usuário — recurso de recuperação de conta inoperante.

- **RF-D07** — Todo parâmetro que difere entre ambientes é explicitamente definido nos perfis `staging` e `prod`, sem depender de valor padrão de desenvolvimento. Inclui, no mínimo: URL do frontend, origem de CORS, credenciais de banco, credenciais de SMTP e segredo de assinatura de token.

### 5. Entrega contínua a partir do CI (RF-D08, RF-D09, RF-D10)

- **RF-D08** — Um commit em `main` que passe no CI dispara automaticamente o deploy para staging; a promoção para produção ocorre em seguida, condicionada ao sucesso do deploy em staging e da verificação de fumaça.
- **RF-D09** — Após cada deploy, uma verificação de fumaça confirma que a aplicação responde e que o fluxo de autenticação funciona. Falha na verificação interrompe a promoção para produção.
- **RF-D10** — Deploys do mesmo ambiente não executam concorrentemente; um deploy mais recente cancela ou aguarda o anterior, nunca produzindo estado misto.

### 6. Operação: reversão, backup e observabilidade (RF-D11, RF-D12)

O `TECH-SPEC.md` §9 previa "Postgres gerenciado com backup diário e PITR". Em VPS o banco é auto-hospedado, então **a responsabilidade pelo backup passa a ser nossa** — é a diferença mais relevante entre o previsto e o escolhido.

- **RF-D11** — Backup diário automático do banco de produção, armazenado **fora do servidor de aplicação**, com retenção mínima de 7 dias e procedimento de restauração documentado e exercitado.
- **RF-D12** — É possível reverter para a versão imediatamente anterior sem reconstruir imagem, e consultar logs e saúde da aplicação sem acesso interativo ao servidor.

## Critérios de aceitação

- **CA-01** (US1, RF-D03) — Dado o domínio de produção, quando o acesso por HTTP, então sou redirecionado para HTTPS com certificado válido e sem aviso do navegador.
- **CA-02** (US1, RF-D03) — Dada a aplicação publicada, quando faço login e navego pelas telas, então as chamadas a `/api/v1/*` respondem sob o mesmo domínio, sem erro de CORS.
- **CA-03** (US1, RF-D04) — Dada uma rota interna como `/simulados`, quando a acesso diretamente pela barra de endereço ou recarrego a página, então a aplicação carrega normalmente, sem 404.
- **CA-04** (US2, RF-D01) — Dada uma sessão autenticada, quando um deploy de rotina ocorre, então permaneço autenticado e a requisição em curso não retorna erro.
- **CA-05** (US3, RF-D08) — Dado um commit em `main` com CI verde, quando o pipeline conclui, então staging e, em seguida, produção rodam essa versão sem nenhum comando manual no servidor.
- **CA-06** (US4, RF-D09) — Dada uma versão que não sobe ou não responde ao *health check*, quando o deploy é tentado, então o tráfego continua na versão anterior e o pipeline é marcado como falho.
- **CA-07** (US5, RF-D08) — Dado que o deploy em staging falhou, quando o pipeline avalia a promoção, então produção não é alterada.
- **CA-08** (US5, RF-D06) — Dado o banco de staging ou de produção, quando inspeciono os dados após o deploy, então não existe nenhum usuário nem conteúdo das migrações de semente de desenvolvimento.
- **CA-09** (US6, RF-D12) — Dada uma versão defeituosa em produção, quando executo a reversão, então a versão anterior volta a atender em até 5 minutos, sem restaurar banco de dados.
- **CA-10** (US7, RF-D11) — Dado o backup diário, quando restauro o backup mais recente em ambiente limpo, então a aplicação sobe sobre esses dados e o histórico de respostas está íntegro.
- **CA-11** (US7, RF-D11) — Dado o servidor de aplicação perdido por completo, quando consulto o armazenamento de backup, então os backups dos últimos 7 dias estão disponíveis fora dele.
- **CA-12** (US8, RF-D12) — Dado um incidente, quando consulto logs e o endpoint de saúde, então obtenho a versão em execução e o estado da aplicação sem abrir sessão interativa no servidor.
- **CA-13** (US9, RF-D07) — Dada uma varredura no repositório, quando procuro credenciais de staging ou produção, então nenhuma é encontrada: todas vêm de variáveis de ambiente.
- **CA-14** (US9, RF-D07) — Dado o fluxo "esqueci minha senha" em produção, quando solicito a redefinição, então recebo um e-mail cujo link aponta para o domínio de produção, não para `localhost`.
- **CA-15** (Borda 1, RF-D09) — Dado um deploy interrompido por falha, quando verifico o ambiente, então ele permanece íntegro na versão anterior.
- **CA-16** (Borda 2, RF-D06) — Dada uma migração do Flyway que falha em staging, quando o pipeline prossegue, então produção não é tocada e a falha é reportada.
- **CA-17** (Borda 3, RF-D12) — Dado o servidor reiniciado, quando ele volta, então os serviços sobem sozinhos, na ordem correta, e a aplicação responde sem intervenção.
- **CA-18** (Borda 4, RF-D03) — Dado o certificado próximo do vencimento, quando a data de renovação chega, então ele é renovado automaticamente, sem queda e sem ação manual.
- **CA-19** (Borda 5, RF-D10) — Dados dois merges em `main` em sequência rápida, quando ambos disparam deploy, então não executam ao mesmo tempo e o ambiente termina na versão do commit mais recente.
- **CA-20** (US3, RF-D01) — Dada a imagem validada em staging, quando é promovida para produção, então é o **mesmo artefato**, identificado pelo mesmo SHA, sem reconstrução.
- **CA-21** (US9, RF-D02) — Dado o contêiner do backend em execução, quando inspeciono o processo da aplicação, então ele não roda como root.
- **CA-22** (US5, RF-D05) — Dada uma alteração de dados em staging, quando consulto produção, então nada mudou: os ambientes não compartilham banco, segredos nem domínio.

## Experiência do usuário

**Estudante.** A experiência desejada é a ausência de experiência: o produto simplesmente está no ar, em um endereço memorizável, com cadeado no navegador e sem tela de erro durante publicações. O único momento em que esta entrega se torna visível para ele é quando falha. Duas exigências herdadas de `PRD.md` §7 seguem valendo e não podem ser degradadas pela infraestrutura: **próxima questão em até 300 ms (p95)** e interface inteiramente em português do Brasil, incluindo qualquer página de erro servida pelo proxy (502, 503, manutenção).

**Operador.** O fluxo principal é: abrir *pull request* → CI verde → merge em `main` → acompanhar o pipeline → ver a versão em produção. Os fluxos de exceção precisam ser igualmente diretos: reverter, consultar logs, restaurar backup. Cada um deve ser executável sob pressão, a partir de documentação escrita antes de ser necessária — um procedimento de restauração redigido durante o incidente já é tarde demais.

**Acessibilidade.** Qualquer página servida fora da aplicação Angular — erro do proxy, manutenção — respeita o contraste mínimo de 4,5:1 e é navegável por teclado, como exige `PRD.md` §7.

## Restrições técnicas de alto nível

| Restrição | Detalhe |
| --- | --- |
| **Mesma origem obrigatória** | O frontend usa caminhos relativos `/api/v1/*` e cookie de refresh `SameSite=Strict`. Separar SPA e API em domínios distintos exigiria reescrever todas as chamadas e enfraquecer a proteção CSRF: está vedado nesta entrega. |
| **Hospedagem** | VPS com Docker Compose, decisão tomada. Implica Postgres auto-hospedado e, portanto, backup sob nossa responsabilidade — divergência consciente do "Postgres gerenciado com PITR" previsto em `TECH-SPEC.md` §9. |
| **Domínio a registrar** | Nenhum domínio existe hoje. É **pré-requisito bloqueante**: sem ele não há TLS válido, e sem TLS o cookie `Secure` do refresh token não trafega — a autenticação não funciona. |
| **Provedor de e-mail** | `PRD.md` §9 mantém em aberto o provedor transacional (SES ou SendGrid). Produção não pode usar MailHog: sem SMTP real, cadastro, convite e redefinição de senha ficam inoperantes. Decisão necessária antes do primeiro acesso público. |
| **Migrações retrocompatíveis** | `spring.flyway.baseline-on-migrate=false`: o banco de produção precisa nascer vazio e evoluir só por migrações versionadas. Padrão *expand/contract*, conforme `TECH-SPEC.md` §9. |
| **Instância única** | O Flyway roda na subida da aplicação. A topologia inicial pressupõe uma instância por ambiente; execução concorrente de migrações está fora do escopo. |
| **Segurança** | Nenhum segredo versionado. `certame.seguranca.cookie-secure` deve permanecer `true` e o segredo JWT ter no mínimo 32 bytes, como o `PropriedadesSeguranca` já valida. Porta administrativa e banco não expostos à internet. |
| **Privacidade (LGPD)** | Backups contêm dados pessoais de estudantes: exigem armazenamento com acesso restrito e retenção definida. O direito de exclusão previsto em `PRD.md` §7 alcança também as cópias de segurança. |
| **Disponibilidade** | 99,5% mensais, manutenção fora de 19h–23h BRT. |
| **Fuso horário** | O domínio opera em `America/Sao_Paulo` (`certame.estudo.zona`). Contêineres e banco não podem assumir o fuso do host. |
| **Desempenho** | A infraestrutura não pode consumir a folga do orçamento de 300 ms (p95) para avançar de questão, já validado em `playwright.prod.config.ts`. |

## Fora do escopo

- **Alta disponibilidade e escala horizontal.** Uma instância por ambiente. Balanceamento, réplicas e *failover* automático ficam para quando houver volume que justifique.
- **PITR (recuperação para um instante arbitrário).** Esta entrega garante backup diário com restauração testada, não recuperação contínua.
- **Kubernetes ou orquestração além do Docker Compose.**
- **CDN para os estáticos.** `TECH-SPEC.md` §9 previa frontend em CDN; a mesma origem exigida pelo código torna isso incompatível com esta entrega. Fica como melhoria futura, dependente de refatorar as chamadas de API.
- **Observabilidade completa.** Prometheus, Micrometer, logs JSON com `traceId` e alertas de 5xx/p95 estão previstos em `TECH-SPEC.md` §9, mas aqui entra apenas o mínimo para operar: logs acessíveis e *health check*. O restante é entrega própria.
- **Ambiente de revisão por pull request.** Só staging e produção.
- **Habilitar o gate de cobertura do frontend.** A métrica de *functions* está em 79,62% contra o limiar de 80% do `angular.json`, majoritariamente porque o v8 conta cada binding de template `.html` como função. Decidir se templates entram na métrica é assunto de qualidade, não de deploy.
- **Reduzir o bundle do frontend.** Hoje em 802 kB contra orçamento de aviso de 500 kB (teto de erro em 1 MB). Não bloqueia a publicação.
- **Migração de dados existentes.** Produção nasce vazia; não há base legada a importar.
- **Carga inicial de conteúdo.** As 5.000 questões previstas em `PRD.md` §9 são trabalho editorial, não de infraestrutura.
