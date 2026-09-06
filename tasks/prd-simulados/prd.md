# Documento de Requisitos do Produto (PRD)

> **Funcionalidade:** Simulados (visão do estudante e gestão administrativa) · **Fase:** F4 — Simulados
> Derivado de `PRD.md` (Certame v1.3). Cobre RF-14 e RF-21, apoiado em RN-01 e RN-04.

## Visão geral

O simulado é a prova de fogo do candidato: uma prova completa, com composição definida por disciplina, duração fixa e janela de aplicação, montada pela administração e aplicada a todos os estudantes nas mesmas condições. Diferente da sessão livre de estudo, o simulado tem nota, tem prazo e vale uma vez só.

Esta funcionalidade cobre as duas pontas do mesmo objeto: a lista de simulados do estudante — disponíveis, agendados e concluídos, com nota e revisão — e o CRUD administrativo que define código, nome, banca, número de questões, duração, janela, status e composição por disciplina. A aplicação do simulado reaproveita o Modo Prova da sessão de estudo, com finalização explícita e correção ao final.

## Objetivos

- **Simulado como ritual:** ≥ 50% dos estudantes ativos concluem ao menos um simulado por mês (premissa de meta, a calibrar após o lançamento).
- **Operação sem planilha:** 100% dos simulados publicados montados pela própria plataforma, sem seleção manual de questões fora dela.
- **Composição sempre válida:** 0 simulados publicados com composição incompatível com o número de questões declarado.
- **Resultado íntegro:** 100% das tentativas concluídas com nota, tempo total e revisão disponíveis imediatamente após a finalização.
- **Comparabilidade:** nenhuma tentativa refeita entra na contagem de ranking, garantindo que a nota registrada seja sempre a da primeira aplicação.

## Histórias de usuário

- **US1** — Como estudante, quero ver os simulados disponíveis, agendados e concluídos em uma lista única, para saber o que posso fazer agora e o que vem depois.
- **US2** — Como estudante, quero iniciar um simulado disponível e responder a prova completa sem gabarito, para simular a condição real.
- **US3** — Como estudante, quero ver minha nota ao finalizar e revisar a prova questão a questão, para aprender com o resultado.
- **US4** — Como estudante, quero que meu simulado concluído fique registrado com a nota, para acompanhar minha evolução.
- **US5** — Como administrador, quero cadastrar um simulado com código, nome, banca, número de questões, duração e janela de aplicação, para publicá-lo à base de estudantes.
- **US6** — Como administrador, quero definir a composição por disciplina, para que o simulado reflita a distribuição do edital.
- **US7** — Como administrador, quero que a publicação seja bloqueada quando a janela estiver ausente ou a composição não fechar com o número de questões, para não colocar no ar uma prova inconsistente.
- **Caso de borda 1** — Tento refazer um simulado já concluído: consigo revisá-lo e refazê-lo apenas como treino, sem alterar a nota registrada.
- **Caso de borda 2** — Abandono um simulado sem finalizar: a tentativa é descartada (RN-04).
- **Caso de borda 3** — A janela do simulado encerra enquanto estou respondendo: o comportamento é definido pela duração já iniciada, e o simulado deixa de aceitar novas entradas.

## Principais funcionalidades

### 1. Lista de simulados do estudante (RF-14)

Uma tela, três estados: o que está aberto agora, o que está agendado e o histórico com notas.

- **RF-14** — Lista de simulados disponíveis, agendados e concluídos, com nota. Um simulado concluído não pode ser refeito para efeito de ranking, mas permite revisão.

### 2. Aplicação do simulado

O simulado é aplicado no Modo Prova (`prd-sessao-de-estudo`, RF-09 e RF-10): série completa, sem feedback, com finalização explícita e correção ao final.

- **RN-01** — Somente questões publicadas compõem o simulado.
- **RN-04** — As respostas só são consideradas após a finalização; abandono descarta a tentativa.

### 3. CRUD de simulados (RF-21)

Ferramenta do administrador para montar e publicar provas.

- **RF-21** — CRUD de simulados com código, nome, banca, número de questões, duração, janela, status e composição por disciplina. Publicar exige janela definida e quantidade compatível com a composição.

## Critérios de aceitação

- **CA-01** (US1, RF-14) — Dada a tela de simulados, quando a abro, então vejo três agrupamentos — disponíveis, agendados e concluídos — com nome, banca, número de questões e duração.
- **CA-02** (US1, RF-14) — Dado um simulado concluído, quando o vejo na lista, então sua nota e a data de conclusão são exibidas.
- **CA-03** (US2, RF-14) — Dado um simulado disponível dentro da janela, quando o inicio, então a prova abre em Modo Prova com o número de questões e a duração declarados.
- **CA-04** (US2, RN-01) — Dada a montagem de um simulado, quando a série é gerada, então ela contém apenas questões publicadas e respeita a composição por disciplina.
- **CA-05** (US3, RF-14) — Dado um simulado finalizado, quando vejo o resultado, então tenho nota, acertos, erros, não respondidas, tempo total e revisão questão a questão.
- **CA-06** (Borda 1, RF-14) — Dado um simulado já concluído, quando o acesso novamente, então posso revisá-lo, e qualquer nova execução não altera a nota registrada nem conta para o ranking.
- **CA-07** (Borda 2, RN-04) — Dada uma tentativa de simulado abandonada sem finalização, quando volto à lista, então o simulado continua sem nota e a tentativa não aparece no histórico.
- **CA-08** (US5, RF-21) — Dado o formulário de simulado, quando cadastro código, nome, banca, número de questões, duração e janela, então o simulado é salvo com status não publicado.
- **CA-09** (US6, RF-21) — Dada a composição por disciplina, quando informo as quantidades, então a soma é exibida e comparada ao número de questões declarado.
- **CA-10** (US7, RF-21) — Dado um simulado sem janela definida ou com composição cuja soma difere do número de questões, quando tento publicá-lo, então a publicação é recusada com a indicação do que está inconsistente.
- **CA-11** (US7, RF-21) — Dado um simulado publicado, quando a janela ainda não começou, então ele aparece como agendado para o estudante; quando a janela está aberta, aparece como disponível; após o encerramento, deixa de aceitar novas tentativas.
- **CA-12** (RF-21) — Dado um usuário sem perfil de administrador, quando tenta criar, editar ou publicar um simulado, então a operação é recusada no servidor.

## Experiência do usuário

**Perfis e necessidades.** O estudante quer saber, em segundos, o que pode fazer agora e como foi da última vez. O administrador monta poucos simulados por mês, mas cada erro de composição atinge toda a base — a tela precisa dar segurança antes de publicar.

**Fluxos principais.**
1. *Fazer simulado* — Simulados → disponível → iniciar → Modo Prova → finalizar → resultado.
2. *Revisar* — Simulados → concluído → revisão questão a questão.
3. *Montar simulado* — Administração → Simulados → novo → dados gerais → composição por disciplina → publicar.
4. *Agendar* — Administração → Simulados → definir janela → publicar (o estudante o vê como agendado até a abertura).

**UI/UX e acessibilidade.**
- Tradução 1:1 do protótipo aprovado sobre o design system Nocturne; estados (disponível, agendado, concluído) sinalizados por rótulo textual, não apenas por cor.
- A aplicação do simulado herda a interface silenciosa do Modo Prova: sem gabarito, sem estatísticas e sem elementos que induzam resposta.
- Cronômetro da prova sempre visível, com aviso claro ao aproximar-se do fim da duração.
- Navegação por teclado completa, foco visível e contraste mínimo de 4,5:1.
- Datas, horários e notas em formato brasileiro; textos em português do Brasil.

## Restrições técnicas de alto nível

- **Stack não negociável:** Angular 22 no frontend; Spring Boot 4.1 / Java 25, PostgreSQL 18 e Flyway no backend.
- **Reuso obrigatório:** a aplicação do simulado usa o Modo Prova já especificado em `prd-sessao-de-estudo`, sem interface paralela.
- **Autorização no servidor:** criar, editar, publicar e agendar simulados exige perfil administrador ou superior.
- **Auditoria:** publicação, despublicação e alteração de janela são ações administrativas auditadas.
- **Integridade da nota:** a tentativa registrada é imutável e vinculada ao usuário, ao simulado e ao tempo gasto.
- **Dependência:** exige acervo publicado suficiente por disciplina para fechar a composição (`prd-banco-de-questoes`).
- **Idioma:** português do Brasil.

## Fora do escopo

- Ranking público, colocação entre candidatos e comparação de notas entre usuários.
- Simulados personalizados gerados pelo estudante e simulados adaptativos.
- Correção de questões discursivas.
- Proctoring, controle antifraude e bloqueio de troca de aba.
- Certificados ou relatórios de simulado exportáveis em PDF.
- Notificações de abertura de janela (push fora da v1).
- Editais e sua relação com as disciplinas cobradas — em `prd-editais` (RF-17, RF-22).
