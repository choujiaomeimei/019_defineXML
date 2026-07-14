package com.stat.service;

/**
 * Codelist 提取的统一后端入口。
 */
public interface CodelistExtractionService {

    enum Scope {
        VARIABLES,
        VLM,
        ALL;

        public static Scope from(String value, Scope defaultValue) {
            if (value == null || value.isBlank()) {
                return defaultValue;
            }
            try {
                return Scope.valueOf(value.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("不支持的提取范围: " + value + "，可选 VARIABLES/VLM/ALL");
            }
        }
    }

    CodelistExtractionResult extract(String projectId, String username, Scope scope);
}
