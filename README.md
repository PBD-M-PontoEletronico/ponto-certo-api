# PontoCerto API — Backend

API REST (Spring Boot + PostgreSQL) do sistema de ponto, com autenticação JWT
e isolamento de dados por empresa (multi-tenant).

## Stack

- **Java 21** + **Spring Boot** (Web, Data JPA, Security, Validation)
- **PostgreSQL** (via Docker)
- **JWT** (biblioteca `jjwt`) para autenticação
- **Lombok** para reduzir código repetitivo

## Pré-requisitos

| Ferramenta | Como confirmar |
|---|---|
| **Docker Desktop** | `docker -v` |
| **JDK 21** | `java -version` |
| **Maven** (geralmente já vem com a IDE) | `mvn -v` |
| **IntelliJ IDEA** (ou outra IDE Java) | — |

## 1. Suba o banco de dados

Na raiz do projeto (onde está o `docker-compose.yml`):

```bash
docker compose up -d
```

Confirme que subiu:
```bash
docker ps
```
Deve aparecer `postgres-pontocerto` e `pgadmin-pontocerto` com status `Up`.

**pgAdmin** (opcional, para inspecionar o banco visualmente): `http://localhost:5050`
- Login: `admin@admin.com` / `admin123`
- Ao registrar o servidor: Host = `postgres`, Port = `5432`, Database = `pontocerto_db`, Username = `postgres`, Password = `senha123`

## 2. Configure o `application.yaml`

Deve existir em `src/main/resources/application.yaml`:

```yaml
spring:
  application:
    name: pontocerto

  datasource:
    url: jdbc:postgresql://localhost:5432/pontocerto_db
    username: postgres
    password: senha123
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true
    database-platform: org.hibernate.dialect.PostgreSQLDialect

jwt:
  secret: "minha-chave-secreta-bem-longa-e-dificil-de-adivinhar-troque-isso-depois"
  expiration: 86400000

server:
  port: 8080

logging:
  level:
    org.hibernate.SQL: debug
```

⚠️ Se existir também um `application.properties` na mesma pasta, **delete-o** — ter os dois ao mesmo tempo faz o Spring ignorar o `.yaml`.

⚠️ Em produção, troque `jwt.secret` por um valor gerado aleatoriamente e mantido fora do controle de versão (variável de ambiente).

## 3. Rode a aplicação

**Pela IDE**: abra a classe principal (`@SpringBootApplication`, ex: `PontocertoApplication.java`) e clique em **Run** (▶️).

**Pelo terminal**, na raiz do projeto:
```bash
mvn spring-boot:run
```

Confirme no console:
```
Started PontocertoApplication in X seconds
```
Sem `ERROR` em vermelho acima.

**Se der erro "Port 8080 was already in use"**: já existe uma instância rodando em segundo plano. Pare todas as execuções ativas na aba "Run"/"Services" da IDE antes de rodar de novo.

## 4. Crie o primeiro usuário (SUPERADMIN)

Não há cadastro público — o primeiro usuário precisa ser inserido direto no banco.

**4.1. Gere um hash de senha.** Crie esta classe temporária em qualquer pacote do projeto, rode com botão direito → Run, depois pode apagar:

```java
package com.mobdata.pontocerto;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GerarHash {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        System.out.println(encoder.encode("SUA_SENHA_AQUI"));
    }
}
```

**4.2. Rode este SQL** no pgAdmin (Query Tool, dentro do banco `pontocerto_db`), colando o hash copiado:

```sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO tb_usuario (id, nome, usuario, senha_hash, perfil, empresa_id)
VALUES (
    gen_random_uuid(),
    'Administrador Master',
    'superadmin',
    'COLE_O_HASH_AQUI',
    'SUPERADMIN',
    NULL
);
```

**4.3. Teste o login** no Postman:
```
POST http://localhost:8080/auth/login
Content-Type: application/json

{
  "usuario": "superadmin",
  "senha": "SUA_SENHA_AQUI"
}
```
Deve retornar `200 OK` com um token JWT, o nome, o perfil e `empresaId: null`.

## Endpoints principais

| Método | Rota | Perfil exigido | Descrição |
|---|---|---|---|
| `POST` | `/auth/login` | Público | Autentica e devolve o token |
| `GET` | `/empresas` | `SUPERADMIN` | Lista empresas |
| `POST` | `/empresas` | `SUPERADMIN` | Cadastra empresa |
| `PATCH` | `/empresas/{id}/situacao?ativa=bool` | `SUPERADMIN` | Ativa/desativa empresa |
| `POST` | `/usuarios` | `SUPERADMIN`, `RH_ADMIN` | Cadastra usuário (RH_ADMIN só na própria empresa) |
| `POST` | `/setores` | `SUPERADMIN`, `RH_ADMIN` | Cadastra setor |
| `GET` | `/setores` | `SUPERADMIN`, `RH_ADMIN` | Lista setores da empresa |
| `GET` | `/setores/{id}` | `SUPERADMIN`, `RH_ADMIN` | Busca setor por id |
| `DELETE` | `/setores/{id}` | `SUPERADMIN`, `RH_ADMIN` | Exclui setor |

Toda rota autenticada exige o header:
```
Authorization: Bearer <token>
```

## Estrutura de pacotes

```
com.mobdata.pontocerto
├── config/       # SecurityConfig, JwtAuthenticationFilter
├── controller/   # Endpoints REST
├── service/      # Regras de negócio
├── repository/   # Acesso ao banco (Spring Data JPA)
├── model/        # Entidades (@Entity)
├── dto/          # Objetos de entrada/saída da API
├── exception/    # Exceções customizadas + GlobalExceptionHandler
├── security/     # TenantContext (isolamento por empresa)
└── util/         # GeoUtils (cálculo de distância/perímetro)
```

## Problemas comuns

| Sintoma | Causa provável | Solução |
|---|---|---|
| `Failed to configure a DataSource` | `application.yaml` ausente ou Postgres não está rodando | Confira os passos 1 e 2 |
| `Port 8080 was already in use` | Outra instância da API já está rodando | Pare todas as execuções na IDE antes de rodar de novo |
| `Could not resolve placeholder 'jwt.secret'` | Indentação errada no YAML (bloco `jwt` dentro de `spring` por engano) | O bloco `jwt` deve estar no mesmo nível de `spring`, não dentro dele |
| `PasswordEncoder bean não encontrado` | Faltam as dependências do JWT no `pom.xml` | Adicione `jjwt-api`, `jjwt-impl`, `jjwt-jackson` (veja abaixo) |
| Erro de CORS no navegador | `SecurityConfig` sem configuração de CORS ativa | Confirme que `corsConfigurationSource()` existe e está em `.cors(...)` |
| Login sempre retorna "Usuário ou senha inválidos" | Usuário não foi criado, ou senha não bate com o hash | Refaça o passo 4 |

## Dependências manuais (JWT)

O Spring Initializr não inclui JWT — adicione no `pom.xml`:

```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
```

Depois de adicionar, force o Maven a recarregar: botão direito no `pom.xml` → **Maven** → **Reload project** (ou `mvn clean install` no terminal).

## Parar tudo

```bash
docker compose down       # mantém os dados salvos
docker compose down -v    # também apaga os dados do banco
```