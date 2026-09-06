# Padrões de código — Certame

Regras derivadas do código que já existe no repositório. Ao criar algo novo, siga o que está aqui;
se precisar divergir, registre a decisão na `TECH-SPEC.md` na mesma alteração.
As regras inegociáveis do projeto estão em `AGENTS.md` e prevalecem sobre este documento.

## 1. Idioma

- **Código, pacotes, classes, métodos, variáveis, tabelas e colunas: português sem acento.**
  `RequisicaoCadastro`, `cadastrarEstudante`, `senha_hash`, `excluido_em`.
- **Mensagens ao usuário: português do Brasil com acentuação**, frase completa e com ponto final:
  `"Informe um e-mail válido."`, `"Este e-mail já está em uso."`.
- Exceção deliberada: os campos do envelope de erro (`code`, `message`, `fieldErrors`) e os nomes
  de propriedades técnicas do Spring permanecem em inglês.
- Comentário é exceção (§2.1). Quando for mesmo necessário, escreva em português, curto, explicando
  **por que** e citando o requisito:
  `/** Porta de escrita da entidade Usuario, incluindo o cadastro de RF-02. */`
- Commits em português sem acento, no imperativo: `adiciona limitador de tentativas de login`.

## 2. Regras gerais de codificação

Valem para os dois lados. Os exemplos aparecem em **Java** (backend) e **TypeScript/Angular**
(frontend) porque a mesma regra se escreve de formas diferentes em cada linguagem.

### 2.1 Não escreva comentários

O nome do método, da variável e do tipo é que deve explicar o código. Comentário só quando a
intenção é genuinamente impossível de ler no código — uma expressão regular densa, um algoritmo
externo, uma decisão contraintuitiva. Nunca comente o óbvio nem repita em prosa o que a linha diz.

```java
// Evite
// verifica se o usuario existe
if (usuarios.findByEmailAndExcluidoEmIsNull(email).isPresent()) {
    // lanca erro de email em uso
    throw new EmailEmUsoException();
}

// Prefira
if (usuarios.findByEmailAndExcluidoEmIsNull(email).isPresent()) {
    throw new EmailEmUsoException();
}

// Aceitável: a regra não se lê no código
/** Formato de RN-08: peso FSRS com 4 casas, aceita notação científica negativa. */
private static final Pattern PESO_FSRS = Pattern.compile("^-?\\d(\\.\\d{1,4})?([eE]-?\\d+)?$");
```

```typescript
// Evite
// soma os acertos da sessao
const total = respostas.filter((r) => r.correta).length;

// Prefira
const acertos = respostas.filter((resposta) => resposta.correta).length;
```

Quando o comentário for mesmo necessário, escreva em português, em uma linha, dizendo **por que** —
e cite o requisito quando houver (`RF-02`, `RN-08`). O Javadoc curto de classe de serviço, como o de
`UsuarioService`, continua permitido; comentário dentro do corpo do método, não.

### 2.2 Arquivos até 100 linhas, métodos até 30

Um arquivo com mais de 100 linhas quase sempre é mais de uma responsabilidade: extraia uma classe,
um serviço, um componente filho. Um método com mais de 30 linhas quase sempre é mais de um passo:
extraia métodos privados com nome de intenção.

```java
// Evite: um metodo que valida, monta, persiste e notifica
@Transactional
public Usuario cadastrarEstudante(RequisicaoCadastro requisicao) {
    // ... 60 linhas fazendo tudo
}

// Prefira: o metodo publico conta a historia, os privados executam
@Transactional
public Usuario cadastrarEstudante(RequisicaoCadastro requisicao) {
    String email = normalizarEmail(requisicao.email());
    garantirEmailInedito(email);
    return persistir(montarEstudante(requisicao, email));
}
private void garantirEmailInedito(String email) { ... }
private Usuario montarEstudante(RequisicaoCadastro requisicao, String email) { ... }
private Usuario persistir(Usuario usuario) { ... }
```

```typescript
// Evite: componente de 300 linhas com lista, filtro, paginacao e dialogo
// Prefira: FlashcardsPage (orquestra) + FlashcardsFiltro + FlashcardsLista + FlashcardDialog
@Component({
  selector: 'app-flashcards-lista',
  imports: [FlashcardCard],
  templateUrl: './flashcards-lista.html',
})
export class FlashcardsLista {
  readonly cards = input.required<Flashcard[]>();
  readonly selecionado = output<Flashcard>();
}
```

No frontend o limite vale por arquivo: template e estilo já vivem separados (`templateUrl`,
`styleUrl`), então um `.ts` acima de 100 linhas é sinal de componente fazendo demais — mova estado
compartilhado para um store em `core/` e divida a tela em componentes de `shared/ui`.

**É proibido comprimir várias instruções em uma linha física para "caber" no limite de 100
linhas** (`if (x) { a(); b(); }` numa linha, `try { ... } catch { ... }` colapsado, um método
inteiro numa única linha, `this.a = a; this.b = b;` no construtor). Isso cumpre a letra da regra e
contraria o objetivo dela — a compressão sintática não é responsabilidade única, é ofuscação. A
única exceção aceita é um getter/setter trivial de uma única instrução (`public UUID getId() {
return id; }`), pelo padrão já usado neste código. Divida sempre por **responsabilidade real**
(extraia uma classe colaboradora coesa), nunca por corte arbitrário de linhas.

### 2.3 No máximo 3 níveis de `if`/`else` — use cláusulas guarda

Trate o caso excepcional primeiro e saia cedo (*early return*). O caminho feliz fica no nível zero
de indentação, sem `else`.

```java
// Evite
public void redefinir(String token, String senha) {
    if (token != null) {
        var registro = tokens.findByToken(token);
        if (registro.isPresent()) {
            if (!registro.get().expirado()) {
                if (registro.get().getUsadoEm() == null) {
                    usuarioService.trocarSenha(registro.get().getUsuario(), senha);
                } else {
                    throw new TokenInvalidoException();
                }
            } else {
                throw new TokenInvalidoException();
            }
        } else {
            throw new TokenInvalidoException();
        }
    } else {
        throw new TokenInvalidoException();
    }
}

// Prefira
public void redefinir(String token, String senha) {
    if (token == null) {
        throw new TokenInvalidoException();
    }
    var registro = tokens.findByToken(token).orElseThrow(TokenInvalidoException::new);
    if (registro.expirado() || registro.getUsadoEm() != null) {
        throw new TokenInvalidoException();
    }
    usuarioService.trocarSenha(registro.getUsuario(), senha);
}
```

```typescript
// Evite
responder(alternativa: Alternativa | null): void {
  if (alternativa) {
    if (!this.sessao.finalizada()) {
      if (!this.sessao.jaRespondida()) {
        this.sessao.registrar(alternativa);
      }
    }
  }
}

// Prefira
responder(alternativa: Alternativa | null): void {
  if (!alternativa) return;
  if (this.sessao.finalizada()) return;
  if (this.sessao.jaRespondida()) return;
  this.sessao.registrar(alternativa);
}
```

No template, o mesmo princípio: `@if` / `@else` encadeado em três níveis vira componente filho ou
um `computed` que resolve o estado antes de renderizar.

### 2.4 No máximo 3 parâmetros — acima disso, objeto parâmetro

Muitos parâmetros escondem um conceito sem nome e viram erro de ordem na chamada. Agrupe em um
`record` (Java) ou `interface` (TypeScript) com nome de domínio.

```java
// Evite
public Page<Questao> buscar(UUID disciplinaId, UUID assuntoId, UUID bancaId,
                            Integer ano, String texto, Pageable pagina) { ... }

// Prefira
public record FiltroQuestoes(UUID disciplinaId, UUID assuntoId, UUID bancaId,
                             Integer ano, String texto) {
}

public Page<Questao> buscar(FiltroQuestoes filtro, Pageable pagina) { ... }
```

O projeto já segue isso: `ContextoRequisicao(ip, userAgent)` existe para não empurrar dois
parâmetros de infraestrutura por toda a cadeia de autenticação.

```typescript
// Evite
criarFlashcard(frente: string, verso: string, questaoId: string,
               disciplinaId: string, assuntoId: string, sessaoId: string) { ... }

// Prefira
export interface NovoFlashcard {
  frente: string;
  verso: string;
  origem: OrigemFlashcard;
}

criarFlashcard(novo: NovoFlashcard) { ... }
```

Em componente Angular, cada `input()` conta como entrada nomeada e não entra nesse limite — mas
mais de três inputs relacionados também pedem um objeto de contexto.

### 2.5 Sem linhas em branco dentro de métodos

Linha em branco separa **membros** (métodos, campos, funções de um arquivo), não trechos de um mesmo
método. Se você sentiu vontade de agrupar blocos com um respiro no meio da função, cada bloco é um
método com nome — extraia em vez de separar visualmente.

```java
// Evite
@Transactional
public Usuario cadastrarEstudante(RequisicaoCadastro requisicao) {
    String email = normalizarEmail(requisicao.email());

    if (usuarios.findByEmailAndExcluidoEmIsNull(email).isPresent()) {
        throw new EmailEmUsoException();
    }

    var usuario = new Usuario(requisicao.nome().trim(), email, ...);

    return usuarios.saveAndFlush(usuario);
}

// Prefira
@Transactional
public Usuario cadastrarEstudante(RequisicaoCadastro requisicao) {
    String email = normalizarEmail(requisicao.email());
    garantirEmailInedito(email);
    return persistir(montarEstudante(requisicao, email));
}

private void garantirEmailInedito(String email) {
    if (usuarios.findByEmailAndExcluidoEmIsNull(email).isPresent()) {
        throw new EmailEmUsoException();
    }
}
```

```typescript
// Evite
finalizar(): void {
  const respostas = this.sessao.respostas();

  const acertos = respostas.filter((resposta) => resposta.correta).length;

  this.api.enviarLote(respostas).subscribe(() => this.mostrarResultado(acertos));
}

// Prefira
finalizar(): void {
  const respostas = this.sessao.respostas();
  const acertos = respostas.filter((resposta) => resposta.correta).length;
  this.api.enviarLote(respostas).subscribe(() => this.mostrarResultado(acertos));
}
```

Entre métodos de uma classe, entre campos e métodos, e entre funções de um arquivo `.ts`, a linha em
branco continua sendo a separação correta.

### 2.6 Nada de número ou texto mágico

Todo literal com significado de negócio vira constante nomeada — o nome é o que explica o conceito
e evita o comentário da regra 2.1. Valor que muda por ambiente não é constante: é configuração.

```java
// Evite
if (tentativas >= 5 && minutos < 15) {
    throw new MuitasTentativasException(900);
}

// Prefira
private static final int MAX_TENTATIVAS = 5;
private static final Duration JANELA_TENTATIVAS = Duration.ofMinutes(15);

if (tentativas >= MAX_TENTATIVAS && decorrido.compareTo(JANELA_TENTATIVAS) < 0) {
    throw new MuitasTentativasException(JANELA_TENTATIVAS.toSeconds());
}
```

Caso real do repositório: o nome do cookie de refresh aparece literal como `"certame_rt"` nos
`@CookieValue` de `AutenticacaoController`, enquanto a montagem do cookie usa
`propriedades.getCookieNome()`. Ao mexer nesse arquivo, unifique na constante — duas fontes para o
mesmo nome é exatamente o defeito que esta regra evita.

```typescript
// Evite
if (resposta.status === 422) { ... }
this.http.get(`/api/v1/questoes?size=20`);

// Prefira
const HTTP_DADOS_INVALIDOS = 422;
const TAMANHO_PAGINA_PADRAO = 20;

if (resposta.status === HTTP_DADOS_INVALIDOS) { ... }
this.http.get(`${API_BASE}/questoes`, { params: { size: TAMANHO_PAGINA_PADRAO } });
```

Constante de módulo em `SCREAMING_SNAKE_CASE`; em Java, `private static final` no topo da classe.
Rótulo de interface em pt-BR fica no template, não em constante — ele é conteúdo, não conceito.

### 2.7 Declare a variável onde ela é usada

Nada de bloco de declarações no início do método. A variável nasce o mais perto possível do uso, com
o menor escopo possível, e sempre `final`/`const` quando não for reatribuída.

```java
// Evite
public ResultadoAutenticacao autenticar(RequisicaoLogin requisicao, ContextoRequisicao contexto) {
    String email;
    Usuario usuario;
    String accessToken;
    email = normalizarEmail(requisicao.email());
    ...
}

// Prefira
public ResultadoAutenticacao autenticar(RequisicaoLogin requisicao, ContextoRequisicao contexto) {
    var usuario = buscarAtivo(normalizarEmail(requisicao.email()));
    conferirSenha(usuario, requisicao.senha());
    var accessToken = tokenService.gerar(usuario);
    return new ResultadoAutenticacao(...);
}
```

```typescript
// Evite
let acertos: number;
let percentual: number;
const respostas = this.sessao.respostas();
acertos = respostas.filter((resposta) => resposta.correta).length;
percentual = (acertos / respostas.length) * 100;

// Prefira
const respostas = this.sessao.respostas();
const acertos = respostas.filter((resposta) => resposta.correta).length;
const percentual = (acertos / respostas.length) * 100;
```

Use `const` por padrão e `let` só quando houver reatribuição real; `var` nunca. Em Java, `var` para
inferência local é bem-vindo quando o tipo já está evidente no lado direito.

### 2.8 Segredo nunca no código

Chave, senha, token, string de conexão e URL de ambiente saem por variável de ambiente, lidas em
`application-*.properties` (backend) ou injetadas na configuração de build (frontend). Nada disso
entra em commit, em teste ou em arquivo versionado.

```java
// Evite
private static final String JWT_SEGREDO = "certame-2026-chave-super-secreta";

// Prefira: application-prod.properties
// certame.seguranca.jwt-segredo=${JWT_SECRET}
@ConfigurationProperties(prefix = "certame.seguranca")
public class PropriedadesSeguranca {
    private String jwtSegredo;
}
```

```typescript
// Evite
const API_BASE = 'https://api.certame.com.br/api/v1';
const TOKEN_ANALYTICS = 'sk_live_9f2c...';

// Prefira
import { environment } from '../../environments/environment';
const API_BASE = environment.apiBase;
```

O único segredo literal tolerado no repositório é o `JWT_SECRET` de desenvolvimento em
`application-dev.properties`, marcado com o comentário que diz que não vale para outro ambiente —
`staging` e `prod` exigem a variável de ambiente. Se um segredo real for commitado por engano, ele
é considerado vazado: rotacione a chave, não basta remover o commit.

## 3. Backend (Java 25 / Spring Boot 4.1)

### Organização

- Pacote por domínio sob `br.com.certame` (`auth/`, `usuario/`, `catalogo/`, `questao/`, ...),
  nunca por camada técnica. `comum/` guarda o que é transversal; `config/` guarda configuração.
- DTOs em `<dominio>/dto/`. Um domínio não acessa o repositório de outro: converse por serviço.

### Camadas

- **Controller** só orquestra: valida (`@Valid`), chama o serviço, monta a `ResponseEntity`.
  Sem regra de negócio, sem acesso a repositório, sem `@Transactional`.
- **Service** concentra a regra, é anotado com `@Transactional` no que escreve e é onde vive a
  autorização de recurso (`@PreAuthorize`). Retorna entidades ou records próprios do serviço
  (ex.: `AutenticacaoService.ResultadoAutenticacao`), nunca `ResponseEntity`.
- **Repository** é Spring Data; JPQL ou nativo só quando o método derivado não resolve.

### Injeção e nulidade

- **Injeção por construtor com campos `private final`.** Nada de `@Autowired` em campo no código
  de produção (`@Autowired` em campo é aceito apenas nos testes de integração).
- **Não use Lombok nas entidades e serviços.** A dependência existe no `pom.xml`, mas o código de
  produção escreve construtores, getters e setters à mão (ver `usuario/Usuario.java`) — mantenha
  assim para que o construtor obrigatório e os setters expostos sejam decisões explícitas.

### DTOs e validação

- DTO é sempre `record`, imutável, com as anotações de Bean Validation e **a mensagem em português
  em cada constraint** — a mensagem padrão do framework nunca chega ao usuário:

  ```java
  public record RequisicaoCadastro(
          @NotBlank(message = "Informe seu nome.")
          @Size(min = 3, max = 120, message = "O nome deve ter entre 3 e 120 caracteres.") String nome,
          ...) {
  }
  ```

- Nomenclatura: `Requisicao*` para entrada, `Resposta*` para saída, `*Resumo` para projeção enxuta.
- Entidade JPA **nunca** cruza a fronteira do controller.

### Entidades JPA

- `@Entity @Table(name = "usuario")` — nome de tabela sempre explícito, no singular e `snake_case`.
- `UUID` com `@GeneratedValue(strategy = GenerationType.UUID)`; datas em `Instant` (UTC).
- Construtor protegido sem argumentos para o JPA + construtor público com os campos obrigatórios.
- Setters apenas para o que realmente muda depois de criado (`setSenhaHash`, `setStatus`,
  `setExcluidoEm`) — o resto é imutável.
- `criado_em` / `atualizado_em` mantidos por `@PrePersist` / `@PreUpdate`.
- Exclusão é lógica: preencha `excluidoEm` e filtre com `...AndExcluidoEmIsNull` nas consultas.
- Enum persistido como texto: `@Enumerated(EnumType.STRING)`, com `check` equivalente na migração.

### Erros

- Toda falha de negócio é uma subclasse de `ErroNegocioException`, que carrega `code`, `status`
  HTTP e mensagem; `getFieldErrors()` é sobrescrito quando o erro aponta para um campo do formulário.
- Um único `@RestControllerAdvice` (`ManipuladorErrosGlobal`) converte tudo para
  `application/problem+json` com o record `ErroApi(code, message, fieldErrors)`.
  Não crie `try/catch` no controller nem outro advice.
- `code` é uma constante em `SNAKE_CASE` maiúsculo e estável (`EMAIL_EM_USO`, `DADOS_INVALIDOS`) —
  o frontend depende dela; a `message` pode mudar, o `code` não.
- Erro de validação responde **422** com `fieldErrors` campo a campo.

### Segurança

- Autorização **sempre** no servidor com `@PreAuthorize`, mesmo quando a interface já esconde a ação.
- Nunca revele existência de conta: fluxos de e-mail respondem igual para conhecido e desconhecido.
- Segredos só por variável de ambiente; nada de credencial literal fora de `application-dev.properties`.

### Migrações Flyway

- SQL em minúsculas, `snake_case`, uma migração por alteração: `V<n>__descricao_sem_acento.sql`.
- `db/migration/` é imutável e vale para todos os ambientes; `db/migration-dev/` (série `V900+`)
  guarda seeds que só o perfil `dev` carrega.
- PK `uuid primary key default gen_random_uuid()`, datas `timestamptz not null default now()`.
- Enum do domínio vira `text` + `check (... in (...))`, espelhando o enum Java.
- Índice único parcial para exclusão lógica: `create unique index ... where excluido_em is null`.
- Hibernate está em `ddl-auto=validate`: se a entidade e a migração divergirem, a aplicação não sobe.

### Testes (JUnit · AssertJ · Mockito · Testcontainers) — regras completas em `.agents/rules/tests.md`

- Nome do método começa com o **id do caso de teste da spec** e descreve o comportamento em
  português: `TU05_cadastraEstudanteComEmailInedito`, `TI04_cadastraUsuarioEAbreSessao`.
- Classe: `*Test` para unidade (sem contexto Spring, dependências mockadas à mão),
  `*IntegracaoTest` para `@SpringBootTest @AutoConfigureMockMvc @Testcontainers`,
  `*ControllerTest` para o slice web.
- Asserções com AssertJ (`assertThat`, `assertThatThrownBy`), nunca `assertEquals`.
- Integração sobe `PostgreSQLContainer<>("postgres:18")` e injeta o datasource por
  `@DynamicPropertySource` — não dependa do `docker-compose`.
- Dados de teste isolados por execução (ex.: e-mail com `UUID.randomUUID()`).
- Em teste, `BCryptPasswordEncoder(4)` para não pagar o custo de força 12.
- Cobertura mínima de 80% (ver `.agents/rules/tests.md`, que prevalece sobre esta seção).

## 4. Frontend (Angular 22)

### Organização

- `core/` (auth, api, models) · `shared/` (ui, pipes, directives) · `layout/` · `features/<dominio>/`.
- Arquivos em `kebab-case` sem sufixo redundante no padrão do Angular 22 (`app.ts`, `app.html`,
  `app.scss`); classes em `PascalCase`; seletores com prefixo `app-`.

### Componentes

- **Standalone sempre** (sem `NgModule`), com `imports: [...]` no próprio decorator.
- **Zoneless:** todo estado é `signal` / `computed`; nada de `setTimeout` para forçar render.
  Membros de template expostos como `protected readonly`.
- `input()` / `output()` como funções — não use os decorators `@Input` / `@Output`.
- Template e estilo em arquivos separados (`templateUrl`, `styleUrl`); estilo inline só em
  componente muito pequeno.
- Estado compartilhado em serviço `providedIn: 'root'` com signals (`AuthStore`,
  `StudySessionStore`, `ToastStore`), não em componente pai.

### Estilo

- Somente tokens do design system Nocturne: `var(--*)` de `src/styles.css`.
  **Nenhum valor cru** de cor, fonte, espaçamento, raio ou sombra. Sem biblioteca de UI de terceiros.
- A marcação replica o protótipo aprovado (`protótipo/Certame.dc.html`), com as mesmas classes.

### HTTP e erros

- Chamadas concentradas em clientes de `core/api`; componente não injeta `HttpClient` direto.
- O interceptor anexa o access token, renova uma única vez em 401 e redireciona ao login na falha.
- Resposta 422 é mapeada campo a campo (`fieldErrors`) para os `FormControl` correspondentes;
  os demais erros viram toast com a `message` vinda da API.

### Formatação e testes

- Prettier com `printWidth: 100` e aspas simples; `.editorconfig` manda em indentação (2 espaços),
  UTF-8, quebra final de linha e remoção de espaço à direita.
- `npm run format:check` (`prettier --check .`) é obrigatório e deve retornar 0 arquivos fora do
  padrão **antes de qualquer merge** — gate automatizado em `.github/workflows/frontend-format.yml`.
  Rode `npx prettier --write .` para corrigir localmente antes de abrir o PR.
- Testes com Vitest (`*.spec.ts`, `npm test`); E2E com Playwright.

## 5. Checklist antes de entregar

- [ ] A tarefa cita o requisito que atende (RF-xx / RN-xx) e a implementação bate com ele.
- [ ] Domínio e testes antes do controller; controller antes do frontend.
- [ ] Nenhum endpoint fora do contrato da Tech Spec §6 — ou o documento foi atualizado junto.
- [ ] Migração nova é aditiva e nenhuma migração já aplicada foi editada.
- [ ] Autorização validada no servidor, não só escondida na interface.
- [ ] Mensagens ao usuário em pt-BR; identificadores sem acento.
- [ ] Arquivos ≤ 100 linhas, métodos ≤ 30, até 3 parâmetros e até 3 níveis de condicional.
- [ ] Sem comentário desnecessário, sem linha em branco dentro de método, sem literal mágico.
- [ ] Nenhum segredo no código; configuração por variável de ambiente.
- [ ] `./mvnw test` e `npm test` passam.
- [ ] `npm run format:check` passa (0 arquivos fora do padrão Prettier).
