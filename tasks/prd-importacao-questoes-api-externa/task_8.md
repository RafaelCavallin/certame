# Tarefa 8.0: Protótipo Nocturne — importações, mapeamentos, divergências e texto-base

## Visão geral

O `Certame.dc.html` **não tem** nenhuma tela de importação, e o cartão de questão não tem bloco de texto de apoio. Pela regra 1 do `AGENTS.md`, nada disso pode nascer direto no Angular: o protótipo aprovado é a fonte da verdade visual, e o frontend é tradução 1:1 dele.

Esta tarefa desenha três telas administrativas novas — `ADMIN: IMPORTAÇÕES`, `ADMIN: MAPEAMENTOS`, `ADMIN: DIVERGÊNCIAS` — e o bloco de texto-base no cartão de questão, usando **componentes Nocturne que já existem** (`.table`, `.field`, `.input`, `.select`, `.seg`, `.tag`, `.btn-primary`, `.dialog`, `.estado-vazio`), como foi feito em `prd-banco-de-questoes`. Não é redesenho e não inventa vocabulário visual.

Não produz código de aplicação nem teste automatizado. A validação é a **aprovação visual do usuário**, e é ela que libera a tarefa 9.0 — cujos seletores de E2E saem desta marcação.

<skills>
### Conformidade com skills

- `impeccable` — aplicável aos estados e à legibilidade da triagem, dentro do limite da regra 1: estados vazio, carregando, parcial e de erro com mensagem própria em pt-BR; motivo do descarte em **texto**, não só cor ou ícone; sinalizadores de pendência com rótulo acessível a leitor de tela; tabela com rolagem horizontal própria em telas menores; confirmação explícita antes de aplicar divergência em questão publicada.
- **Desvio registrado:** `impeccable` pediria pré-visualização antes de gravar e triagem em massa. Ambas estão **explicitamente fora do escopo** do PRD e ficam para as fases seguintes. O MVP entrega interface suficiente para revisar o que entrou, e nada além disso (RF-70).
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

`AGENTS.md` e as três rules lidos na íntegra. Regras que incidem aqui:

- **Regra 1** — o frontend é tradução 1:1 do protótipo. Estas telas entram **antes** do Angular, com os tokens `var(--*)` de `styles.css`. Nenhuma cor, fonte, espaçamento, raio ou sombra inventados; nenhuma biblioteca de UI de terceiros (RNF-09).
- **Regra 3 / RNF-09** — toda a interface e todas as mensagens em português do Brasil.
- **Acessibilidade** — rótulo textual em todo sinalizador, contraste mínimo 4,5:1, tabela navegável por teclado.
</rules>

<requirements>
- **RF-70** — tela de importações em `admin/`: formulário do pedido (fornecedor, filtros, quantidade de 1 a 50), lista dos lotes já executados e o relatório de cada lote.
- **RF-66** no relatório — fornecedor, filtros pedidos, solicitante, início e fim, totais e a distribuição pelos **dez** resultados.
- **RF-71** — lista das candidatas do lote com o resultado de cada uma e, para as importadas, link direto para a tela de edição de questão **que já existe** (`admin/questoes/:id`). Não há editor paralelo.
- **RF-72** — sinalizadores por questão importada: sem comentário, sem assunto, sem banca, sem órgão, sem dificuldade (RT-1) e divergência pendente — visíveis também como filtro na listagem de questões.
- **RF-73** — fila de pendências de mapeamento: valor externo, quantidade de candidatas afetadas e a ação de mapear. O diálogo de mapeamento oferece **apenas nós existentes** da árvore: não há caminho na interface para criar disciplina ou assunto a partir daqui (RN-15, CA-13).
- **RF-64** na tela de divergências — comparação lado a lado do conteúdo local e externo, com as três decisões e confirmação explícita antes de aplicar sobre questão publicada.
- **RF-57** — bloco de texto-base **antes** do enunciado no cartão de questão, com a mesma renderização de texto puro do restante.
- Estados com mensagem própria: "Nenhuma importação ainda", "Importando 18 de 50", "Lote parcial: o fornecedor interrompeu em 34 de 50", "Nenhuma candidata com este resultado".
</requirements>

## Subtarefas

- [ ] 8.1 Desenhar `ADMIN: IMPORTAÇÕES`: lista de lotes com código, fonte, situação, totais e data, e o botão de nova importação.
- [ ] 8.2 Desenhar o diálogo do pedido, com fornecedor, matéria, assunto, banca, órgão, ano e quantidade (1 a 50), e a mensagem de erro **no campo**, sem toast.
- [ ] 8.3 Desenhar o relatório do lote: cabeçalho com filtros e totais, a distribuição pelos dez resultados e a tabela de candidatas com motivo em texto e link para `admin/questoes/:id`.
- [ ] 8.4 Desenhar o estado de progresso ("Importando 18 de 50") e o estado de lote parcial com a causa.
- [ ] 8.5 Desenhar `ADMIN: MAPEAMENTOS`: fila de pendências ordenada por candidatas afetadas, o dicionário existente e o diálogo de mapear — só com nós existentes da árvore.
- [ ] 8.6 Desenhar `ADMIN: DIVERGÊNCIAS`: comparação lado a lado, o aviso de bloqueio por resposta e as três decisões, com o `dialog` de confirmação para questão publicada.
- [ ] 8.7 Acrescentar os sinalizadores de pendência à linha de `ADMIN: QUESTÕES` e os filtros de fonte e pendência à barra de filtros.
- [ ] 8.8 Acrescentar o bloco de procedência (fonte, identificador, data, link externo) ao `ADMIN: FORMULÁRIO DE QUESTÃO`, com a variante reduzida de questão `INTERNA`.
- [ ] 8.9 Acrescentar o bloco de texto-base ao cartão de questão da sessão de estudo e às telas de correção e resultado.
- [ ] 8.10 Conferir contraste ≥ 4,5:1, rótulo textual em cada sinalizador e rolagem horizontal própria das tabelas em telas menores.
- [ ] 8.11 Submeter as telas novas e as alteradas à **aprovação visual do usuário** — é ela que libera a tarefa 9.0.

## Detalhes de implementação

Ver TechSpec, seções *Visão dos componentes* → tabela **Frontend** (a lista das telas e o que cada uma consome) e a linha de `protótipo/Certame.dc.html`, *Conformidade com o AGENTS.md e as rules* → **Regra 1**, e *Conformidade com skills* → `impeccable`, com o desvio registrado. O PRD detalha os fluxos em *Experiência do usuário* → **Fluxos principais** e **Interface e acessibilidade**.

Os seletores usados aqui são os que a suíte E2E da tarefa 9.0 vai consumir.

## Critérios de aceitação relacionados

- CA-09
- CA-12
- CA-19
- CA-22
- CA-28

## Testes da tarefa

Sem testes automatizados. A validação é a aprovação visual do usuário, e a marcação resultante é exercitada pelos testes E2E-01 a E2E-06 da tarefa 9.0.

## Arquivos relevantes

- `protótipo/Certame.dc.html`
