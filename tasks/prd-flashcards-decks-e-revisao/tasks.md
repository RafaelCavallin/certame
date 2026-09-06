# Resumo das tarefas de implementação de Flashcards — decks, limites e revisão espaçada (FSRS)

## Tarefas

- [x] 1.0 Design — fechar as lacunas do protótipo (aviso de fila acumulada, "Sem deck", rótulo em minutos)
- [x] 2.0 Migração e persistência (`V14__flashcards_revisao.sql`, `SessaoRevisao`, `FlashcardRevisao`, colunas novas em `Flashcard`)
- [x] 3.0 Agendador FSRS (`flashcard/fsrs`: pesos, curva de esquecimento, dificuldade, estabilidade, passos, `AgendadorFsrs`)
- [x] 4.0 Relógio de estudo, limites de deck e CRUD completo de deck
- [x] 5.0 Montagem da fila diária (`MontadorFila`, `ConsumoDiario`)
- [x] 6.0 Serviços de sessão e revisão (`SessaoRevisaoService`, `RegistroRevisaoService`, `ResumoRevisaoService`, `TempoEstudoFlashcards`)
- [x] 7.0 Controllers e contrato da API (`RevisaoController`, `DeckController` `PUT`/`DELETE`)
- [x] 8.0 Frontend — cliente HTTP, modelos e tela de Decks
- [x] 9.0 Frontend — sessão de revisão de flashcards
- [x] 10.0 Testes E2E, endurecimento e atualização da documentação
