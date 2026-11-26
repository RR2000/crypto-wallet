-- "Forgot password" links sent by email. Only the SHA-256 of the random token is kept; a link works once (used_at)
-- and until expires_at. Deleting a user deletes its links.
create table password_reset_tokens (
    token_hash varchar(64)              primary key,
    user_email varchar(320)             not null references users (email) on delete cascade,
    created_at timestamp with time zone not null,
    expires_at timestamp with time zone not null,
    used_at    timestamp with time zone
);
create index password_reset_tokens_user on password_reset_tokens (user_email);
