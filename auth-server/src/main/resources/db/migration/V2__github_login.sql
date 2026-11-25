-- Users who arrive from GitHub have no password until they choose one (it also encrypts their wallet).
ALTER TABLE users ALTER COLUMN password SET NULL;
