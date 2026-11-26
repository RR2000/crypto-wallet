# oauth2-blockchain

An OAuth2 / OpenID Connect provider that also keeps your Ethereum wallets: you sign in with an email and a
password (or with GitHub), like on any website, and your accounts are there on every device. Private keys are
encrypted in the browser with your password; the server only ever stores ciphertext.

This is the project of my bachelor's thesis in Computer Science at the University of Turin (2022-2023),
carried out at ALTEN: *Integration between authentication services and blockchain: how to manage cryptographic
wallets* ([thesis](https://github.com/RR2000/RR2000/blob/main/Rondinella-Raoul-Thesis.pdf) and
[presentation](https://github.com/RR2000/RR2000/blob/main/Rondinella-Raoul-Presentation.pdf), in Italian).

**Live demo: [wallet.raoulrondinella.dev](https://wallet.raoulrondinella.dev)**, on the Sepolia test network.
Test ETH is worth nothing: never import a key that holds real funds.

## How it works

```
browser                                         wallet.raoulrondinella.dev
┌─────────────────────────────┐   /            ┌──────────────────────────────────────┐
│ web app (Vue 3 + Vuetify)   │ ─────────────► │ web: nginx, static files              │
│  - OIDC login, code + PKCE  │   /auth/*      │      └─► auth: Spring Authorization   │
│  - decrypt / encrypt keys   │ ─────────────► │          Server, users + wallets (SQL)│
│  - balances via JSON-RPC ───┼──► Sepolia     └──────────────────────────────────────┘
└─────────────────────────────┘
```

1. The web app is a public OAuth2 client: Authorization Code with PKCE, no client secret in the browser.
2. The `wallet` scope adds the encrypted wallet (Ethereum keystore V3 JSON) to the user's profile.
3. To create, import or reveal a key, the app asks for the password, checks it on the server, then decrypts
   and re-encrypts the keystore locally. Only ciphertext goes back to the server.
4. Balances come straight from a public Sepolia node.
5. Optional sign in with GitHub: a GitHub account lands in the local account with the same *verified* email (or a
   new one, which then chooses a password). A forgotten password can be replaced after a GitHub login; nothing is
   deleted: accounts encrypted with the old password stay in the wallet, locked, and open again with it.
6. Optional "forgot password" by email: a single-use link, valid 30 minutes, to choose a new password. Same rule as
   with GitHub: the wallet is not touched, the keys encrypted with the old password stay locked until it turns up.

| part | stack |
|---|---|
| `auth-server/` | Java 21, Spring Boot 4, Spring Security authorization server, JPA, Flyway, PostgreSQL (H2 locally) |
| `web/` | Vue 3, Vuetify 3, oidc-client-ts, ethers 6, Vite |

## Run it locally

With Docker: `docker compose up --build`, then open http://127.0.0.1:8082.

Without Docker, in two terminals:

```sh
cd auth-server && ./mvnw spring-boot:run   # http://127.0.0.1:9000/auth
cd web && npm ci && npm run dev             # http://127.0.0.1:5173 (proxies /auth to :9000)
```

Sign in with GitHub is on when `GITHUB_CLIENT_ID` and `GITHUB_CLIENT_SECRET` are set, from a GitHub OAuth App
with callback `<server>/auth/login/oauth2/code/github` (locally `http://127.0.0.1:5173/auth/login/oauth2/code/github`).

"Forgot password" by email is on when `SMTP_HOST`, `SMTP_USERNAME`, `SMTP_PASSWORD` and `MAIL_FROM` are set
(`SMTP_PORT` defaults to 587, STARTTLS is required); the links point to `WALLET_WEB_URL`, never to the Host of the
request. Otherwise the login page does not offer it and `/auth/forgotPassword` answers 404.
The request always gets the same answer, whether an account has the email or not, and the mail leaves in the
background; the link holds 32 random bytes and the database keeps only their SHA-256; a new password voids the
other links. At most 10 requests per hour per client address, 3 mails per hour per email and 50 per hour in all.
The token travels in the URL fragment (`/auth/resetPassword#token=...`), so it never reaches an access log.

Use `127.0.0.1`, not `localhost`: the authorization server rejects `localhost` redirect URIs (RFC 8252).

Tests: `cd auth-server && ./mvnw verify` (it also starts a real PostgreSQL, no Docker needed), `cd web && npm test`.

### Database

Locally the server uses an H2 file (`./data/wallet`), the tests H2 in memory and an embedded PostgreSQL; the demo
uses PostgreSQL:
`WALLET_DB_URL` (e.g. `jdbc:postgresql://postgres:5432/wallet`), `WALLET_DB_USER`, `WALLET_DB_PASSWORD`.
Flyway owns the schema: portable migrations in `db/migration/common`, the few that need a dialect in
`db/migration/h2` and `db/migration/postgresql`.

Moving from H2: start once with `WALLET_IMPORT_FROM=jdbc:h2:file:/data/wallet;IFEXISTS=TRUE`. Right after the
migrations, before serving requests, the server copies users and authorities from that file in one transaction,
only if the destination is empty, and logs the counts (`wallet import: ...`), never the rows. Later restarts with
the variable still set do nothing.

## What changed since the thesis

The thesis version (early 2023) was brought back to life in 2026 with the smallest set of changes that makes it
safe to run in public:

- Spring Boot 3.0 to 4.1, Java 17 to 21; the web app rewritten on current libraries (web3.js 1.x replaced by
  ethers 6, the admin template's demo pages removed), `npm audit` clean.
- The web app is a public client with PKCE instead of a confidential client with a secret shipped to the browser.
- The bcrypt hash of the password is no longer part of the profile: the password is checked by the server
  (`/api/password/verify`), and password changes verify the current one server-side.
- Registering with an existing email no longer overwrites that account.
- Database schema managed by Flyway; web app and server on the same origin, so no CORS.
- The enterprise Proof-of-Authority network used at the time is gone: the demo runs on Sepolia.

Known limits of a demo: the signing key of the tokens is generated at startup (a restart signs everyone out),
and the demo is a single instance with a single database.
