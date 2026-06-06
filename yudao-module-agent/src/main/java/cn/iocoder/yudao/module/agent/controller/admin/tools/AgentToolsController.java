package cn.iocoder.yudao.module.agent.controller.admin.tools;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/agent/tools")
@Tag(name = "内部接口 - 供 Python Agent 工具调用")
@Validated
public class AgentToolsController {

    @GetMapping("/users/stats")
    @Operation(summary = "获取用户统计数据(示例)")
    @Parameters({
            @Parameter(name = "startDate", description = "开始日期 (yyyy-MM-dd)", required = true, example = "2023-01-01"),
            @Parameter(name = "endDate", description = "结束日期 (yyyy-MM-dd)", required = true, example = "2023-12-31")
    })
    public CommonResult<Map<String, Object>> getUserStats(
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate) {
        
        // 模拟业务逻辑
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", 1024);
        stats.put("activeUsers", 256);
        stats.put("startDate", startDate);
        stats.put("endDate", endDate);
        
        return CommonResult.success(stats);
    }
}
