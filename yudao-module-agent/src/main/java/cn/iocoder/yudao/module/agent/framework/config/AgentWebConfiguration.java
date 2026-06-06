package cn.iocoder.yudao.module.agent.framework.config;

import cn.iocoder.yudao.module.agent.framework.security.AgentSecurityInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

/**
 * Agent Web 模块配置
 *
 * @author 芋道源码
 */
@Configuration
public class AgentWebConfiguration implements WebMvcConfigurer {

    @Resource
    private AgentProperties agentProperties;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 专门拦截工具接口，保证仅 Python 服务可访问。
        // 注意：不拦截 relay 接口，因为 relay 接口是由前端 (普通用户) 访问的。
        registry.addInterceptor(new AgentSecurityInterceptor(agentProperties))
                .addPathPatterns("/admin-api/agent/tools/**");
    }
}
