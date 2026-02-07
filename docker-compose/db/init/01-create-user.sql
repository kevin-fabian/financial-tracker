DROP DATABASE IF EXISTS financial_tracker_db;
DROP SCHEMA IF EXISTS financial_tracker_user;
DROP USER IF EXISTS financial_tracker_user;
DROP USER IF EXISTS financial_tracker_apps;
CREATE USER financial_tracker_user WITH PASSWORD 'financial_tracker_user' CREATEDB;
CREATE USER financial_tracker_apps WITH PASSWORD 'financial_tracker_apps';
CREATE DATABASE financial_tracker_db WITH OWNER = financial_tracker_user;