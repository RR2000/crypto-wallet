-- Users who arrive from GitHub have no password until they choose one (it also encrypts their wallet).
-- Same change as h2/V2__github_login.sql, in PostgreSQL syntax.
ALTER TABLE users ALTER COLUMN password DROP NOT NULL;
