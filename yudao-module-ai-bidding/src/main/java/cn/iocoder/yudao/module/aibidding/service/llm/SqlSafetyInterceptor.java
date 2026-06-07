package cn.iocoder.yudao.module.aibidding.service.llm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
public class SqlSafetyInterceptor {

    private static final Set<String> FORBIDDEN_KEYWORDS = Set.of(
            "DROP", "DELETE", "UPDATE", "INSERT", "ALTER",
            "TRUNCATE", "CREATE", "REPLACE", "MERGE", "GRANT", "REVOKE"
    );

    public void validate(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new SqlSafetyException("SQL 不能为空");
        }
        String upper = sql.toUpperCase().trim();
        for (String keyword : FORBIDDEN_KEYWORDS) {
            if (upper.matches(".*\\b" + keyword + "\\b.*")) {
                throw new SqlSafetyException("禁止的 SQL 操作: " + keyword);
            }
        }
        if (!upper.startsWith("SELECT") && !upper.startsWith("WITH")) {
            throw new SqlSafetyException("仅允许 SELECT 查询");
        }
    }

    public static class SqlSafetyException extends RuntimeException {
        public SqlSafetyException(String message) {
            super(message);
        }
    }
}
