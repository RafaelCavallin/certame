# Acesso no ambiente de desenvolvimento

## Usuários

Os usuários abaixo são criados pela migração `V900__seed_acesso_dev.sql` quando a aplicação é executada com o perfil Spring `dev`.

| Perfil | E-mail | Senha |
| --- | --- | --- |
| Professor | `professor@dev.certame.local` | `senhaDev123` |
| Administrador | `admin@dev.certame.local` | `senhaDev123` |
| Super administrador | `superadmin@dev.certame.local` | `senhaDev123` |

Esses dados são exclusivos do ambiente de desenvolvimento e não são carregados em `staging` ou `prod`.

## Executar a aplicação

### Infraestrutura

Na raiz do projeto:

```bash
docker compose up -d
```

Serviços iniciados:

- PostgreSQL: `localhost:55432`
- MailHog SMTP: `localhost:55525`
- MailHog UI: <http://localhost:58025>

Para parar os serviços:

```bash
docker compose down
```

### Backend

Em outro terminal:

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

API: <http://localhost:8080>

### Frontend

Em outro terminal:

```bash
cd frontend
npm ci
npm start
```

Aplicação: <http://localhost:4200>
