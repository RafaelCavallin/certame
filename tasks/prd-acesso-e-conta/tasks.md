# Resumo das tarefas de implementação de Acesso e conta

## Tarefas

- [x] 1.0 Fundação do projeto (pacote, dependências e perfis) — pacote `br.com.certame`, dependências de autenticação/testes, perfis datasource/Flyway e Compose com PostgreSQL 18 e MailHog.
- [x] 2.0 Migração e persistência (Flyway, entidades e repositórios) — API Docker/Testcontainers atualizada e TI-14/TI-15 aprovados.
- [x] 3.0 Fundação transversal (erros, segurança e CORS) — envelope problem+json, exceções de negócio, JWT stateless HS256, BCrypt 12, CORS configurável e propriedades de segurança.
- [x] 4.0 Login, refresh e logout (RF-01, RF-05) — autenticação JWT, refresh rotativo em cookie, logout idempotente, limitador e observabilidade.
- [x] 5.0 Cadastro de estudante (RF-02) — `UsuarioService`, cadastro de estudante autenticado, tratamento de e-mail duplicado, limitador e métricas implementados com TU-05 a TU-07 e TI-04/TI-05 aprovados.
- [x] 6.0 Redefinição de senha (RF-03, RF-04) — recuperação por token de uso único, SMTP assíncrono, ativação de convites, expurgo, limitador, métricas e testes TU-08 a TU-13/TI-06 a TI-10 implementados.
- [x] 7.0 Núcleo do frontend (AuthStore, interceptor e guardas) — AuthStore em signals, cliente das seis rotas, refresh único em 401, guardas, rotas públicas e restauração de sessão no bootstrap implementados com TU-18 a TU-22.
- [x] 8.0 Fundação visual (design system Nocturne e aprovação do protótipo) — tokens Nocturne, Inter e Phosphor locais, assets Angular e Playwright configurados.
- [x] 9.0 Telas de acesso (tradução 1:1 do protótipo) — páginas de acesso, recuperação e redefinição, shell protegido e logout implementados; 30 testes e cobertura acima de 80% aprovados.
- [x] 10.0 E2E e endurecimento — Playwright contra SPA/API reais, PostgreSQL/MailHog isolados, auditorias de privacidade/observabilidade, cobertura e validação do WAR executável concluídas.
