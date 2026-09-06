# Tarefa 1.0: Lacunas do protótipo e nova tela `ADMIN: CATÁLOGO` (`Certame.dc.html`)

## Visão geral

O protótipo aprovado já contém as três telas administrativas (`ADMIN: QUESTÕES`, `ADMIN: FORMULÁRIO DE QUESTÃO`, `ADMIN: MATÉRIAS`), mas cinco peças exigidas por esta funcionalidade não existem nele, e uma quarta tela inteira — `ADMIN: CATÁLOGO`, para RF-37 (cadastro de bancas e órgãos pelo administrador) — nem chegou a ser desenhada, porque esse requisito foi acrescentado depois da primeira versão da TechSpec. A regra 1 do `AGENTS.md` impede que qualquer marcação nasça direto no Angular, então tudo isso entra primeiro no protótipo, com componentes Nocturne existentes, e a aprovação visual do usuário libera a tarefa 9.0.

Esta tarefa não produz código de aplicação nem testes automatizados; ela desbloqueia E2E-02, E2E-08, E2E-11, E2E-13 e E2E-16, cujos seletores saem desta marcação.

<skills>
### Conformidade com skills

- `impeccable` — **não aplicável**: o layout é congelado pela regra 1 do `AGENTS.md`. As cinco lacunas são acréscimos com componentes Nocturne já existentes, não redesenho.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

`AGENTS.md` lido na íntegra. Não existe `.agents/rules/` neste projeto. Regra relevante: regra 1 — nenhuma marcação nasce no Angular; todo componente novo usa `var(--*)` de `styles.css` do Nocturne, sem valor cru e sem biblioteca de UI de terceiros. Regra 3 — todos os rótulos acrescentados em português do Brasil. Desvio registrado e deliberado: o ícone "Duplicar" existente no protótipo é **removido**, porque o PRD não pede a funcionalidade (decisão registrada em *Riscos conhecidos* da TechSpec).
</rules>

<requirements>
- **Campo de busca por texto** na listagem de questões — hoje a tela só tem três `select`; CA-02 exige termo livre combinado com filtros (componente `.input`).
- **Ações de publicar e despublicar na linha** da listagem (US4), visíveis apenas para o perfil administrador.
- **Bloco de gabarito Certo/Errado** no formulário (CA-04) — hoje o bloco de alternativas é fixo; o tipo `CE` substitui o bloco inteiro pela escolha entre Certo e Errado (componente `.seg`).
- **Bloco de erro inline de publicação** (CA-06) — lista de campos pendentes marcada campo a campo, sem perder o que foi digitado.
- **Diálogo de confirmação** para ações destrutivas (excluir, despublicar, arquivar), com a variante que troca "Excluir" por "Arquivar" quando o assunto tem questões vinculadas (CA-11) — componente `.dialog` com o papel `--color-danger-*`.
- **Campo Órgão vira `select`**, alinhado ao modelo `orgao_id` da §5 e à decisão de semear o catálogo (hoje é `input` livre).
- **Remoção do ícone "Duplicar"** da linha da listagem.
- Rótulos de status com texto, nunca apenas cor, com contraste mínimo 4,5:1.
- **Nova tela `ADMIN: CATÁLOGO`** (RF-37) — cadastro de bancas e órgãos pelo administrador, com listagem, formulário de criar/editar (nome e sigla) e exclusão com confirmação. Reusa o padrão visual das outras telas administrativas (`.table`, `.field`, `.input`, `.dialog`), não é redesenho.
</requirements>

## Subtarefas

- [x] 1.1 Acrescentar o campo de busca por texto à barra de filtros da tela `ADMIN: QUESTÕES`, usando `.input`.
- [x] 1.2 Acrescentar as ações de publicar e despublicar na linha da tabela, junto de editar e excluir.
- [x] 1.3 Remover o ícone "Duplicar" da linha da tabela.
- [x] 1.4 Acrescentar o bloco de gabarito Certo/Errado ao `ADMIN: FORMULÁRIO DE QUESTÃO`, como alternativa ao bloco de alternativas quando o tipo é `CE`.
- [x] 1.5 Acrescentar o bloco de erro inline de publicação ao formulário, com marcação por campo pendente.
- [x] 1.6 Trocar o campo Órgão de `input` livre para `select`.
- [x] 1.7 Acrescentar o diálogo de confirmação de ação destrutiva, com a variante "excluir → arquivar" da tela `ADMIN: MATÉRIAS`.
- [x] 1.8 Conferir contraste ≥ 4,5:1 e rótulo textual em todos os estados de status acrescentados.
- [x] 1.9 Desenhar a tela `ADMIN: CATÁLOGO` (listagem e formulário de banca/órgão) com os componentes Nocturne já usados nas demais telas administrativas.
- [x] 1.10 Submeter as telas alteradas e a tela nova à aprovação visual do usuário — é ela que libera a tarefa 9.0.

## Detalhes de implementação

Ver TechSpec, seções *Riscos conhecidos* (as cinco lacunas, a ausência de tela para RF-37, o desvio do "Duplicar" e o campo Órgão) e *Sequenciamento do desenvolvimento* (passo 1) e *Conformidade com o AGENTS.md e as rules* (regra 1). Os seletores usados aqui (`.table`, `.field`, `.input`, `.seg`, `.tag`, `.btn-primary`, `.dialog`) são os que a suíte E2E da tarefa 10.0 vai consumir.

## Critérios de aceitação relacionados

- CA-02
- CA-04
- CA-06
- CA-11
- CA-13
- CA-14

## Testes da tarefa

Sem testes automatizados. A validação é a aprovação visual do usuário, e a marcação resultante é exercitada pelos testes E2E da tarefa 10.0 (E2E-02, E2E-08, E2E-11, E2E-13, E2E-15, E2E-16).

## Arquivos relevantes

- `protótipo/Certame.dc.html`
