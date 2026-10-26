#!/bin/bash
# Creates the Apparat databases and loads database/setup.sql into each one.
#   ./database/setup-db.sh          creates apparat2 (the app) and apparat2_test (the tests)
#   ./database/setup-db.sh --yes    same, without asking for confirmation
#
# The MySQL connection can be changed with environment variables:
#   MYSQL_HOST (default 127.0.0.1)   MYSQL_PORT (default 3308)
#   MYSQL_USER (default root)        MYSQL_PWD  (default empty)
#
# WARNING: setup.sql drops and recreates every Apparat table, so any data in
# these two databases is replaced.

set -e

HOST="${MYSQL_HOST:-127.0.0.1}"
PORT="${MYSQL_PORT:-3308}"
USER="${MYSQL_USER:-root}"
DIR="$(cd "$(dirname "$0")" && pwd)"

if [ "$1" != "--yes" ]; then
    echo "This will DROP and recreate all tables in 'apparat2' and 'apparat2_test'"
    echo "on $HOST:$PORT. Any data in them will be lost."
    read -r -p "Type yes to continue: " answer
    if [ "$answer" != "yes" ]; then
        echo "Cancelled."
        exit 1
    fi
fi

for db in apparat2 apparat2_test; do
    echo "Setting up $db ..."
    mysql -h "$HOST" -P "$PORT" -u "$USER" --protocol=TCP \
        -e "CREATE DATABASE IF NOT EXISTS $db CHARACTER SET utf8mb4;"
    mysql -h "$HOST" -P "$PORT" -u "$USER" --protocol=TCP "$db" < "$DIR/setup.sql"
done

echo "Done. Demo logins are listed in README.md."
