package cn.iocoder.yudao.module.agent.service.relay;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.agent.framework.config.AgentProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Agent 代理转发服务实现
 *
 * @author 芋道源码
 */
@Service
@Slf4j
public class AgentRelayServiceImpl {

    private final WebClient webClient;

    public AgentRelayServiceImpl(AgentProperties properties) {
        this.webClient = WebClient.builder()
                .baseUrl(properties.getServiceUrl())
                // 注意 FastAPI 通常大小写不敏感，但有时候配置不对会坑，这里强制保持文档格式
                .defaultHeader("X-Internal-Api-Key", properties.getServiceApiKey())
                .build();
    }

    /**
     * 将前端请求代理到 Python 服务，并将返回的 SSE 流透传回前端
     *
     * @param requestBody 前端传递的请求体
     * @return 用于打字机效果的 SseEmitter
     */
    public SseEmitter relayRun(Object requestBody) {
        // 设置 0L 表示不超时，因为大模型的思考时间可能较长
        SseEmitter emitter = new SseEmitter(0L);

        Long userId = SecurityFrameworkUtils.getLoginUserId();
        // 如果后端当前没有拿到真实用户，为了过 FastAPI 的校验，可以临时先塞个默认值 "anonymous"
        String userIdStr = userId != null ? String.valueOf(userId) : "anonymous";
        
        log.info("[relayRun][发送请求到Python端] URL: {}, X-User-Id: {}", "/api/v1/internal/run", userIdStr);

        webClient.post()
                .uri("/api/v1/internal/run")
                .header("x-user-id", userIdStr)
                .bodyValue(requestBody)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                // 优雅映射标准的 SSE 事件
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {})
                .subscribe(
                        event -> {
                            try {
                                SseEmitter.SseEventBuilder builder = SseEmitter.event()
                                        .id(event.id())
                                        .name(event.event())
                                        .data(event.data());
                                emitter.send(builder);
                            } catch (Exception e) {
                                log.error("[relayRun][向前端推送 SSE 数据失败]", e);
                                emitter.completeWithError(e);
                            }
                        },
                        error -> {
                            log.error("[relayRun][调用 Python Agent 接口异常]", error);
                            emitter.completeWithError(error);
                        },
                        // 流式数据接收完成
                        emitter::complete
                );

        return emitter;
    }

    /**
     * 获取用户会话列表
     *
     * @param skip 跳过数量
     * @param limit 限制数量
     * @return 会话列表
     */
    public Object getConversationList(Integer skip, Integer limit) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        String userIdStr = userId != null ? String.valueOf(userId) : "anonymous";

        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/internal/conversations")
                        .queryParam("skip", skip)
                        .queryParam("limit", limit)
                        .build())
                .header("X-User-Id", userIdStr)
                .retrieve()
                .bodyToMono(Object.class)
                .block();
    }

    /**
     * 获取单次会话历史记录
     *
     * @param conversationId 会话 ID
     * @return 历史记录详情
     */
    public Object getConversationMessages(String conversationId) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        String userIdStr = userId != null ? String.valueOf(userId) : "anonymous";

        return webClient.get()
                .uri("/api/v1/internal/conversations/{conversation_id}/messages", conversationId)
                .header("X-User-Id", userIdStr)
                .retrieve()
                .bodyToMono(Object.class)
                .block();
    }
}
