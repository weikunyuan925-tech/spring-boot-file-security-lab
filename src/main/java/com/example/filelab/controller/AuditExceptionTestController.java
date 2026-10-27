
package com.example.filelab.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
//有两个限制：
//- 只有在 dev Profile 激活时，控制器才会注册。
//- 还必须显式启用 audit.test-endpoints.enabled，接口才会生效。
@RestController
@Profile("dev")
@ConditionalOnProperty(
        name = "audit.test-endpoints.enabled",
        havingValue = "true"
)
@RequestMapping("/api/audit-test")
public class AuditExceptionTestController {

    @GetMapping("/exception")
    public void triggerException() {
        throw new IllegalStateException(
                "TEST-SECRET-SHOULD-NOT-LEAK"
        );
    }
}
