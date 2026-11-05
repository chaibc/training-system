package com.hanmo.calligraphy.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hanmo.calligraphy.common.ApiResponse;
import com.hanmo.calligraphy.common.BaseCrudController;
import com.hanmo.calligraphy.domain.entity.LeaveRequest;
import com.hanmo.calligraphy.domain.mapper.LeaveRequestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 请假申请记录：leave_request 表的查询通道（写入已关闭）。
 *
 * <p>业务模块：请假审批；数据表 leave_request；接口前缀 {@code /api/admin/leave}。</p>
 * <p>后台页面：管理端「请假审批」(/leave，Leave.vue) —— 页面用 /page 拉列表、/stats 拉指标卡，
 * 审批动作走 /api/admin/workflow/leaves/{id}/approve，补课安排走 /api/admin/workflow/leaves/{id}/makeup。</p>
 * <p>权限：该前缀不在 SecurityConfig 的「写操作仅 ADMIN」清单内，GET 允许 ADMIN 与 ACADEMIC（POST/PUT/DELETE 已关闭，返回 405）。</p>
 * <p>通道：继承 {@link BaseCrudController}，只开放 page / list / {id} 查询通道；create / update / delete 已关闭（见 {@link #writeChannelsEnabled()}）；
 * 该表无可搜索列，分页 keyword 会被忽略。</p>
 * <p>写入约束：通用写通道已关闭，POST / PUT / DELETE 返回 405；写入统一走对应的业务接口，避免绕过授权校验、课时扣减与审批联动。</p>
 */
@Tag(name = "管理端-请假记录")
@RestController
@RequestMapping("/api/admin/leave")
public class LeaveRequestController extends BaseCrudController<LeaveRequest> {

    public LeaveRequestController(LeaveRequestMapper mapper) {
        super(mapper, LeaveRequest.class);
    }

    /**
     * 请假状态统计（按状态计数，供页面顶部指标卡使用）。
     *
     * <p>指标卡不能基于「当前页数据」做前端过滤，否则翻页后数字会变；这里用三条 count 查询返回全量口径。</p>
     *
     * @return PENDING / APPROVED / REJECTED / PENDING_MAKEUP 四个数量与 total
     */
    @Operation(summary = "请假状态统计")
    @GetMapping("/stats")
    public ApiResponse<Map<String, Long>> stats() {
        Map<String, Long> data = new LinkedHashMap<>();
        long total = 0;
        for (String status : List.of("PENDING", "APPROVED", "REJECTED")) {
            long count = mapper.selectCount(new LambdaQueryWrapper<LeaveRequest>()
                    .eq(LeaveRequest::getStatus, status));
            data.put(status, count);
            total += count;
        }
        // 待补课：已审批通过且补课状态仍为 PENDING 的请假单
        data.put("PENDING_MAKEUP", mapper.selectCount(new LambdaQueryWrapper<LeaveRequest>()
                .eq(LeaveRequest::getMakeupStatus, "PENDING")));
        data.put("TOTAL", total);
        return ApiResponse.ok(data);
    }

    /**
     * 关闭通用写通道：请假审批必须走审批接口（联动出勤记录与课时扣减）。
     *
     * <p>保留 page / list / {id} 查询；写入请走 {@code /api/admin/workflow/leaves/{id}/approve}。</p>
     *
     * @return false，POST / PUT / DELETE 统一返回 405
     */
    @Override
    protected boolean writeChannelsEnabled() {
        return false;
    }
}
