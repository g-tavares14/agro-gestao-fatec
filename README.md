# Agro-Gestão

[![Licença: MIT](https://img.shields.io/badge/licen%C3%A7a-MIT-2e7d32.svg)](LICENSE)

Plataforma acadêmica de gestão rural para **pequenos produtores de Mogi das Cruzes**. O sistema reúne propriedade, culturas, custos de produção, diário de campo, financeiro e documentos num só lugar — inclusive importando fichas de custo em PDF (formato EMATER-DF) com o modelo generativo Gemini.

A chave do Gemini fica **somente no backend**; o navegador nunca a recebe.

## Contexto acadêmico

Este repositório é o produto de um **Trabalho de Conclusão de Curso** desenvolvido em parceria com a **Fatec Mogi das Cruzes**, com o time de **Agronegócios**.

A proposta do TCC é **levar tecnologia a pequenos agricultores da região de Mogi das Cruzes**. Em sua maioria, esses produtores não têm um meio sistemático de controlar o processo produtivo: custos, tratos culturais, receitas e documentos ficam no caderno, na memória ou em planilhas soltas. Sem esse controle, fica difícil saber o custo real da lavoura, o resultado de cada cultura e o que a propriedade de fato ganha ou perde.

O Agro-Gestão responde a esse problema com um sistema web simples de operar, que:

- cadastra a propriedade e as culturas do ciclo;
- organiza o custo de produção em quatro grupos (ações mecânicas, ações manuais, insumos e demais despesas);
- permite lançar o diário de campo e o financeiro (incluindo um demonstrativo por cultura);
- guarda documentos da propriedade com acesso autenticado;
- opcionalmente lê a ficha de custo em PDF e devolve os dados para revisão humana antes de gravar.

É um software acadêmico, pensado para demonstrar o uso de tecnologia acessível no cotidiano do pequeno produtor — não um produto comercial.

## Funcionalidades

| Módulo | O que faz |
| --- | --- |
| Autenticação | Cadastro e login por e-mail e senha; login com Google (opcional) |
| Propriedades | Cadastro da terra (nome, município, área) |
| Culturas | Ciclo, variedade, produtividade e status por talhão |
| Extração de PDF | A IA lê a ficha EMATER e separa os quatro grupos de custo; o usuário revisa e confirma |
| Custos | Lançamentos por cultura, na mesma classificação da planilha de campo |
| Diário de campo | Tratos, ocorrências e observações do talhão |
| Financeiro | Receitas, despesas e demonstrativo simples (por cultura ou consolidado) |
| Documentos | Contratos, notas e laudos da propriedade, com download autenticado |
| Painel | Visão geral da operação autenticada |

## Stack

| Camada | Tecnologia |
| --- | --- |
| Interface | Angular **21 LTS**, servida por Nginx |
| API | Java **21** + Spring Boot **3.5.16** |
| Autenticação | JWT (e-mail e senha) e Google OAuth 2.0 (opcional) |
| Análise de PDF | Google Gemini (`gemini-2.5-flash` por padrão), só no backend |
| Persistência | JPA / Hibernate + Spring Data JPA (PostgreSQL 16) |
| Objetos (PDF e anexos) | MinIO (S3-compatível), bucket privado |
| Empacotamento | Docker Compose |

O Nginx do frontend encaminha `/api`, `/oauth2` e `/login/oauth2` para o backend na porta interna 8080.

## Pré-requisitos

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) instalado e em execução (Docker Engine + Compose v2).
- Porta **80** livre no computador (interface web). As portas **5432** (PostgreSQL), **9000** e **9001** (MinIO) também são publicadas para inspeção local.

Não é necessário instalar Java, Node.js ou PostgreSQL na máquina hospedeira.

## Configuração do ambiente

Na raiz do repositório:

```bash
cp .env.example .env
```

Edite o `.env` e **altere** pelo menos:

- `POSTGRES_PASSWORD`
- `JWT_SECRET` (o valor de exemplo é apenas um placeholder; gere outro, por exemplo com `openssl rand -hex 32`)
- `MINIO_ROOT_PASSWORD` e `MINIO_SECRET_KEY` (mantenha-os iguais em desenvolvimento)

O arquivo `.env` **não** é versionado (está no `.gitignore`). Use só o `.env.example` como modelo.

## Chave do Gemini (análise de PDF)

1. Acesse o [Google AI Studio](https://aistudio.google.com/apikey).
2. Crie uma chave de API.
3. Atribua o valor a `GEMINI_API_KEY` no `.env`.

Essa chave **nunca** vai para o frontend: apenas o serviço `backend` a lê e chama a API do Gemini.

**Sem `GEMINI_API_KEY`**, cadastro, autenticação e o restante da aplicação funcionam normalmente. A rota de análise de PDF responde com um erro explícito informando que a chave não está configurada.

## Google OAuth (login social, opcional)

O login com Google usa **credenciais OAuth de um projeto no Google Cloud**, distintas da chave do Gemini.

**Gemini (AI Studio) ≠ OAuth (Google Cloud).** São produtos, consoles e credenciais diferentes. Preencher `GEMINI_API_KEY` não habilita o botão “Entrar com Google”; preencher `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` não habilita a extração de PDF.

Para habilitar o OAuth:

1. Abra o [Google Cloud Console](https://console.cloud.google.com/) e crie (ou selecione) um projeto.
2. Ative a **People API** (dados básicos do perfil do usuário).
3. Em **APIs e serviços → Tela de consentimento OAuth**, configure o aplicativo (tipo Externo em testes acadêmicos) e adicione o e-mail de teste.
4. Em **APIs e serviços → Credenciais**, crie um cliente **OAuth 2.0 → Aplicativo da Web**.
5. Origens JavaScript autorizadas:
   - `http://localhost`
6. URIs de redirecionamento autorizados:
   - `http://localhost/login/oauth2/code/google`
7. Copie o ID e o segredo do cliente para `GOOGLE_CLIENT_ID` e `GOOGLE_CLIENT_SECRET` no `.env`.

**Sem essas variáveis**, o login por e-mail e senha continua disponível.

## Subir o sistema

Na raiz do repositório:

```bash
docker compose up --build
```

Aguarde o PostgreSQL e o MinIO ficarem saudáveis, o job `minio-init` criar o bucket privado `agro-gestao` e o backend / frontend iniciarem.

Abra no navegador:

[http://localhost](http://localhost)

Para encerrar:

```bash
docker compose down
```

Os dados do PostgreSQL e do MinIO permanecem nos volumes nomeados do Compose.

## Como testar a extração de PDF

1. Confirme que `GEMINI_API_KEY` está preenchida no `.env` e que os containers foram recriados depois da alteração (`docker compose up --build`).
2. Autentique-se na interface (e-mail/senha ou Google, se configurado).
3. Envie o arquivo de exemplo [`docs/alface.pdf`](docs/alface.pdf) — ficha EMATER-DF *Custo de produção por hectare*, cultura Alface (Aspersão).
4. Confira o JSON extraído contra o contrato em [`docs/formato-pdf-cultura.md`](docs/formato-pdf-cultura.md). Campos ausentes na planilha devem vir `null`; o modelo não deve inventar variedade, área plantada nem data de plantio.

## Mapa de pastas

```
agro-gestao/
├── backend/                      # API Spring Boot (Dockerfile próprio)
├── frontend/                     # SPA + Nginx (proxy /api, /oauth2, /login/oauth2)
├── docs/
│   ├── alface.pdf                # Planilha de exemplo (EMATER-DF, Alface)
│   └── formato-pdf-cultura.md    # Contrato do JSON extraído pelo Gemini
├── docker-compose.yml            # postgres, minio, minio-init, backend, frontend
├── .env.example                  # Modelo de variáveis (sem segredos reais)
├── .gitignore                    # Dependências, builds, .env e dados locais
├── LICENSE                       # MIT
└── README.md
```

O `.gitignore` da raiz cobre o monorepo: `node_modules/`, `dist/`, `target/`, `.angular/`, `.idea/`, `.env` (e variantes locais), logs, cobertura de testes e diretórios de dados do MinIO. O `.env.example` continua versionado de propósito.

## Comportamento sem chaves externas

| Situação | Efeito |
| --- | --- |
| `GEMINI_API_KEY` vazia | O sistema sobe. Qualquer análise de PDF devolve erro claro; o restante permanece utilizável. |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` vazios | O sistema sobe. Apenas o login por e-mail e senha fica ativo. |

## Licença

Distribuído sob a [licença MIT](LICENSE).

## Autoria e créditos

- **Software:** [Guilherme Tavares](https://github.com/g-tavares14)
- **Parceria acadêmica:** Fatec Mogi das Cruzes — time de Agronegócios
- **Trabalho:** TCC com o objetivo de apoiar pequenos agricultores da região de Mogi das Cruzes no controle do processo produtivo
