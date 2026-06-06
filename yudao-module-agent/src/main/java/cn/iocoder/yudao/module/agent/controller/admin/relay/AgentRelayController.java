package cn.iocoder.yudao.module.agent.controller.admin.relay;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.agent.service.relay.AgentRelayServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@RestController
@RequestMapping("/agent/relay")
@Tag(name = "管理后台 - Agent 转发代理")
public class AgentRelayController {

    @Resource
    private AgentRelayServiceImpl agentRelayService;

    @PostMapping(value = "/run", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式调用 Python Agent")
    public SseEmitter run(@RequestBody Map<String, Object> reqVO) {
        // 注意：Controller 返回 SseEmitter 时，
        // Yudao 框架现有的 GlobalResponseBodyHandler 不会对其进行 CommonResult 包装，
        // 因此可以直接安全透传给前端
        return agentRelayService.relayRun(reqVO);
    }

    @GetMapping("/conversations")
    @Operation(summary = "获取当前用户的会话列表")
    public CommonResult<Object> getConversationList(
            @RequestParam(value = "skip", required = false, defaultValue = "0") @Parameter(description = "跳过记录数") Integer skip,
            @RequestParam(value = "limit", required = false, defaultValue = "100") @Parameter(description = "每页数量") Integer limit) {
        return success(agentRelayService.getConversationList(skip, limit));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    @Operation(summary = "获取单次会话历史记录")
    public CommonResult<Object> getConversationMessages(
            @PathVariable("conversationId") @Parameter(description = "会话 ID") String conversationId) {
        return success(agentRelayService.getConversationMessages(conversationId));
    }
}
