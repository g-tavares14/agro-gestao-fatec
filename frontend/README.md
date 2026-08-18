# Agro-Gestão (frontend)

Angular 21 + Angular Material com tema agro (verde profundo e âmbar).

## Desenvolvimento

```bash
npm start
```

A aplicação sobe em `http://localhost:4200`. As rotas `/api`, `/oauth2` e `/login/oauth2` são encaminhadas para `http://localhost:8080`.

## Build

```bash
npm run build
```

A saída de produção fica em `dist/frontend/browser`.

## Docker

```bash
docker build -t agro-gestao-frontend .
```

O `nginx` da imagem atende o SPA na porta 80 e faz proxy do backend apenas em `/api/`, `/oauth2/` e `/login/oauth2/`.
