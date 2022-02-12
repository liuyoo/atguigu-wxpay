package com.example.demo;

import com.example.demo.config.WxPayConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.security.PrivateKey;

@SpringBootTest
class DemoApplicationTests {
    @Resource
    private WxPayConfig wxPayConfig;

    @Test
    void testgetPraivateKey() {
        String privateKeyPath = wxPayConfig.getPrivateKeyPath();

        PrivateKey praivateKey = wxPayConfig.getPraivateKey(privateKeyPath);

        System.out.println(praivateKey);

    }

}
