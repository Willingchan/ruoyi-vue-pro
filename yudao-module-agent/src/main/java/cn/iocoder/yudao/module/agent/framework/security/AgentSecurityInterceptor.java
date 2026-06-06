package cn.iocoder.yudao.module.agent.framework.security;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.agent.framework.config.AgentProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Agent 工具接口安全拦截器
 * 用于校验请求是否来自受信的 Python Agent 节点
 *
 * @author 芋道源码
 */
@Slf4j
@RequiredArgsConstructor
public class AgentSecurityInterceptor implements HandlerInterceptor {

    private final AgentProperties agentProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String secret = request.getHeader("X-Agent-Secret");
        
        // 校验 Header 中携带的 Secret 是否匹配 properties 中的配置
        if (StrUtil.isBlank(secret) || !secret.equals(agentProperties.getToolsSecret())) {
            log.warn("[preHandle][非法访问 Agent Tools API, URI: {}, IP: {}]", request.getRequestURI(), request.getRemoteAddr());
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return false;
        }
        
        return true; // 校验通过，放行
    }
}
