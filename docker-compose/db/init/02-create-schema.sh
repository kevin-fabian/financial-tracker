#!/bin/bash
set -e

# Connect as postgres superuser to create the schema in financial_tracker_db
psql -v ON_ERROR_STOP=1 --username postgres --dbname postgres <<-EOSQL
-- Connect to financial_tracker_db and create the schema
\connect financial_tracker_db financial_tracker_user

CREATE SCHEMA if not exists financial_tracker_schema AUTHORIZATION financial_tracker_user;
ALTER USER financial_tracker_user SET search_path to 'financial_tracker_schema';
GRANT ALL ON SCHEMA "financial_tracker_schema" TO financial_tracker_user;
GRANT ALL ON SCHEMA "financial_tracker_schema" TO financial_tracker_apps;
EOSQL
