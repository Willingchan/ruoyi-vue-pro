package cn.iocoder.yudao.module.agent.controller.admin.tools;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.service.dept.DeptService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@RestController
@RequestMapping("/agent/tools")
@Tag(name = "内部接口 - 供 Python Agent 工具调用")
@Validated
public class AgentToolsController {

    @Resource
    private AdminUserService adminUserService;
    
    @Resource
    private DeptService deptService;

    @GetMapping("/users/info")
    @Operation(summary = "工具：根据手机号或账号获取单个员工详情（包括查询 '自己' 的信息）")
    @Parameters({
            @Parameter(name = "keyword", description = "账号、手机号，或者透传当前上下文的 X-User-Id 数字", required = true, example = "13800138000")
    })
    public CommonResult<Map<String, Object>> getUserInfo(
            @RequestParam("keyword") String keyword) {

        AdminUserDO user = null;
        
        // 判断 keyword 是否是纯数字（大模型用 X-User-Id 查自己的时候）
        try {
            Long userId = Long.parseLong(keyword);
            user = adminUserService.getUser(userId);
        } catch (NumberFormatException e) {
            // ignore
        }

        // 尝试通过手机号或账号查询
        if (user == null) {
            user = adminUserService.getUserByMobile(keyword);
        }
        if (user == null) {
            user = adminUserService.getUserByUsername(keyword);
        }
        
        if (user == null) {
            return CommonResult.error(404, "未查找到对应员工信息");
        }

        Map<String, Object> simpleUser = new HashMap<>();
        simpleUser.put("id", user.getId());
        simpleUser.put("username", user.getUsername());
        simpleUser.put("nickname", user.getNickname());
        simpleUser.put("mobile", user.getMobile());
        simpleUser.put("email", user.getEmail());
        simpleUser.put("status", user.getStatus() == 0 ? "正常" : "禁用");
        simpleUser.put("remark", user.getRemark());
        simpleUser.put("createTime", user.getCreateTime()); // 入职/创建时间可能对个人查询比较有用

        if (user.getDeptId() != null) {
            DeptDO dept = deptService.getDept(user.getDeptId());
            simpleUser.put("department", dept != null ? dept.getName() : "未分配");
        } else {
            simpleUser.put("department", "未分配");
        }

        return success(simpleUser);
    }
}
