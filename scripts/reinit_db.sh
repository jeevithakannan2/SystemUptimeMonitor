#!/bin/bash

# Load .env variables
if [ -f .env ]; then
  export $(grep -v '^#' .env | xargs)
fi

# Default values if .env is missing or incomplete
DB_URL=${DB_URL:-jdbc:postgresql://localhost:5432/sysuptimemonitor}

# Parse JDBC URL to standard PG params
if [[ "$DB_URL" =~ jdbc:postgresql://([^:/]+)(:([0-9]+))?/([^?]+) ]]; then
    DB_HOST=${BASH_REMATCH[1]}
    DB_PORT=${BASH_REMATCH[3]:-5432}
    DB_NAME=${BASH_REMATCH[4]}
fi

# Export PG environment variables for psql
export PGPASSWORD=$DB_PASSWORD
export PGUSER=$DB_USERNAME
export PGHOST=$DB_HOST
export PGPORT=$DB_PORT
export PGDATABASE=$DB_NAME

if [ "$1" == "history" ]; then
    echo "Clearing monitor history (incidents, runs, audits)..."
    psql -f scripts/clear_monitor_history.sql
elif [ "$1" == "all" ]; then
    echo "Clearing ALL data..."
    psql -f scripts/clear_all_data.sql
else
    echo "Usage: $0 [history|all]"
    echo "  history: Clears incidents, monitor runs, and audit logs. Resets failure counts."
    echo "  all:     Clears ALL tables including users and monitors."
    exit 1
fi
