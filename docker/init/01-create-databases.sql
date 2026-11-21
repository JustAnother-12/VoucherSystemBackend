\getenv app_password DB_PASSWORD

CREATE ROLE voucher_app
    WITH LOGIN PASSWORD :'app_password';

CREATE DATABASE vouchersystem_dev OWNER voucher_app;

CREATE DATABASE vouchersystem_test OWNER voucher_app;