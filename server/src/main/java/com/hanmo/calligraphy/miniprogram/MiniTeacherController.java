package com.hanmo.calligraphy.miniprogram;

import com.hanmo.calligraphy.common.ApiResponse;
import com.hanmo.calligraphy.miniprogram.dto.MiniDtos.ApproveLeaveRequest;
import com.hanmo.calligraphy.miniprogram.dto.MiniDtos.RollcallRequest;
import com.hanmo.calligraphy.miniprogram.dto.MiniDtos.WorkCreateRequest;
import com.hanmo.calligraphy.security.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 小程序教师端接口分组：工作台、课程与点名、请假审批、作品上传、教师账号信息。
 *
 * <p>URL 前缀 {@code /api/mini/teacher}，要求登录且角色为教师
 * （类级 {@code @PreAuthorize("hasRole('TEACHER')")}）。</p>
 *
 * <p><b>权限边界</b>：角色通过只代表“是教师”，具体数据边界在 Service：
 * {@code requireTeacher} 要求当前账号关联 {@code teacher_profile} 且档案 ACTIVE；
 * {@code requireMyLesson} 要求课次 {@code teacher_id} 等于本人，否则 403；
 * 班级、学员、待审批请假、作品列表都按本人 ID 过滤。因此教师 A 无法审批教师 B 课次上的请假。</p>
 */
@Tag(name = "小程序-教师端")
@RestController
@RequestMapping("/api/mini/teacher")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TEACHER')")
public class MiniTeacherController {

    private final MiniTeacherService teacherService;

    /**
     * 教师工作台聚合。
     *
     * <p>返回教师档案、今日/本周课程、带班数、学员数、待审批请假数与待审核作品数。</p>
     *
     * @param user 当前登录教师
     * @return teacher/today/todayLessons/weekLessons/classCount/studentCount/pendingLeaves/pendingWorks/weekStart
     * @throws BusinessException 403 未关联教师档案或档案已停用
     */
    @Operation(summary = "教师工作台聚合")
    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> overview(@AuthenticationPrincipal LoginUser user) {
        return ApiResponse.ok(teacherService.home(user));
    }

    /**
     * 某日课程（含点名状态与待审批请假标记）。
     *
     * @param user 当前登录教师
     * @param date 日期，缺省今天
     * @return 课次数组，仅本人课次
     * @throws BusinessException 403 未关联教师档案
     */
    @Operation(summary = "某日课程")
    @GetMapping("/lessons")
    public ApiResponse<List<Map<String, Object>>> lessons(
            @AuthenticationPrincipal LoginUser user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(teacherService.lessons(user, date));
    }

    /**
     * 本周课程（周一至周日）。
     *
     * @param user 当前登录教师
     * @param week 该周任意日期，缺省本周
     * @return 课次数组，仅本人课次
     * @throws BusinessException 403 未关联教师档案
     */
    @Operation(summary = "本周课程")
    @GetMapping("/lesson-week")
    public ApiResponse<List<Map<String, Object>>> weekLessons(
            @AuthenticationPrincipal LoginUser user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate week) {
        return ApiResponse.ok(teacherService.weekLessons(user, week));
    }

    /**
     * 课次详情与点名名单。
     *
     * @param user     当前登录教师
     * @param lessonId 课次 ID，必须是本人课次
     * @return lesson/classInfo/students/summary/pendingLeaves
     * @throws BusinessException 403 不是本人课次；404 课次不存在
     */
    @Operation(summary = "课次详情与点名名单")
    @GetMapping("/lessons/{id}")
    public ApiResponse<Map<String, Object>> lessonDetail(@AuthenticationPrincipal LoginUser user,
                                                         @PathVariable Long id) {
        return ApiResponse.ok(teacherService.lessonDetail(user, id));
    }

    /**
     * 提交点名并自动消课。
     *
     * <p>出勤/缺勤各消 1 课时；请假按已通过请假的 {@code deduct_hours} 处理，未审批请假按扣课。
     * 重复提交只按新旧 consumedHours 差额调整，不会重复扣课；课次标记 DONE。</p>
     *
     * <p>副作用：写/更新 {@code attendance}、按差额调整 {@code hour_account} 并写 {@code hour_ledger}、
     * 课次置 DONE、写审计日志。</p>
     *
     * @param user     当前登录教师
     * @param lessonId 课次 ID，必须是本人课次
     * @param request  点名明细
     * @return summary（人数统计与 done 标记）+ items + message
     * @throws BusinessException 400 名单为空/重复/状态非法/学员不在班级；403 非本人课次；404 课次不存在
     */
    @Operation(summary = "点名并自动消课")
    @PostMapping("/lessons/{id}/rollcall")
    public ApiResponse<Map<String, Object>> rollcall(@AuthenticationPrincipal LoginUser user,
                                                     @PathVariable Long id,
                                                     @RequestBody RollcallRequest request) {
        return ApiResponse.ok(teacherService.rollcall(user, id, request));
    }

    /**
     * 待审批请假。
     *
     * <p>只返回本人课次上的 PENDING 请假，按提交时间倒序。</p>
     *
     * @param user 当前登录教师
     * @return 请假数组
     */
    @Operation(summary = "待审批请假")
    @GetMapping("/leaves/pending")
    public ApiResponse<List<Map<String, Object>>> pendingLeaves(@AuthenticationPrincipal LoginUser user) {
        return ApiResponse.ok(teacherService.pendingLeaves(user));
    }

    /**
     * 已处理请假。
     *
     * @param user   当前登录教师
     * @param status 状态过滤，可选；不传时默认查非 PENDING 的记录
     * @return 请假数组，按 id 倒序
     */
    @Operation(summary = "已处理请假")
    @GetMapping("/leaves/history")
    public ApiResponse<List<Map<String, Object>>> leaveHistory(@AuthenticationPrincipal LoginUser user,
                                                               @RequestParam(required = false) String status) {
        return ApiResponse.ok(teacherService.leaveHistory(user, status));
    }

    /**
     * 审批请假。
     *
     * <p>通过：写/更新 {@code attendance}（LEAVE），按 {@code deduct_hours} 补扣或返还课时，
     * 写 {@code hour_ledger}；驳回：只改状态与原因，必须填写驳回原因。</p>
     *
     * <p>副作用：更新 {@code leave_request}、可能变更 {@code attendance}/{@code hour_account}、写审计日志。</p>
     *
     * @param user     当前登录教师
     * @param leaveId  请假记录 ID，其课次必须属于本人
     * @param request  approve=true 通过 / false 驳回 + 原因
     * @return 审批后的请假记录
     * @throws BusinessException 400 已处理或缺驳回原因；403 非本人课次；404 请假不存在
     */
    @Operation(summary = "审批请假")
    @PostMapping("/leaves/{id}/approve")
    public ApiResponse<Map<String, Object>> approve(@AuthenticationPrincipal LoginUser user,
                                                    @PathVariable Long id,
                                                    @RequestBody ApproveLeaveRequest request) {
        return ApiResponse.ok(teacherService.approveLeave(user, id, request));
    }

    /**
     * 作品上传选项：可上传学员（含家长授权状态）与书体字典。
     *
     * @param user 当前登录教师
     * @return students（id/name/classId/className/authorized）+ workTypes
     */
    @Operation(summary = "上传作品选项")
    @GetMapping("/work-options")
    public ApiResponse<Map<String, Object>> workOptions(@AuthenticationPrincipal LoginUser user) {
        return ApiResponse.ok(teacherService.workOptions(user));
    }

    /**
     * 我上传的作品及审核状态。
     *
     * @param user 当前登录教师
     * @return 作品数组，按 id 倒序
     */
    @Operation(summary = "我上传的作品")
    @GetMapping("/works")
    public ApiResponse<List<Map<String, Object>>> works(@AuthenticationPrincipal LoginUser user) {
        return ApiResponse.ok(teacherService.works(user));
    }

    /**
     * 上传学员作品（提交审核）。
     *
     * <p>校验：学员在自己所带班级中、已勾选家长授权、后台存在 ACTIVE 的 work_auth；
     * 班级必须是自己所带班级，未指定时自动取该学员所在班。</p>
     *
     * <p>副作用：插入 {@code work}（PENDING，authorized=1），写审计日志。</p>
     *
     * @param user    当前登录教师
     * @param request 作品信息
     * @return id/title/status/message
     * @throws BusinessException 400 未确认授权或无有效授权；403 学员不在自己班或班级越权；
     *         404 学员不存在
     */
    @Operation(summary = "上传学员作品（提交审核）")
    @PostMapping("/works")
    public ApiResponse<Map<String, Object>> createWork(@AuthenticationPrincipal LoginUser user,
                                                       @Valid @RequestBody WorkCreateRequest request) {
        return ApiResponse.ok(teacherService.createWork(user, request));
    }

    /**
     * 教师账号信息。
     *
     * @param user 当前登录教师
     * @return teacher/identity/classCount/studentCount/classes
     * @throws BusinessException 403 未关联教师档案
     */
    @Operation(summary = "教师账号信息")
    @GetMapping("/account")
    public ApiResponse<Map<String, Object>> account(@AuthenticationPrincipal LoginUser user) {
        return ApiResponse.ok(teacherService.account(user));
    }
}
