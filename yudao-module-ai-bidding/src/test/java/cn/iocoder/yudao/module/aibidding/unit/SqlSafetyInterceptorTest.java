package cn.iocoder.yudao.module.aibidding.unit;

import cn.iocoder.yudao.module.aibidding.service.llm.SqlSafetyInterceptor;
import cn.iocoder.yudao.module.aibidding.service.llm.SqlSafetyInterceptor.SqlSafetyException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SqlSafetyInterceptorTest {

    private final SqlSafetyInterceptor interceptor = new SqlSafetyInterceptor();

    @Test @DisplayName("T6.1: SELECT 通过")
    void shouldAllowSelect() {
        assertThatCode(() -> interceptor.validate(
                "SELECT * FROM contract WHERE region='广东'"))
                .doesNotThrowAnyException();
    }

    @Test @DisplayName("T6.2: DROP TABLE 被拦截")
    void shouldBlockDrop() {
        assertThatThrownBy(() -> interceptor.validate("DROP TABLE contract"))
                .isInstanceOf(SqlSafetyException.class)
                .hasMessageContaining("DROP");
    }

    @Test @DisplayName("T6.3: DELETE 被拦截")
    void shouldBlockDelete() {
        assertThatThrownBy(() -> interceptor.validate("DELETE FROM contract"))
                .isInstanceOf(SqlSafetyException.class)
                .hasMessageContaining("DELETE");
    }

    @Test @DisplayName("T6.4: UPDATE 被拦截")
    void shouldBlockUpdate() {
        assertThatThrownBy(() -> interceptor.validate("UPDATE contract SET name='x'"))
                .isInstanceOf(SqlSafetyException.class)
                .hasMessageContaining("UPDATE");
    }
}
