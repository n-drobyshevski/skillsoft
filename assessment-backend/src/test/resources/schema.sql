-- H2 doesn't support JSONB natively, create a type alias instead
CREATE TYPE IF NOT EXISTS "JSONB" AS JSON;