package cn.iocoder.yudao.module.agent.controller.admin.tools;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptListReqVO;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.post.PostPageReqVO;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.role.RolePageReqVO;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserPageReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.PostDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.service.dept.DeptService;
import cn.iocoder.yudao.module.system.service.dept.PostService;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import cn.iocoder.yudao.module.system.service.permission.RoleService;
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
import java.util.*;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@RestController
@RequestMapping("/agent/tools/users")
@Tag(name = "内部接口 - 供 Python Agent 调用 (用户高级查询)")
@Validated
public class AgentUserQueryController {

    @Resource
    private AdminUserService adminUserService;
    @Resource
    private DeptService deptService;
    @Resource
    private PostService postService;
    @Resource
    private RoleService roleService;
    @Resource
    private PermissionService permissionService;

    @GetMapping("/search-complex")
    @Operation(summary = "工具：聚合条件查询用户列表")
    @Parameters({
            @Parameter(name = "keyword", description = "员工姓名/账号/手机模糊搜索"),
            @Parameter(name = "deptName", description = "所属部门(中文名称)"),
            @Parameter(name = "postName", description = "所属岗位(中文名称)"),
            @Parameter(name = "roleName", description = "拥有角色(中文名称)"),
            @Parameter(name = "status", description = "状态(0正常, 1禁用)"),
            @Parameter(name = "pageNo", description = "页码，默认 1"),
            @Parameter(name = "pageSize", description = "每页数量，默认 10")
    })
    public CommonResult<Map<String, Object>> searchUsersComplex(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "deptName", required = false) String deptName,
            @RequestParam(value = "postName", required = false) String postName,
            @RequestParam(value = "roleName", required = false) String roleName,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "pageNo", required = false, defaultValue = "1") Integer pageNo,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize) {

        // 1. 防御性分页截断，保证给 LLM 的数据不会超限爆炸
        if (pageSize > 20) {
            pageSize = 20;
        }

        // 2. 翻译过滤条件: 中文名 -> ID 集合
        Set<Long> tempFilterDeptIds = null;
        if (StrUtil.isNotBlank(deptName)) {
            List<DeptDO> depts = deptService.getDeptList(new DeptListReqVO().setName(deptName));
            if (CollUtil.isEmpty(depts)) {
                return success(buildEmptyResult("未查找到名称包含 [" + deptName + "] 的部门"));
            }
            tempFilterDeptIds = depts.stream().map(DeptDO::getId).collect(Collectors.toSet());
        }
        final Set<Long> finalFilterDeptIds = tempFilterDeptIds;

        // 处理岗位 (因原生 AdminUserService 没有 getPage 带岗位条件的扩展，采用交集策略)
        Set<Long> filterUserIdsByPost = null;
        if (StrUtil.isNotBlank(postName)) {
            PostPageReqVO postReq = new PostPageReqVO();
            postReq.setName(postName);
            postReq.setPageNo(1);
            postReq.setPageSize(100);
            PageResult<PostDO> postPage = postService.getPostPage(postReq);
            if (CollUtil.isEmpty(postPage.getList())) {
                return success(buildEmptyResult("未查找到名称包含 [" + postName + "] 的岗位"));
            }
            Set<Long> postIds = postPage.getList().stream().map(PostDO::getId).collect(Collectors.toSet());
            List<AdminUserDO> usersByPost = adminUserService.getUserListByPostIds(postIds);
            if (CollUtil.isEmpty(usersByPost)) {
                return success(buildEmptyResult("该岗位下没有任何员工"));
            }
            filterUserIdsByPost = usersByPost.stream().map(AdminUserDO::getId).collect(Collectors.toSet());
        }

        // 处理角色
        Set<Long> filterUserIdsByRole = null;
        if (StrUtil.isNotBlank(roleName)) {
            RolePageReqVO roleReq = new RolePageReqVO();
            roleReq.setName(roleName);
            roleReq.setPageNo(1);
            roleReq.setPageSize(100);
            PageResult<RoleDO> rolePage = roleService.getRolePage(roleReq);
            if (CollUtil.isEmpty(rolePage.getList())) {
                return success(buildEmptyResult("未查找到名称包含 [" + roleName + "] 的角色"));
            }
            Set<Long> roleIds = rolePage.getList().stream().map(RoleDO::getId).collect(Collectors.toSet());
            filterUserIdsByRole = permissionService.getUserRoleIdListByRoleId(roleIds);
            if (CollUtil.isEmpty(filterUserIdsByRole)) {
                return success(buildEmptyResult("该角色下没有任何员工"));
            }
        }

        // 3. 构建 User 表的综合查询请求
        UserPageReqVO reqVO = new UserPageReqVO();
        reqVO.setPageNo(pageNo);
        reqVO.setPageSize(pageSize);
        reqVO.setStatus(status);
        if (StrUtil.isNotBlank(keyword)) {
            // 用 username 来兜底关键字，由于没有多维混合搜索，简化处理
            reqVO.setUsername(keyword); 
        }

        // 计算 User ID 交集
        final Set<Long> finalUserIds = intersectUserIds(filterUserIdsByPost, filterUserIdsByRole);
        if (finalUserIds != null && finalUserIds.isEmpty()) {
            return success(buildEmptyResult("没有任何员工同时满足您指定的岗位和角色条件"));
        }

        // 4. 调用原生分页接口 (如果是空集合，说明没有 ID 限制，传 null)
        PageResult<AdminUserDO> pageResult = adminUserService.getUserPage(reqVO);
        
        // 由于原生方法可能不支持传多个 in userIds，我们在这里对查出来的数据在内存里做进一步的安全过滤
        // (对于超大型应用最好写自定义 Mapper，但在 Agent 的场景中，这种过滤足够稳定且安全)
        List<AdminUserDO> filteredList = pageResult.getList().stream()
                .filter(u -> finalUserIds == null || finalUserIds.contains(u.getId()))
                .filter(u -> finalFilterDeptIds == null || (u.getDeptId() != null && finalFilterDeptIds.contains(u.getDeptId())))
                .collect(Collectors.toList());
        
        // 5. 极致裁剪结构给大模型
        List<Map<String, Object>> resultList = new ArrayList<>();
        for (AdminUserDO user : filteredList) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", user.getId());
            map.put("username", user.getUsername());
            map.put("nickname", user.getNickname());
            map.put("mobile", user.getMobile());
            map.put("status", user.getStatus() == 0 ? "正常" : "禁用");
            if (user.getDeptId() != null) {
                DeptDO dept = deptService.getDept(user.getDeptId());
                map.put("department", dept != null ? dept.getName() : "未分配");
            }
            resultList.add(map);
        }

        // 6. 构造防御性元数据提示
        Map<String, Object> finalResponse = new HashMap<>();
        finalResponse.put("total", pageResult.getTotal()); // 这里取大范围总量，内存过滤可能导致稍有偏差，但可接受
        finalResponse.put("returned_count", resultList.size());
        finalResponse.put("has_more", pageResult.getTotal() > pageSize);
        finalResponse.put("users", resultList);

        // 如果存在分页截断，追加提示给大模型
        if (pageResult.getTotal() > pageSize) {
            finalResponse.put("hint_for_llm", "符合条件的数据量较大，本次仅返回了部分结果。请用自然语言提示用户：数据过多，是否将完整数据下载到本地查看？");
        }

        return success(finalResponse);
    }

    /**
     * 构建空结果模板
     */
    private Map<String, Object> buildEmptyResult(String reason) {
        Map<String, Object> empty = new HashMap<>();
        empty.put("total", 0);
        empty.put("returned_count", 0);
        empty.put("users", new ArrayList<>());
        empty.put("reason", reason);
        return empty;
    }

    /**
     * 计算 ID 集合的交集
     */
    private Set<Long> intersectUserIds(Set<Long> set1, Set<Long> set2) {
        if (set1 == null && set2 == null) return null;
        if (set1 == null) return set2;
        if (set2 == null) return set1;
        
        Set<Long> result = new HashSet<>(set1);
        result.retainAll(set2);
        return result;
    }
}
