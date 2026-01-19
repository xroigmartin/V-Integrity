#!/bin/bash
set -e

# Variables loaded from environment or defaults
# Note: POSTGRES_USER (superuser) is already created by the image
APP_USER=${POSTGRES_APP_USER:-ledger_app}
APP_PASSWORD=${POSTGRES_APP_PASSWORD:-ledger_app_password}
OWNER_USER=${POSTGRES_OWNER_USER:-ledger_owner}
OWNER_PASSWORD=${POSTGRES_OWNER_PASSWORD:-ledger_owner_password}
DB_NAME=${POSTGRES_DB:-ledger}
SCHEMA_NAME="ledger"

echo "Creating roles and database configuration..."

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    -- 1. Create Roles if they don't exist
    DO
    \$do\$
    BEGIN
       IF NOT EXISTS (
          SELECT FROM pg_catalog.pg_roles
          WHERE  rolname = '$OWNER_USER') THEN
          CREATE ROLE $OWNER_USER LOGIN PASSWORD '$OWNER_PASSWORD';
       END IF;
       IF NOT EXISTS (
          SELECT FROM pg_catalog.pg_roles
          WHERE  rolname = '$APP_USER') THEN
          CREATE ROLE $APP_USER LOGIN PASSWORD '$APP_PASSWORD';
       END IF;
    END
    \$do\$;

    -- 2. Create Schema (owned by owner_user)
    CREATE SCHEMA IF NOT EXISTS $SCHEMA_NAME AUTHORIZATION $OWNER_USER;

    -- 3. Grant Privileges

    -- Owner needs full control over the schema
    GRANT ALL PRIVILEGES ON SCHEMA $SCHEMA_NAME TO $OWNER_USER;
    -- Allow owner to create objects in the database (needed for Flyway history table if public)
    GRANT CREATE ON DATABASE $DB_NAME TO $OWNER_USER;

    -- App User needs usage on schema
    GRANT USAGE ON SCHEMA $SCHEMA_NAME TO $APP_USER;

    -- App User needs SELECT/INSERT on all future tables in this schema
    ALTER DEFAULT PRIVILEGES FOR ROLE $OWNER_USER IN SCHEMA $SCHEMA_NAME
    GRANT SELECT, INSERT ON TABLES TO $APP_USER;

    -- Also grant on sequences for ID generation
    ALTER DEFAULT PRIVILEGES FOR ROLE $OWNER_USER IN SCHEMA $SCHEMA_NAME
    GRANT USAGE, SELECT ON SEQUENCES TO $APP_USER;

    -- Explicitly DENY UPDATE/DELETE for App User (Defense in Depth)
    -- Note: Postgres doesn't have explicit DENY, but by NOT granting them, we achieve it.
    -- The triggers we added in V1 are the second layer of defense.
EOSQL

echo "Database initialization completed successfully."
