#!/bin/bash
set -e

# Connect as schema_owner and create the schema
psql -v ON_ERROR_STOP=1 --username financial_tracker_user --dbname financial_tracker_db <<-EOSQL
    \connect financial_tracker_db financial_tracker_user

  CREATE SCHEMA if not exists financial_tracker_schema AUTHORIZATION financial_tracker_user;
  ALTER USER financial_tracker_user SET search_path to 'financial_tracker_schema';
  GRANT ALL ON SCHEMA "financial_tracker_schema" TO financial_tracker_user;
GRANT ALL ON SCHEMA "financial_tracker_schema" TO financial_tracker_apps;
EOSQL
