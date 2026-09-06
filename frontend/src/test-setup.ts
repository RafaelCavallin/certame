/**
 * Fixa o fuso horário da execução dos testes.
 *
 * Vários testes de data comparam a conversão entre o UTC vindo da API e o
 * horário local exibido nos inputs `datetime-local`. Sem um fuso fixo, o
 * resultado depende da máquina: passa em America/Sao_Paulo e falha em UTC,
 * como nos runners do GitHub Actions. America/Sao_Paulo é o fuso do domínio,
 * o mesmo declarado em `certame.estudo.zona` no backend.
 *
 * O `process` é declarado localmente porque o projeto não depende de
 * @types/node e o tsconfig dos testes expõe apenas `vitest/globals`.
 */
declare const process: { env: Record<string, string | undefined> };

process.env['TZ'] = 'America/Sao_Paulo';
