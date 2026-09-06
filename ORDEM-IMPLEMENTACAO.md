# Ordem de implementação dos PRDs

> Baseado nas dependências declaradas em [PRD.md](PRD.md) (§10 — PRDs derivados).

1. **[Acesso e conta](tasks/prd-acesso-e-conta/prd.md)** (F1) — autenticação e perfis; pré-requisito de toda tela.
2. **[Banco de questões e taxonomia](tasks/prd-banco-de-questoes/prd.md)** (F1) — acervo publicado e árvore de matérias/assuntos, necessários para sessão de estudo, simulados, editais e flashcards (RN-01).
3. **[Início e sessão de estudo](tasks/prd-sessao-de-estudo/prd.md)** (F2) — depende de 1 e 2; gera o histórico imutável de respostas (RN-02) e hospeda a criação de flashcard.
4. **[Cadernos e desempenho](tasks/prd-cadernos-e-desempenho/prd.md)** (F3) — consome o histórico de respostas de 3.
5. **[Simulados](tasks/prd-simulados/prd.md)** (F4) — reaproveita o Modo Prova de 3 e o acervo de 2.
6. **[Editais](tasks/prd-editais/prd.md)** (F4) — depende da taxonomia de 2; pode ser feito em paralelo com 5.
7. **[Gestão de usuários](tasks/prd-gestao-de-usuarios/prd.md)** (F5) — depende só de 1; pode avançar em paralelo com 4–6 se houver capacidade.
8. **[Flashcards — criação e gerenciamento](tasks/prd-flashcards-criacao-e-gerenciamento/prd.md)** (F6) — depende da sessão de estudo (3).
9. **[Flashcards — decks e revisão espaçada](tasks/prd-flashcards-decks-e-revisao/prd.md)** (F6) — depende de 8 (sem cards não há fila) e retroalimenta o resumo do dia em 3.

## Paralelismo possível

- 6 pode andar junto com 5.
- 7 pode andar em qualquer momento após 1, já que não tem dependências de conteúdo.
- O restante é sequencial por causa das dependências de dados: perfil → acervo → resolução → derivados.
