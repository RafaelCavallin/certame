# Regras de testes

Leitura obrigatória antes de escrever qualquer teste. Complementa
`.agents/rules/code-standards.md` §3 (backend) e §4 (frontend); em caso de divergência sobre
cobertura, **este documento prevalece**.

## 1. Regras inegociáveis

1. **Todo código deve ser coberto por testes automatizados.** Regra crítica — não existe entrega de
   funcionalidade sem teste. Código novo sem teste é código incompleto, mesmo que "funcione na tela".
2. **Cobertura mínima de 80%** por módulo (`service`/`domínio` no backend, `core`/`features` no
   frontend). Cobertura é piso, não meta: o esforço vai para o que é **crítico**.
3. **Priorize pelo risco.** Correção de simulado, cálculo FSRS (RN-08), imutabilidade de `resposta`
   (RN-02), autorização por perfil e login/refresh são muito mais críticos que um CRUD de matéria.
   Fluxo crítico merece testes de caminho feliz **e** de todas as bordas; CRUD simples merece o
   essencial.
4. **Nada de teste decorativo.** Um teste que passa mesmo com a regra de negócio quebrada é pior que
   nenhum teste — dá falsa segurança.

### O que conta como crítico neste projeto

| Prioridade | Áreas |
| --- | --- |
| Alta | `auth` (login, refresh, redefinição), autorização `@PreAuthorize`, correção de simulado, `flashcard/fsrs`, imutabilidade de `resposta`, fila de revisão e limites do deck (RN-10/RN-11) |
| Média | sessão de estudo, cadernos, estatísticas de desempenho, filtros do catálogo |
| Baixa | cadastros administrativos simples, mapeamentos DTO triviais |

## 2. FIRST

### F — Fast

Teste lento não roda, e teste que não roda não protege ninguém. A suíte de unidade tem que rodar em
segundos.

- Sem rede, sem disco, sem banco, sem `Thread.sleep` na camada de unidade — use **stub/fake**.
- `BCryptPasswordEncoder(4)` em teste, nunca a força 12 de produção.
- Testcontainers **só** em teste de integração (`*IntegracaoTest`), nunca para validar uma regra que
  cabe em um teste de unidade.

```java
// Evite — sobe o contexto Spring inteiro só para testar um if
@SpringBootTest
class LimitadorTentativasTest { ... }

// Prefira — objeto puro, milissegundos
class LimitadorTentativasTest {
    @Test void TU21_bloqueiaAposCincoTentativas() {
        var limitador = new LimitadorTentativas(new PropriedadesSeguranca(), Clock.systemUTC());
        ...
    }
}
```

### I — Independent

Nenhum teste pode depender da execução, da ordem ou do estado deixado por outro. Se dependem, um
defeito único derruba uma cascata de testes e some com o sinal do que realmente quebrou.

- **Zero estado estático compartilhado** entre testes (`static List<...>` acumulando, contador de
  classe, cache global). Se existir, limpe em `@BeforeEach`.
- Cada teste monta seus próprios dados (`Arrange` completo), mesmo que isso repita três linhas.
- Dados isolados por execução: e-mail com `UUID.randomUUID()`, nunca `"teste@certame.com"` fixo em
  teste de integração.
- Não use `@TestMethodOrder` para "fazer funcionar" — se precisou ordenar, o teste está acoplado.

```java
// Evite — TU02 só passa porque TU01 rodou antes
class UsuarioServiceTest {
    static Usuario usuarioCriado;
    @Test void TU01_cadastra() { usuarioCriado = service.cadastrar(...); }
    @Test void TU02_atualiza() { service.atualizar(usuarioCriado.getId(), ...); }
}

// Prefira — cada teste é autossuficiente
class UsuarioServiceTest {
    @Test void TU02_atualizaNomeDoUsuarioExistente() {
        var usuario = new Usuario("Pessoa", "p@e.com", "hash", PerfilUsuario.ESTUDANTE, StatusUsuario.ATIVO);
        var repo = mock(UsuarioRepository.class);
        when(repo.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        ...
    }
}
```

### R — Repeatable

Rodou dez vezes, deu o mesmo resultado — na sua máquina, na do colega e no CI.

Fontes de instabilidade e como matar cada uma:

| Fonte | Solução |
| --- | --- |
| `Instant.now()` / `LocalDate.now()` | injete `Clock` e use `Clock.fixed(...)` no teste |
| API externa / e-mail | mock da interface (`EnviadorEmail`), nunca SMTP real |
| `UUID.randomUUID()` numa asserção | fixe o valor esperado ou asserte a forma, não o valor |
| aleatoriedade (embaralhar questões) | injete `Random` com semente fixa |
| ordem de coleção do banco | `ORDER BY` explícito e `containsExactlyInAnyOrder` |
| `setTimeout` / esperas fixas no front | `vi.useFakeTimers()` ou espera por condição |

```java
// Evite — quebra na virada do dia e no fuso do CI
assertThat(token.getExpiraEm()).isEqualTo(Instant.now().plusSeconds(900));

// Prefira — tempo controlado
var agora = Instant.parse("2026-01-01T00:00:00Z");
var service = new RefreshTokenService(repo, propriedades, Clock.fixed(agora, ZoneOffset.UTC));
assertThat(service.emitir(usuario).getExpiraEm()).isEqualTo(agora.plusSeconds(900));
```

```typescript
// Evite
expect(component.saudacao()).toBe('Bom dia');   // depende da hora de quem roda

// Prefira
vi.setSystemTime(new Date('2026-01-01T09:00:00Z'));
expect(component.saudacao()).toBe('Bom dia');
```

### S — Self-validating

O teste decide sozinho se passou ou falhou, e **falha de verdade quando o comportamento quebra**.
Cobertura sem asserção é linha executada, não comportamento verificado.

- Toda asserção com AssertJ no backend (`assertThat`, `assertThatThrownBy`) e `expect` no frontend.
  Nunca `assertEquals`, nunca `console.log` como verificação.
- Asserte o **resultado e o efeito colateral relevante**, não apenas "não lançou exceção".
- Ao testar exceção, asserte o tipo **e** a mensagem/estado resultante.
- Regra prática: mude o sinal de uma comparação na implementação — se nenhum teste ficar vermelho, o
  teste está fraco.

```java
// Evite — cobre a linha, não valida nada
@Test void TU07_corrigeSimulado() {
    service.corrigir(simuladoId);
}

// Prefira — nota, contagem e persistência verificadas
@Test void TU07_corrigeSimuladoContabilizandoAcertosEErros() {
    var resultado = service.corrigir(simuladoId);
    assertThat(resultado.acertos()).isEqualTo(3);
    assertThat(resultado.erros()).isEqualTo(2);
    assertThat(resultado.percentual()).isEqualTo(60.0);
    verify(repo).save(argThat(s -> s.getStatus() == StatusSimulado.FINALIZADO));
}
```

```typescript
// Evite
it('carrega decks', async () => {
  await component.carregar();
  expect(component.decks()).toBeTruthy();
});

// Prefira
it('preenche a lista de decks e encerra o carregando', async () => {
  await component.carregar();
  expect(component.decks()).toHaveLength(2);
  expect(component.decks()[0].nome).toBe('Direito Constitucional');
  expect(component.carregando()).toBe(false);
});
```

### T — Timely

**Ignorado neste projeto.** Não exigimos escrever o teste antes do código; exigimos que ele exista
no mesmo commit da funcionalidade.

## 3. Estrutura de cada teste: AAA / Given-When-Then

Todo teste tem três blocos visíveis e nessa ordem. Uma linha em branco separa os blocos (é a única
exceção à regra de "sem linha em branco dentro de método" do `code-standards.md`).

```java
@Test void TU05_cadastraEstudanteComEmailInedito() {
    // Arrange (Given)
    var repo = mock(UsuarioRepository.class);
    when(repo.existsByEmail("novo@certame.com")).thenReturn(false);
    var service = new AutenticacaoCadastroService(repo, new BCryptPasswordEncoder(4), enviadorEmail);

    // Act (When)
    var criado = service.cadastrar(new RequisicaoCadastro("Pessoa", "novo@certame.com", "Senha@123"));

    // Assert (Then)
    assertThat(criado.perfil()).isEqualTo(PerfilUsuario.ESTUDANTE);
    assertThat(criado.status()).isEqualTo(StatusUsuario.PENDENTE);
    verify(enviadorEmail).enviarConfirmacao(eq("novo@certame.com"), anyString());
}
```

```typescript
it('desabilita o envio enquanto a senha for inválida', () => {
  // Arrange
  const fixture = TestBed.createComponent(LoginPage);
  const component = fixture.componentInstance;

  // Act
  component.form.setValue({ email: 'a@b.com', senha: '123' });
  fixture.detectChanges();

  // Assert
  expect(component.podeEnviar()).toBe(false);
});
```

Regras de nome:

- Backend: `TU`/`TI` + id do caso da spec + comportamento em português sem acento —
  `TU14_consumeTokenValidoMarcaComoUsado`, `TI04_cadastraUsuarioEAbreSessao`.
- Frontend: `describe` com o nome da unidade; `it` descreve o comportamento em português —
  `it('bloqueia o envio quando o e-mail é inválido')`.
- Um comportamento por teste. Se precisar de "e" no meio do nome duas vezes, são dois testes.

## 4. Pirâmide de testes

```
        /\        E2E (poucos)      — Playwright, fluxos críticos ponta a ponta
       /  \       Integração        — Testcontainers / slices Spring / TestBed com HTTP mockado
      /____\      Unidade (base)    — JUnit + Mockito / Vitest puro
```

Proporção alvo: **~70% unidade · ~20% integração · ~10% E2E**.

| Camada | Backend | Frontend | Quando usar |
| --- | --- | --- | --- |
| Unidade | `*Test` — sem contexto Spring, dependências mockadas à mão | Vitest puro sobre service/signal/pipe | regra de negócio, cálculo, validação, estado |
| Integração | `*IntegracaoTest` (`@SpringBootTest @AutoConfigureMockMvc @Testcontainers`) e `*ControllerTest` (slice web) | `TestBed` + `provideHttpClientTesting` | contrato HTTP, segurança, SQL/JPA, migração Flyway |
| E2E | — | Playwright | login → estudar → responder; simulado ponta a ponta; revisão de flashcards |

Subir um nível da pirâmide custa tempo de execução. Só suba quando o nível de baixo **não consegue**
provar o que você precisa provar (ex.: `@PreAuthorize` só é provado de verdade no slice web).

## 5. Ordem de escrita (eficiência)

Escreva nesta ordem — cada passo aproveita o contexto já carregado do anterior:

1. **Domínio/serviço (unidade)** — regras de negócio e bordas. É onde mora o valor.
2. **Repositório/persistência**, se houver query própria ou constraint relevante.
3. **Controller (slice web)** — status HTTP, formato do JSON, autorização por perfil.
4. **Integração ponta a ponta do backend** — só para o fluxo crítico da funcionalidade.
5. **Frontend unitário** — service de API, store de signals, pipes, lógica do componente.
6. **E2E (Playwright)** — apenas o caminho crítico da funcionalidade, um cenário por fluxo.

Dentro de cada passo: caminho feliz primeiro, depois bordas (nulo, vazio, limite, duplicado), depois
falhas (não encontrado, sem permissão, conflito).

## 6. Dublês de teste

| Dublê | Use para | Exemplo no projeto |
| --- | --- | --- |
| **Stub** | devolver dado pronto e evitar dependência lenta | `when(repo.findByEmail(...)).thenReturn(Optional.of(usuario))` |
| **Mock** | verificar interação e neutralizar não-determinismo | `verify(enviadorEmail).enviarConfirmacao(...)` |
| **Fake** | implementação simples em memória para reuso | `EnviadorEmail` que guarda mensagens numa lista |
| **Spy** | preservar comportamento real e observar uma chamada | use com parcimônia; sinal de acoplamento |

- Mocke **a fronteira**, não o objeto sob teste. Se você precisa mockar três dependências internas
  para o teste passar, a classe tem responsabilidades demais.
- Não mocke tipos que você não possui (`Instant`, coleções da JDK) — injete `Clock`/dados.

```typescript
// Frontend: HTTP sempre por HttpTestingController, nunca fetch real
TestBed.configureTestingModule({
  providers: [provideHttpClient(), provideHttpClientTesting()],
});
const http = TestBed.inject(HttpTestingController);
service.listarDecks().subscribe(...);
http.expectOne('/api/v1/flashcards/decks').flush([{ id: '1', nome: 'Direito Constitucional' }]);
http.verify();
```

## 7. Ferramentas e comandos

**Backend** — JUnit 6 (Jupiter, via Spring Boot 4.1) · AssertJ · Mockito · Testcontainers 2.0.5
(PostgreSQL 18):

```bash
cd backend && ./mvnw test                          # suíte + relatório JaCoCo em target/site/jacoco/
./mvnw test -Dtest=AutenticacaoServiceTest        # uma classe
./mvnw verify                                      # aplica a regra de 80% e falha se não atingir
```

A cobertura é medida pelo **JaCoCo**: `prepare-agent` + `report` na fase `test`, e `check` na fase
`verify` com limite de **80% de linhas por classe `*Service`**. Ficam fora da medição
`CertameApplication`, `ServletInitializer`, `config/**` e os `dto/**` (records sem lógica).

**Frontend** — Vitest (jsdom) pelo builder `@angular/build:unit-test`; E2E com Playwright:

```bash
cd frontend && npm test                            # Vitest (*.spec.ts), sem cobertura — rápido
npm run test:coverage                              # cobertura v8; falha abaixo de 80%
npx playwright test                                # E2E (após a instalação prevista no PRD de acesso e conta)
```

A cobertura usa `@vitest/coverage-v8` pelo builder `@angular/build:unit-test`, com limite de **80%**
em statements, branches, functions e lines (`angular.json` → `test.options.coverageThresholds`).
Relatório HTML em `frontend/coverage/`. Ficam fora `main.ts`, `*.config.ts`, `*.routes.ts` e os
próprios `*.spec.ts`.

Arquivos: `*.spec.ts` ao lado do código no frontend; `src/test/java/...` espelhando o pacote de
produção no backend. E2E em `frontend/e2e/*.spec.ts`.

## 8. Checklist antes de abrir o PR

- [ ] Todo código novo tem teste; nenhuma classe/serviço novo entrou sem `*Test`.
- [ ] Cobertura ≥ 80% no módulo tocado, com o fluxo crítico coberto em caminho feliz **e** bordas.
- [ ] Cada teste é Fast (sem I/O na unidade), Independent (sem estado compartilhado), Repeatable
      (`Clock` fixo, sem rede) e Self-validating (asserções sobre valor e comportamento).
- [ ] Estrutura AAA/Given-When-Then visível em todos os testes.
- [ ] Nomes seguem o padrão (`TU`/`TI` + id + comportamento; `it('...')` descritivo).
- [ ] A pirâmide foi respeitada: nada foi para integração/E2E que caberia em unidade.
- [ ] `./mvnw verify` e `npm run test:coverage` passam localmente (ambos aplicam o piso de 80%).
