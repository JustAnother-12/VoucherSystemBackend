package com.dacn.VoucherSystem.enums;

public enum TimeGranularity {
    HOUR("hour"),
    DAY("day");

    private final String sqlUnit;

    TimeGranularity(String sqlUnit) {
        this.sqlUnit = sqlUnit;
    }

    public String sqlUnit() {
        return sqlUnit;
    }
}