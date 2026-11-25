-- Users of the authorization server. The wallet column holds the browser-encrypted keystore
-- as JSON ([encrypted accounts, account names, account in use]): the server never sees a private key.
create table users (
    email                  varchar(320) primary key,
    password               varchar(100) not null,
    first_name             varchar(100),
    last_name              varchar(100),
    wallet                 text         not null default '[[],[],[]]',
    is_account_expired     boolean      not null default false,
    is_account_locked      boolean      not null default false,
    is_credentials_expired boolean      not null default false,
    is_enabled             boolean      not null default true
);

create table authorities (
    user_email varchar(320) not null references users (email) on delete cascade,
    authority  varchar(50)  not null
);
