package cn.iocoder.yudao.module.agent.framework.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Agent 代理与工具配置
 *
 * @author 芋道源码
 */
@ConfigurationProperties(prefix = "agent")
@Component
@Data
public class AgentProperties {

    /**
     * Python Agent 的基础 URL，例如：http://127.0.0.1:8000
     */
    private String serviceUrl;

    /**
     * 调用 Python 时的鉴权 Key (前向代理)
     */
    private String serviceApiKey;

    /**
     * 允许 Python 调用的鉴权 Key (反向工具调用)
     */
    private String toolsSecret;

}
