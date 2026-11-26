# Prompt --- Criação de PRD para Integração de APIs de Questões no Certame

Atue como **Product Manager sênior com forte experiência em arquitetura
de software, integrações REST, Spring Boot, Angular, PostgreSQL e
produtos EdTech**.

Sua tarefa é **criar uma PRD (Product Requirements Document) completa e
implementável** para adicionar ao projeto **Certame** uma funcionalidade
de **importação de questões de concursos públicos a partir de APIs
externas**.

## 1. Contexto do produto

O **Certame** é uma plataforma de estudos para concursos públicos. O
projeto já existe e possui backend, frontend, banco de dados e domínio
implementados.

**IMPORTANTE:** antes de escrever a PRD, analise o código-fonte atual do
projeto que será fornecido junto com este prompt. **Não presuma a
arquitetura, entidades, campos, endpoints, pacotes ou fluxos com base
apenas nesta descrição. O código atual é a fonte de verdade.**

A solução deve se adaptar ao Certame existente, buscando **alterações
mínimas, coesas e compatíveis com a arquitetura atual**.

O projeto utiliza, entre outras tecnologias:

-   Java + Spring Boot no backend;
-   Angular no frontend;
-   PostgreSQL;
-   Flyway para migrations;
-   arquitetura organizada por domínios/módulos.

O Certame já possui funcionalidades relacionadas a questões,
alternativas, disciplinas, assuntos, bancas, órgãos, estudo,
respostas/desempenho, cadernos, simulados, fluxo editorial e outras
funcionalidades. Identifique no código quais delas realmente existem e
como estão implementadas antes de propor qualquer mudança.

## 2. Problema a resolver

O Certame precisa construir um acervo significativo de questões para
concursos.

O cadastro exclusivamente manual torna difícil atingir rapidamente um
volume relevante de questões publicadas. Queremos, portanto, permitir
que questões provenientes de fornecedores externos sejam **importadas e
normalizadas para o modelo interno do Certame**, sem transformar a
aplicação em um simples proxy dessas APIs.

A questão importada deve, após importação e eventual revisão, funcionar
como uma questão nativa do Certame e participar normalmente dos fluxos
já existentes.

## 3. Diretriz arquitetural principal

Considere como hipótese inicial a arquitetura:

``` text
API externa
    ↓
cliente/adaptador de integração
    ↓
DTO externo
    ↓
normalização / anti-corruption layer
    ↓
mapeamento de taxonomia
    ↓
modelo interno do Certame
    ↓
persistência local
    ↓
fluxo editorial existente
    ↓
questão publicada
    ↓
estudo / cadernos / simulados / desempenho
```

Entretanto, **valide essa abordagem contra o código existente** e
proponha ajustes caso haja uma solução mais coerente com a arquitetura
real.

A aplicação **não deve depender da disponibilidade da API externa
durante uma sessão de estudo**.

## 4. Fornecedores considerados

Considere inicialmente suporte a múltiplos fornecedores, mesmo que o MVP
implemente somente um.

Fornecedores atualmente avaliados:

1.  **QAPI**
2.  **Quest API**
3.  **API das Questões**

O primeiro PoC/MVP deverá considerar **QAPI como primeiro fornecedor**,
mas a arquitetura não deve criar acoplamento desnecessário a ela.

A integração deve permitir futuramente adicionar outro fornecedor sem
reescrever o domínio de questões.

Exemplo conceitual:

``` text
QAPI DTO ─────────┐
                  ↓
Quest API DTO → QuestaoExterna → domínio Certame
                  ↑
Outra API DTO ────┘
```

Avalie a necessidade de uma representação intermediária, como
`QuestaoExterna`, em vez de permitir que DTOs específicos dos
fornecedores circulem pelo domínio.

## 5. Dados disponíveis na QAPI

Considere que a QAPI pode fornecer dados equivalentes a:

``` json
{
  "_id": "identificador externo",
  "orgao": "TJ/SE",
  "banca": "FGV",
  "ano": 2023,
  "cargo": "Analista Judiciário - Análise de Sistemas",
  "materia": "Conhecimento Específico",
  "assunto": "Arquitetura de Computadores",
  "enunciado": "...",
  "opcaoA": "...",
  "opcaoB": "...",
  "opcaoC": "...",
  "opcaoD": "...",
  "opcaoE": "...",
  "gabarito": "A",
  "comentario": "...",
  "texto": "QTXT566467"
}
```

Existe também suporte a textos-base associados através de endpoint
próprio.

**Não trate esse exemplo como contrato definitivo.** A PRD deve prever
validação do contrato real/documentação antes da implementação.

## 6. Procedência da questão

Analise a necessidade de adicionar à questão informações que permitam
saber sua procedência, como:

-   origem/fornecedor;
-   identificador externo;
-   URL ou referência externa, quando disponível;
-   data da importação;
-   data da última sincronização/verificação, se necessária.

Uma possibilidade conceitual é:

``` text
origem = QAPI
identificador_externo = abc123
```

Considere uma enumeração semelhante a:

``` text
INTERNA
QAPI
QUEST_API
API_DAS_QUESTOES
```

Mas escolha nomes e modelagem coerentes com os padrões reais encontrados
no projeto.

A solução deve impedir duplicação da mesma questão em novas importações.
Avalie uma restrição lógica/bancária equivalente a:

``` text
UNIQUE (origem, identificador_externo)
```

Não substitua o identificador/código interno existente do Certame pelo
identificador do fornecedor.

## 7. Fluxo editorial

Analise o fluxo editorial atual do Certame no código.

Questões importadas **não devem automaticamente contornar as regras
editoriais existentes**.

Defina na PRD:

-   em qual status uma questão importada deve entrar;
-   quando ela pode ser publicada;
-   quais validações existentes devem continuar sendo aplicadas;
-   como tratar questões incompletas;
-   como tratar questões sem comentário/resolução;
-   como tratar questões cujo gabarito ou conteúdo externo seja
    posteriormente alterado.

Se o Certame atualmente exigir comentário/resolução para publicação,
preserve essa regra salvo se houver uma justificativa forte e
explicitamente documentada para alterá-la.

## 8. Comentário/resolução

Esse é um requisito crítico.

A QAPI aparentemente disponibiliza um campo de comentário/resolução. A
PRD deve prever validação da:

-   presença;
-   qualidade;
-   formatação;
-   completude;
-   compatibilidade com as regras editoriais do Certame.

A existência do campo não significa automaticamente que o conteúdo deve
ser considerado adequado para publicação.

Defina como questões sem comentário válido devem ser tratadas.

## 9. Taxonomia

Este é um dos principais problemas do projeto.

APIs externas podem utilizar nomes diferentes dos existentes no Certame.

Exemplos:

``` text
API: "Informática"
Certame: "Tecnologia da Informação"
```

ou:

``` text
API: "Conhecimentos de Informática"
Certame: "Informática"
```

Não crie automaticamente novas disciplinas/assuntos apenas porque o
fornecedor retornou um nome desconhecido.

A PRD deve especificar uma estratégia de **mapeamento de taxonomia
externa → taxonomia interna**, considerando pelo menos:

-   disciplina;
-   assunto;
-   banca;
-   órgão;
-   eventualmente cargo/prova, caso faça sentido após analisar o modelo
    atual.

Avalie a necessidade de persistir mapeamentos, por exemplo:

``` text
origem
tipo_taxonomia
valor_externo
entidade_interna_id
```

A solução deve permitir:

-   reutilizar mapeamentos;
-   detectar valores ainda não mapeados;
-   impedir importação/publicação quando faltar um mapeamento
    obrigatório;
-   corrigir mapeamentos;
-   auditar o que aconteceu durante uma importação.

## 10. Tipos de questão

Analise os tipos suportados atualmente pelo Certame.

A importação deve mapear corretamente, quando aplicável:

-   múltipla escolha;
-   certo/errado.

Defina como detectar o tipo quando o fornecedor não o informar
explicitamente.

Não importe silenciosamente um tipo que o Certame não suporte.

## 11. Texto-base

Algumas questões possuem texto-base compartilhado.

A QAPI pode fornecer uma referência para esse texto através de outro
endpoint.

A PRD deve definir como o MVP tratará isso.

Avalie opções como:

-   incorporar o texto-base ao enunciado;
-   criar/modelar texto-base separadamente;
-   adiar modelagem própria para uma versão posterior.

Priorize a solução que cause menor impacto no domínio atual, desde que
não prejudique a apresentação correta da questão.

## 12. Imagens, anexos, HTML e LaTeX

Alguns fornecedores podem retornar:

-   imagens;
-   anexos;
-   HTML;
-   fórmulas/LaTeX;
-   tabelas;
-   textos com formatação especial.

Analise o que o Certame suporta atualmente.

Para o MVP, considere a possibilidade de **não importar questões que
dependam de recursos ainda não suportados**, em vez de aumentar
excessivamente o escopo.

A PRD deve definir como identificar, rejeitar ou sinalizar esses casos.

Nenhuma questão deve ser importada de forma que fique incompleta ou
incorreta para o estudante.

## 13. Importação idempotente

A importação deve ser segura para repetição.

Executar duas vezes uma importação equivalente não deve gerar duas
cópias da mesma questão.

A PRD deve abordar:

-   identificação externa;
-   deduplicação;
-   atualização;
-   conflitos;
-   reprocessamento;
-   falhas parciais.

## 14. Alterações posteriores no fornecedor

Considere que uma questão externa pode posteriormente sofrer:

-   mudança de gabarito;
-   anulação;
-   correção de enunciado;
-   correção de alternativa;
-   alteração de classificação/taxonomia;
-   remoção.

Uma questão já publicada e respondida por estudantes **não deve ser
modificada silenciosamente**.

Defina um fluxo para detectar alterações externas e encaminhá-las para
revisão antes de alterar conteúdo relevante no Certame.

Preserve a integridade do histórico de respostas e desempenho.

## 15. Relatório de importação

A importação deve produzir um resultado auditável.

Considere estados/resultados como:

``` text
encontrada
importável
importada
já existente
ignorada
sem mapeamento
sem comentário
tipo não suportado
recurso não suportado
erro
alteração externa detectada
```

A PRD deve especificar quais informações precisam ser registradas e
apresentadas.

## 16. PoC inicial

Antes de desenvolver uma interface administrativa completa, queremos um
**PoC pequeno e controlado**.

Proponha um PoC que:

1.  conecte à QAPI;
2.  recupere aproximadamente 20--50 questões;
3.  preferencialmente utilize questões de Tecnologia da Informação e
    bancas relevantes;
4.  converta o DTO externo para uma representação interna neutra;
5.  valide compatibilidade com o domínio do Certame;
6.  **não precise persistir as questões no primeiro teste**, caso isso
    facilite validar a integração;
7.  gere um relatório semelhante a:

``` text
50 questões encontradas

42 importáveis
3 já existentes
2 sem comentário
2 sem mapeamento de assunto
1 recurso não suportado
```

Defina critérios objetivos para considerar o PoC aprovado ou reprovado.

## 17. Segurança

A PRD deve contemplar:

-   API Key somente no backend;
-   configuração por variável de ambiente/configuração segura;
-   nunca enviar credenciais ao Angular;
-   timeouts;
-   tratamento de indisponibilidade;
-   rate limiting do fornecedor;
-   logs sem exposição de segredo;
-   validação/sanitização de conteúdo externo;
-   proteção contra HTML ou conteúdo potencialmente inseguro vindo do
    fornecedor.

## 18. Licenciamento e dependências externas

Este é um **bloqueador de negócio**, não apenas uma observação jurídica.

Antes da importação em produção, deve haver confirmação do fornecedor
sobre:

1.  possibilidade de persistir questões no PostgreSQL do Certame;
2.  possibilidade de exibi-las para usuários cadastrados;
3.  possibilidade de exibi-las para usuários pagantes/assinantes;
4.  possibilidade de continuar utilizando questões já importadas após
    eventual cancelamento do serviço;
5.  possibilidade de enviar uma questão individual para um serviço de IA
    generativa para produzir explicações, sem treinamento/fine-tuning;
6.  requisitos de atribuição da fonte;
7.  restrições de redistribuição.

A PRD deve representar essas confirmações como **pré-condições/gates
para produção** quando aplicável.

Não assuma direitos que não estejam explicitamente confirmados.

## 19. Fora do escopo inicial

Considere inicialmente fora do escopo, salvo se a análise do projeto
demonstrar necessidade:

-   importação de toda a base do fornecedor;
-   sincronização automática em grande escala;
-   interface administrativa sofisticada;
-   importação CSV/Excel genérica;
-   geração automática de comentários por IA;
-   suporte completo a imagens/áudio/vídeo;
-   suporte completo a fórmulas se ainda inexistente;
-   múltiplos fornecedores funcionando simultaneamente no MVP;
-   alteração do fluxo de estudo para consultar API externa em tempo
    real.

## 20. Evolução futura

A PRD deve deixar espaço para fases posteriores, como:

-   interface administrativa de importação;
-   preview antes da importação;
-   importação em lote;
-   jobs assíncronos;
-   sincronização periódica;
-   segundo fornecedor;
-   comparação/deduplicação entre fornecedores;
-   suporte a imagens;
-   suporte a LaTeX;
-   tratamento de provas completas;
-   geração assistida de comentários;
-   métricas de qualidade por fornecedor;
-   auditoria completa.

Não implemente essas funcionalidades no MVP apenas por serem
possibilidades futuras.

## 21. O que espero da PRD

Produza uma PRD detalhada, mas orientada à implementação.

Ela deve conter, no mínimo:

1.  **Resumo executivo**
2.  **Contexto**
3.  **Problema**
4.  **Objetivos**
5.  **Não objetivos**
6.  **Personas/atores envolvidos**, apenas quando realmente relevantes
7.  **Premissas e restrições**
8.  **Análise do estado atual do Certame**, citando classes, entidades,
    migrations, services, endpoints e fluxos reais encontrados no código
9.  **Fluxo proposto**
10. **Requisitos funcionais**, numerados (`RF-001`, `RF-002`...)
11. **Requisitos não funcionais**, numerados (`RNF-001`, `RNF-002`...)
12. **Regras de negócio**, numeradas (`RN-001`, `RN-002`...)
13. **Modelo conceitual das alterações**
14. **Impactos no banco de dados**
15. **Impactos no backend**
16. **Impactos no frontend**
17. **Estratégia de integração com fornecedores**
18. **Estratégia de mapeamento de taxonomia**
19. **Estratégia de deduplicação/idempotência**
20. **Tratamento de atualizações externas**
21. **Tratamento de erros e falhas parciais**
22. **Segurança**
23. **Observabilidade e auditoria**
24. **Licenciamento e gates para produção**
25. **PoC**
26. **Critérios de aceite do PoC**
27. **MVP**
28. **Critérios de aceite do MVP**
29. **Fora do escopo**
30. **Riscos e mitigações**
31. **Métricas de sucesso**
32. **Plano de rollout/fases**
33. **Questões em aberto**
34. **Decisões arquiteturais que precisam de ADR**, quando aplicável.

## 22. Regras para elaboração

Siga estas regras rigorosamente:

-   **Analise primeiro o código atual do Certame.**
-   Não invente entidades, campos, endpoints ou regras que já possam ser
    verificados no repositório.
-   Diferencie claramente **estado atual**, **mudança proposta** e
    **possibilidade futura**.
-   Não proponha reescrever funcionalidades que já funcionam.
-   Prefira mudanças incrementais.
-   Preserve compatibilidade com os fluxos atuais sempre que possível.
-   Não acople o domínio à QAPI.
-   DTOs específicos de fornecedores devem ficar restritos à camada de
    integração.
-   Não permita que a disponibilidade da API externa afete sessões
    normais de estudo.
-   Não permita publicação silenciosa de conteúdo incompleto.
-   Não permita alteração silenciosa de questão já publicada devido a
    mudança externa.
-   Não crie automaticamente taxonomia interna a partir de valores
    externos desconhecidos.
-   Não exponha API Keys ao frontend.
-   Não assuma permissão jurídica para persistência ou redistribuição.
-   Identifique explicitamente qualquer ponto que precise de validação
    antes da implementação.
-   Evite overengineering: diferencie claramente o necessário para
    **PoC**, **MVP** e **futuro**.
-   Quando houver mais de uma solução possível, apresente alternativas,
    trade-offs e uma recomendação.
-   Sempre justifique alterações estruturais relevantes com base no
    código existente ou em um requisito concreto.

## 23. Resultado esperado

A PRD deve ser suficientemente precisa para que, após aprovação, outra
IA ou um desenvolvedor consiga transformá-la em:

``` text
PRD
 ↓
Plano técnico
 ↓
Issues/Tasks
 ↓
Implementação
 ↓
Testes
 ↓
Pull Request
```

Não escreva código de produção nesta etapa.

Não implemente a funcionalidade.

**Entregue somente a PRD.**
