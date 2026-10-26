CREATE ROLE voucher_app
    WITH LOGIN PASSWORD '123';

CREATE DATABASE vouchersystem_dev OWNER voucher_app;

CREATE DATABASE vouchersystem_test OWNER voucher_app;