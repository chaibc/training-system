package com.hanmo.calligraphy.miniprogram;

import com.hanmo.calligraphy.common.ApiResponse;
import com.hanmo.calligraphy.miniprogram.dto.MiniDtos.BindingRequest;
import com.hanmo.calligraphy.miniprogram.dto.MiniDtos.ChildProfileRequest;
import com.hanmo.calligraphy.miniprogram.dto.MiniDtos.CommentRequest;
import com.hanmo.calligraphy.miniprogram.dto.MiniDtos.EnrollmentCreateRequest;
import com.hanmo.calligraphy.miniprogram.dto.MiniDtos.LeaveCreateRequest;
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
 * 小程序家长端接口分组：工作台、子女档案、绑定关系、课表/课次、请假、报名、课时、
 * 上课记录、成长相册与作品留言。
 *
 * <p>URL 前缀 {@code /api/mini/parent}，要求登录且角色为家长
 * （类级 {@code @PreAuthorize("hasRole('PARENT')")}，JWT 过滤器按角色写入 {@code ROLE_PARENT}）。</p>
 *
 * <p><b>数据范围不在这里控制</b>：凡是带 {@code studentId} 的接口，Service 内部都会调用
 * {@code requireBinding/requireChild} 校验 {@code parent_student.status=ACTIVE}，
 * 未绑定或绑定待审核一律 403，仅凭“知道学员 ID”无法越权读取。</p>
 *
 * <p>其余校验（课次是否属于该学员、报名能否取消、请假截止时间等）也在 Service 中完成，
 * 本类只负责接收参数与包装统一响应。写操作大多会写审计日志（作品留言除外）。</p>
 */
@Tag(name = "小程序-家长端")
@RestController
@RequestMapping("/api/mini/parent")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PARENT')")
public class MiniParentController {

    private final MiniParentService parentService;

    /**
     * 家长工作台聚合。
     *
     * <p>一次返回家长称呼、子女卡片、最近公告、待审核绑定数与待审批请假总数。</p>
     *
     * @param user 当前登录家长（JWT 解析）
     * @return parentName/children/notices/pendingBindings/pendingLeaves
     */
    @Operation(summary = "家长工作台聚合")
    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> overview(@AuthenticationPrincipal LoginUser user) {
        return ApiResponse.ok(parentService.overview(user));
    }

    /**
     * 已绑定子女列表。
     *
     * <p>只返回 {@code status=ACTIVE} 的绑定；已提交解绑但教务未处理的仍算 ACTIVE，
     * 审核期间家长仍可查看。</p>
     *
     * @param user 当前登录家长
     * @return 子女卡片数组，字段见 MiniParentService#childSummary
     */
    @Operation(summary = "已绑定子女列表")
    @GetMapping("/children")
    public ApiResponse<List<Map<String, Object>>> children(@AuthenticationPrincipal LoginUser user) {
        return ApiResponse.ok(parentService.children(user.getId()));
    }

    /**
     * 学员档案详情：基础信息 + 班级 + 课时账户 + 最近 3 条出勤。
     *
     * @param user      当前登录家长
     * @param studentId 学员 ID，必须已绑定
     * @return 档案 Map
     * @throws BusinessException 403 未绑定或绑定待审核；404 学员不存在
     */
    @Operation(summary = "学员档案详情")
    @GetMapping("/children/{studentId}")
    public ApiResponse<Map<String, Object>> childDetail(@AuthenticationPrincipal LoginUser user,
                                                        @PathVariable Long studentId) {
        return ApiResponse.ok(parentService.childDetail(user, studentId));
    }

    /**
     * 修改学员基础资料（仅性别、出生日期、学校、年级）。
     *
     * <p>副作用：更新 {@code student} 并写审计日志；姓名、学号等关键信息不在此开放。</p>
     *
     * @param user      当前登录家长
     * @param studentId 学员 ID，必须已绑定
     * @param request   待修改字段，null 表示不修改
     * @return 修改后的档案详情（同 childDetail）
     * @throws BusinessException 403 未绑定；404 学员不存在
     */
    @Operation(summary = "修改学员基础资料")
    @PutMapping("/children/{studentId}")
    public ApiResponse<Map<String, Object>> updateChild(@AuthenticationPrincipal LoginUser user,
                                                        @PathVariable Long studentId,
                                                        @RequestBody ChildProfileRequest request) {
        return ApiResponse.ok(parentService.updateChild(user, studentId, request));
    }

    /**
     * 当前家长的全部绑定关系（含待审核、已驳回、已解绑）。
     *
     * <p>解绑待审核在库中仍是 ACTIVE + source=UNBIND，这里映射为 {@code UNBIND_PENDING}。</p>
     *
     * @param user 当前登录家长
     * @return 绑定记录数组
     */
    @Operation(summary = "我的绑定关系")
    @GetMapping("/bindings")
    public ApiResponse<List<Map<String, Object>>> bindings(@AuthenticationPrincipal LoginUser user) {
        return ApiResponse.ok(parentService.bindings(user));
    }

    /**
     * 提交学员绑定申请。
     *
     * <p>两种模式：绑定已有学员（学员编号 + 主监护人手机号后四位）或新建学员并绑定。
     * 首位监护人自动为主联系人，一个学员最多 2 位 ACTIVE 监护人。</p>
     *
     * <p>副作用：可能新建 {@code student}；写入/复用 {@code parent_student}（状态 PENDING）；
     * 写审计日志。审核由管理端完成。</p>
     *
     * @param user    当前登录家长
     * @param request 绑定参数
     * @return studentId/studentName/studentNo/status=PENDING/statusText/message
     * @throws BusinessException 400 参数缺失、编号不匹配、后四位不正确、已绑定或监护人已满；
     *         404 学员不存在
     */
    @Operation(summary = "提交学员绑定申请")
    @PostMapping("/bindings")
    public ApiResponse<Map<String, Object>> createBinding(@AuthenticationPrincipal LoginUser user,
                                                          @RequestBody BindingRequest request) {
        return ApiResponse.ok(parentService.createBinding(user, request));
    }

    /**
     * 申请解绑学员。
     *
     * <p>不会立即解除：只把 {@code parent_student.source} 置为 UNBIND，绑定仍保持 ACTIVE，
     * 家长在教务审核期间仍可查看学员；重复提交返回 400。</p>
     *
     * <p>副作用：更新绑定记录 + 写审计日志。</p>
     *
     * @param user      当前登录家长
     * @param bindingId 绑定记录 ID，必须属于本人
     * @return status=UNBIND_PENDING、statusText
     * @throws BusinessException 400 绑定非 ACTIVE 或已提交过解绑；404 绑定不存在或非本人
     */
    @Operation(summary = "申请解绑学员（需教务审核）")
    @PostMapping("/bindings/{id}/unbind")
    public ApiResponse<Map<String, Object>> unbind(@AuthenticationPrincipal LoginUser user,
                                                   @PathVariable Long id) {
        return ApiResponse.ok(parentService.requestUnbind(user, id));
    }

    /**
     * 某周课表。
     *
     * @param user      当前登录家长
     * @param studentId 学员 ID，必须已绑定
     * @param week      该周任意日期，缺省当周
     * @return child/hours/weekStart/weekEnd/lessons/today
     * @throws BusinessException 403 未绑定该学员
     */
    @Operation(summary = "我的课表")
    @GetMapping("/schedule")
    public ApiResponse<Map<String, Object>> schedule(@AuthenticationPrincipal LoginUser user,
                                                     @RequestParam Long studentId,
                                                     @RequestParam(required = false)
                                                     @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate week) {
        return ApiResponse.ok(parentService.schedule(user, studentId, week));
    }

    /**
     * 课次详情：课次、该学员出勤与请假、能否请假及提示。
     *
     * @param user      当前登录家长
     * @param studentId 学员 ID，必须已绑定
     * @param lessonId  课次 ID
     * @return 课次 Map 追加 child/leave/canLeave/leaveHint
     * @throws BusinessException 403 课次不属于该学员；404 课次不存在
     */
    @Operation(summary = "课次详情")
    @GetMapping("/lessons/{lessonId}")
    public ApiResponse<Map<String, Object>> lessonDetail(@AuthenticationPrincipal LoginUser user,
                                                         @PathVariable Long lessonId,
                                                         @RequestParam Long studentId) {
        return ApiResponse.ok(parentService.lessonDetail(user, studentId, lessonId));
    }

    /**
     * 请假申请选项：可请假课次、原因字典、截止小时数与规则文案。
     *
     * <p>可请假课次 = 该学员在读班级中 SCHEDULED、尚未开始、且没有 PENDING/APPROVED 请假的课次。</p>
     *
     * @param user      当前登录家长
     * @param studentId 学员 ID，必须已绑定
     * @return child/lessons/reasons/cutoffHours/ruleText
     * @throws BusinessException 403 未绑定该学员
     */
    @Operation(summary = "请假申请选项")
    @GetMapping("/leave-options")
    public ApiResponse<Map<String, Object>> leaveOptions(@AuthenticationPrincipal LoginUser user,
                                                         @RequestParam Long studentId) {
        return ApiResponse.ok(parentService.leaveOptions(user, studentId));
    }

    /**
     * 提交请假申请。
     *
     * <p>规则：课次必须属于该学员、状态 SCHEDULED、尚未开始，且同一课次只能有一条有效申请；
     * 距开课不足 {@code leave_cutoff_hours}（默认 4）小时时按扣 1 课时处理。</p>
     *
     * <p>副作用：插入 {@code leave_request}（PENDING，含 deductHours），写审计日志。</p>
     *
     * @param user    当前登录家长
     * @param request 学员、课次、原因
     * @return id/leaveNo/status/statusText/deductHours/deductText/lesson
     * @throws BusinessException 400 课次不属于该学员、不可请假、已开始或已有有效申请；404 课次不存在
     */
    @Operation(summary = "提交请假申请")
    @PostMapping("/leaves")
    public ApiResponse<Map<String, Object>> createLeave(@AuthenticationPrincipal LoginUser user,
                                                        @Valid @RequestBody LeaveCreateRequest request) {
        return ApiResponse.ok(parentService.createLeave(user, request));
    }

    /**
     * 请假记录。
     *
     * @param user      当前登录家长
     * @param studentId 学员 ID，必须已绑定
     * @param status    状态过滤，可选；传 UNBIND 时返回空数组（防御性处理）
     * @return 请假记录数组，按 id 倒序
     * @throws BusinessException 403 未绑定该学员
     */
    @Operation(summary = "请假记录")
    @GetMapping("/leave-records")
    public ApiResponse<List<Map<String, Object>>> leaveRecords(@AuthenticationPrincipal LoginUser user,
                                                               @RequestParam Long studentId,
                                                               @RequestParam(required = false) String status) {
        return ApiResponse.ok(parentService.leaveRecords(user, studentId, status));
    }

    /**
     * 请假通过后申请补课。
     *
     * <p>受 {@code business_rule.makeup_enabled} 开关控制；已申请/已安排补课的不能重复提交。</p>
     *
     * <p>副作用：把 {@code leave_request.makeup_status} 置为 PENDING，写审计日志。</p>
     *
     * @param user    当前登录家长
     * @param leaveId 请假记录 ID，必须属于本人子女
     * @return 更新后的请假记录
     * @throws BusinessException 400 请假未通过、未开放补课或已申请；404 请假不存在
     */
    @Operation(summary = "申请补课")
    @PostMapping("/leaves/{id}/makeup")
    public ApiResponse<Map<String, Object>> makeup(@AuthenticationPrincipal LoginUser user,
                                                   @PathVariable Long id) {
        return ApiResponse.ok(parentService.applyMakeup(user, id));
    }

    /**
     * 报名选项：上架课程 + 可报名班级（含已报人数、剩余名额、最低价）。
     *
     * @param user      当前登录家长
     * @param studentId 学员 ID，必须已绑定
     * @return child/courses/classes
     * @throws BusinessException 403 未绑定该学员
     */
    @Operation(summary = "报名选项（课程与班级）")
    @GetMapping("/enroll/options")
    public ApiResponse<Map<String, Object>> enrollmentOptions(@AuthenticationPrincipal LoginUser user,
                                                              @RequestParam Long studentId) {
        return ApiResponse.ok(parentService.enrollmentOptions(user, studentId));
    }

    /**
     * 该学员的报名记录。
     *
     * @param user      当前登录家长
     * @param studentId 学员 ID，必须已绑定
     * @return 报名记录数组，按 id 倒序
     * @throws BusinessException 403 未绑定该学员
     */
    @Operation(summary = "报名记录")
    @GetMapping("/enrollments")
    public ApiResponse<List<Map<String, Object>>> enrollments(@AuthenticationPrincipal LoginUser user,
                                                              @RequestParam Long studentId) {
        return ApiResponse.ok(parentService.enrollments(user, studentId));
    }

    /**
     * 提交报名申请。
     *
     * <p>校验课程上架、班级与课程匹配、班级 ACTIVE、名额未满、同课程无进行中的报名。</p>
     *
     * <p>副作用：插入 {@code enrollment}（PENDING_CONFIRM，金额取该课程最低价，source=MINI），
     * 写审计日志；后续由教务确认并收费。</p>
     *
     * @param user    当前登录家长
     * @param request 学员、课程、班级（班级可空）
     * @return 报名记录追加 message
     * @throws BusinessException 400 课程/班级不合法、名额已满或重复报名
     */
    @Operation(summary = "提交报名申请")
    @PostMapping("/enrollments")
    public ApiResponse<Map<String, Object>> createEnrollment(@AuthenticationPrincipal LoginUser user,
                                                             @Valid @RequestBody EnrollmentCreateRequest request) {
        return ApiResponse.ok(parentService.createEnrollment(user, request));
    }

    /**
     * 家长取消尚未确认的报名。
     *
     * <p>仅 PENDING_CONFIRM / PENDING_PAYMENT 可取消；只能取消自己提交的报名。</p>
     *
     * <p>副作用：状态置为 CANCELLED，写审计日志。</p>
     *
     * @param user 当前登录家长
     * @param id   报名记录 ID
     * @return 取消后的报名记录
     * @throws BusinessException 400 当前状态不允许取消；404 报名不存在或非本人
     */
    @Operation(summary = "家长取消未确认报名")
    @PostMapping("/enrollments/{id}/cancel")
    public ApiResponse<Map<String, Object>> cancelEnrollment(@AuthenticationPrincipal LoginUser user,
                                                             @PathVariable Long id) {
        return ApiResponse.ok(parentService.cancelEnrollment(user, id));
    }

    /**
     * 课时账户与流水。
     *
     * @param user      当前登录家长
     * @param studentId 学员 ID，必须已绑定
     * @return child/account（总/已消/剩余/有效期/续费预警）/ledger/renewalHint
     * @throws BusinessException 403 未绑定该学员
     */
    @Operation(summary = "课时账户与流水")
    @GetMapping("/hours")
    public ApiResponse<Map<String, Object>> hours(@AuthenticationPrincipal LoginUser user,
                                                  @RequestParam Long studentId) {
        return ApiResponse.ok(parentService.hours(user, studentId));
    }

    /**
     * 上课记录。
     *
     * @param user      当前登录家长
     * @param studentId 学员 ID，必须已绑定
     * @param status    出勤状态过滤，可选
     * @return child/records
     * @throws BusinessException 403 未绑定该学员
     */
    @Operation(summary = "上课记录")
    @GetMapping("/attendance")
    public ApiResponse<Map<String, Object>> attendance(@AuthenticationPrincipal LoginUser user,
                                                       @RequestParam Long studentId,
                                                       @RequestParam(required = false) String status) {
        return ApiResponse.ok(parentService.attendance(user, studentId, status));
    }

    /**
     * 成长相册：该学员已展示且家长已授权的作品，按月分组。
     *
     * @param user      当前登录家长
     * @param studentId 学员 ID，必须已绑定
     * @return child/timeline（month、works、teacherComment）/workCount
     * @throws BusinessException 403 未绑定该学员
     */
    @Operation(summary = "成长相册")
    @GetMapping("/growth")
    public ApiResponse<Map<String, Object>> growth(@AuthenticationPrincipal LoginUser user,
                                                   @RequestParam Long studentId) {
        return ApiResponse.ok(parentService.growth(user, studentId));
    }

    /**
     * 账号与绑定信息：手机号脱敏、微信绑定状态、身份、绑定关系与子女列表。
     *
     * @param user 当前登录家长
     * @return name/phone/avatar/wechatNickname/status/statusText/wechatBound/identity/bindings/children
     * @throws BusinessException 401 账号不存在
     */
    @Operation(summary = "账号与绑定信息")
    @GetMapping("/account")
    public ApiResponse<Map<String, Object>> account(@AuthenticationPrincipal LoginUser user) {
        return ApiResponse.ok(parentService.account(user));
    }

    /**
     * 提交作品留言。
     *
     * <p>作品必须是已展示状态；留言进入 PENDING，教务审核通过后才在公开页展示。
     * 受 {@code business_rule.comment_enabled} 开关控制，正文不超过 300 字。</p>
     *
     * <p>副作用：插入 {@code work_comment}（PENDING）。</p>
     *
     * @param user    当前登录家长
     * @param workId  作品 ID
     * @param request 留言正文
     * @return id/status=PENDING/message
     * @throws BusinessException 400 未开放留言、内容为空或超长；404 作品不存在或未展示
     */
    @Operation(summary = "作品留言（审核后展示）")
    @PostMapping("/works/{workId}/comments")
    public ApiResponse<Map<String, Object>> comment(@AuthenticationPrincipal LoginUser user,
                                                    @PathVariable Long workId,
                                                    @Valid @RequestBody CommentRequest request) {
        return ApiResponse.ok(parentService.comment(user, workId, request.content()));
    }
}
