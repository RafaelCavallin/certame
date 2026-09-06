# Tarefa 9.0: Telas de acesso (tradução 1:1 do protótipo)

## Visão geral

Implementa as três páginas Angular da funcionalidade em tradução 1:1 do `protótipo/Certame.dc.html` estendido e aprovado: a tela única de acesso com as abas Entrar / Criar conta, o painel de "Esqueci minha senha" com seu estado de confirmação, e a tela de definir nova senha nos três estados (formulário, concluído e link inválido). Não se inventa marcação, cor, espaçamento ou raio.

> **Pré-requisito atendido:** a aprovação visual do protótipo (passo 8c) foi concedida pelo usuário. Esta tarefa está liberada.

<skills>
### Conformidade com skills

- `angular-developer` — componentes standalone, detecção zoneless, Reactive Forms, `input()` para o parâmetro de rota, estilos de componente, ARIA e acessibilidade, convenções de nomenclatura.
- `impeccable` — **não aplicável**: o layout é congelado pela regra 1 do `AGENTS.md`.
</skills>

<rules>
### Conformidade com o AGENTS.md e as rules

`AGENTS.md` lido na íntegra. Não existe `.agents/rules/`. Regra 1 é a regra central: mesma marcação e mesmos tokens do protótipo aprovado, sempre por `var(--*)` de `styles.css`, sem hex cru e sem biblioteca de UI de terceiros. Regra 3: toda a interface e todas as mensagens em português do Brasil. **Decisão registrada na TechSpec:** o bloco "Entrar como (demonstração)" do protótipo é artifício de demonstração e **não** é implementado — em produção o perfil vem do JWT.
</rules>

<requirements>
- **RF-01** — `AcessoPage` com o controle segmentado Entrar / Criar conta, alternando sem recarregar a página; a aba ativa vem da rota (`/login` ou `/cadastro`). O campo Nome é exclusivo do cadastro.
- **CA-02** — Credencial inválida exibe erro inline abaixo do campo de senha, o e-mail digitado permanece preenchido e o foco vai para o campo de senha.
- **CA-05** — E-mail duplicado no cadastro marca o campo de e-mail sem limpar nome e senha.
- **RF-03** — `EsqueciSenhaPage` com o pedido e a tela de confirmação exibida incondicionalmente; e-mail malformado é barrado por validação inline antes do envio (CA-07).
- **RF-04** — `DefinirSenhaPage` consumindo `:token` da rota, com os três estados do protótipo; token inválido ou expirado exibe o aviso com a ação de solicitar um novo (CA-09).
- **RF-05** — Ação "Sair" no rodapé da navegação, disponível em todos os perfis, limpando o `AuthStore` imediatamente (CA-12).
- Acessibilidade: rótulos associados aos campos, atributos de autocompletar adequados para gerenciadores de senha, navegação completa por teclado (`Tab` entre campos, `Enter` submete) e foco visível em todos os controles.
- Erros sempre inline e próximos ao campo, nunca em alerta modal.
</requirements>

## Subtarefas

- [x] 9.1 Implementar `features/auth/acesso-page` com as abas, os três campos e o bloco de erro inline, consumindo `AuthStore.entrar` e `AuthStore.cadastrar`.
- [x] 9.2 Ligar a aba ativa à rota, de modo que `/login` e `/cadastro` rendereizem o mesmo componente com a aba pré-selecionada.
- [x] 9.3 Implementar o comportamento de erro de CA-02: preservar o e-mail e devolver o foco ao campo de senha.
- [x] 9.4 Implementar `features/auth/esqueci-senha-page` com o formulário e o estado de confirmação incondicional.
- [x] 9.5 Implementar `features/auth/definir-senha-page` com os três estados (formulário, concluído, link inválido) e o consumo do `:token`.
- [x] 9.6 Implementar a ação "Sair" no rodapé da navegação, chamando `AuthStore.sair`.
- [x] 9.7 Substituir os placeholders de rota da tarefa 7.0 pelos componentes reais e aplicar as guardas.
- [x] 9.8 Revisar rótulos, `autocomplete`, ordem de tabulação e foco visível nas quatro telas.
- [x] 9.9 Conferir, contra o protótipo, que nenhuma marcação, cor, espaçamento ou raio foi inventado, e que o bloco "Entrar como (demonstração)" não foi implementado.

## Detalhes de implementação

Ver TechSpec, seções *Visão dos componentes → Frontend* (tabela de componentes `features/auth/*`), *Sequenciamento do desenvolvimento → Ordem de construção*, item 9, e *Principais decisões* (uma página e duas rotas; o seletor de demonstração não é implementado; as lacunas de design entram pelo protótipo). A fonte visual é `protótipo/Certame.dc.html`, com os seletores `.field`, `.input`, `.btn-primary` e o controle segmentado das abas.

## Critérios de aceitação relacionados

- CA-01
- CA-02
- CA-03
- CA-04
- CA-05
- CA-06
- CA-07
- CA-08
- CA-09
- CA-12

## Testes da tarefa

Os casos de teste desta camada são os E2E, executados na tarefa 10.0 (E2E-01 a E2E-10), que dependem das três páginas concluídas aqui.

## Arquivos relevantes

- `frontend/src/app/features/auth/acesso-page/`
- `frontend/src/app/features/auth/esqueci-senha-page/`
- `frontend/src/app/features/auth/definir-senha-page/`
- `frontend/src/app/app.routes.ts`
- `protótipo/Certame.dc.html` (fonte visual, não alterada nesta tarefa)
