package cn.iocoder.yudao.module.aibidding.integration;

import cn.iocoder.yudao.module.aibidding.AiBiddingApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = AiBiddingApplication.class)
class SmokeTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void shouldLoadSpringContext() {
        assertThat(context).isNotNull();
        assertThat(context.getBeanDefinitionCount()).isGreaterThan(0);
    }
}
