--- execute using hpa account
DROP DATABASE IF EXISTS financial_tracker_db;
DROP SCHEMA IF EXISTS financial_tracker_user;
DROP USER IF EXISTS financial_tracker_user;
DROP USER IF EXISTS financial_tracker_apps;
CREATE USER financial_tracker_user WITH PASSWORD 'financial_tracker_user' CREATEDB;
CREATE USER financial_tracker_apps WITH PASSWORD 'financial_tracker_apps';
CREATE DATABASE financial_tracker_db WITH OWNER = financial_tracker_user;

--- execute using financial_tracker_user
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE SCHEMA if not exists financial_tracker_schema AUTHORIZATION financial_tracker_user;
ALTER USER financial_tracker_user SET search_path to 'financial_tracker_schema';
GRANT ALL ON SCHEMA "financial_tracker_schema" TO financial_tracker_user;
GRANT ALL ON SCHEMA "financial_tracker_schema" TO financial_tracker_apps;
