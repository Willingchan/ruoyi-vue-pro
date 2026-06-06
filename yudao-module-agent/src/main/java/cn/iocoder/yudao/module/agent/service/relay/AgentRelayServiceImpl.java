package cn.iocoder.yudao.module.agent.service.relay;

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
                // 默认将服务间调用的 API KEY 置入 Header 中
                .defaultHeader("X-Internal-API-Key", properties.getServiceApiKey())
                .build();
    }

    /**
     * 将前端请求代理到 Python 服务，并将返回的 SSE 流透传回前端
     *
     * @param requestBody 前端传递的请求体（如包含 query, conversation_id 等）
     * @return 用于打字机效果的 SseEmitter
     */
    public SseEmitter relayRun(Object requestBody) {
        // 设置 0L 表示不超时，因为大模型的思考时间可能较长
        SseEmitter emitter = new SseEmitter(0L);

        webClient.post()
                .uri("/api/v1/internal/run")
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
}
