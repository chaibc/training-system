package com.hanmo.calligraphy.miniprogram;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hanmo.calligraphy.common.ApiResponse;
import com.hanmo.calligraphy.domain.entity.CourseCategory;
import com.hanmo.calligraphy.domain.entity.CourseStage;
import com.hanmo.calligraphy.domain.entity.LeaveReason;
import com.hanmo.calligraphy.domain.entity.TrialSlot;
import com.hanmo.calligraphy.domain.mapper.CourseCategoryMapper;
import com.hanmo.calligraphy.domain.mapper.CourseStageMapper;
import com.hanmo.calligraphy.domain.mapper.LeaveReasonMapper;
import com.hanmo.calligraphy.domain.mapper.TrialSlotMapper;
import com.hanmo.calligraphy.miniprogram.dto.MiniDtos.TrialRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 小程序公开接口分组：首页聚合、筛选字典、课程/师资/作品/公告浏览、试听预约与作品点赞。
 *
 * <p>URL 前缀 {@code /api/public}，面向<b>未登录游客</b>（未登录的家长/教师也会调用）。
 * {@link com.hanmo.calligraphy.security.SecurityConfig} 对 {@code /api/public/**} 直接 permitAll，
 * 因此本组接口不做身份校验。</p>
 *
 * <p>不鉴权的原因：这里都是招生展示内容与公开表单，服务端不需要知道“是谁”；
 * 防滥用靠参数校验与业务限制，而不是靠令牌。对外只返回展示字段，不含家长手机号、
 * 家长账号 ID、学号、证件号等隐私；作品仅 {@code DISPLAYED}、教师仅 {@code ACTIVE}、
 * 公告仅 {@code PUBLISHED}、资质仅 {@code visible=1 且 VERIFIED}。</p>
 */
@Tag(name = "小程序-公开内容")
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class MiniPublicController {

    private final MiniPublicService publicService;
    private final CourseCategoryMapper categoryMapper;
    private final CourseStageMapper stageMapper;
    private final TrialSlotMapper trialSlotMapper;
    private final LeaveReasonMapper leaveReasonMapper;

    /**
     * 首页聚合数据：机构信息、推荐课程、师资、作品、公告、资质。
     *
     * <p>一次请求返回首屏所需数据，减少小程序首屏的并发请求数。</p>
     *
     * @return institution（未配置时该键不存在）/courses（推荐最多 3 门）/teachers（最多 4 位）/
     *         works（最多 6 件）/notices（最多 2 条）/qualifications（已核验且展示）
     */
    @Operation(summary = "首页聚合数据")
    @GetMapping("/home")
    public ApiResponse<Map<String, Object>> home() {
        return ApiResponse.ok(publicService.home());
    }

    /**
     * 课程筛选字典：分类、阶段、试听时段、请假原因。
     *
     * <p>四张字典表数据量小且全站共用，直接在这里查 Mapper，未再包一层 Service；
     * 只返回 {@code status=ENABLED} 的记录并按 sort 升序。</p>
     *
     * @return categories / stages / trialSlots / leaveReasons 四个数组
     */
    @Operation(summary = "课程筛选字典")
    @GetMapping("/meta")
    public ApiResponse<Map<String, Object>> meta() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("categories", categoryMapper.selectList(new LambdaQueryWrapper<CourseCategory>()
                .eq(CourseCategory::getStatus, "ENABLED").orderByAsc(CourseCategory::getSort)));
        data.put("stages", stageMapper.selectList(new LambdaQueryWrapper<CourseStage>()
                .eq(CourseStage::getStatus, "ENABLED").orderByAsc(CourseStage::getSort)));
        data.put("trialSlots", trialSlotMapper.selectList(new LambdaQueryWrapper<TrialSlot>()
                .eq(TrialSlot::getStatus, "ENABLED").orderByAsc(TrialSlot::getSort)));
        data.put("leaveReasons", leaveReasonMapper.selectList(new LambdaQueryWrapper<LeaveReason>()
                .eq(LeaveReason::getStatus, "ENABLED").orderByAsc(LeaveReason::getSort)));
        return ApiResponse.ok(data);
    }

    /**
     * 上架课程列表，可按分类、阶段、关键字过滤。
     *
     * @param categoryId 课程分类 ID，可选
     * @param stageId    阶段 ID，可选
     * @param keyword    课程名模糊关键字，可选
     * @return 课程简要信息数组（含分类/阶段名、最低价与计价说明）；无匹配返回空数组
     */
    @Operation(summary = "课程列表")
    @GetMapping("/courses")
    public ApiResponse<List<Map<String, Object>>> courses(@RequestParam(required = false) Long categoryId,
                                                          @RequestParam(required = false) Long stageId,
                                                          @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(publicService.courses(categoryId, stageId, keyword));
    }

    /**
     * 课程详情：课程字段 + 有效价格 + 可见大纲。
     *
     * @param id 课程 ID
     * @return 课程详情 Map（字段见 MiniPublicService#courseMap），追加 prices、outlines
     * @throws BusinessException 404 课程不存在或已下架
     */
    @Operation(summary = "课程详情")
    @GetMapping("/courses/{id}")
    public ApiResponse<Map<String, Object>> courseDetail(@PathVariable Long id) {
        return ApiResponse.ok(publicService.courseDetail(id));
    }

    /**
     * 师资列表（仅 status=ACTIVE，按 sort 升序）。
     *
     * @return 教师展示信息数组：姓名、头衔、教龄、书体、简介、头像等，不含手机号
     */
    @Operation(summary = "师资列表")
    @GetMapping("/teachers")
    public ApiResponse<List<Map<String, Object>>> teachers() {
        return ApiResponse.ok(publicService.teachers());
    }

    /**
     * 教师详情：展示字段 + 该教师所带班级涉及的课程。
     *
     * @param id 教师档案 ID
     * @return 教师详情，额外含 courses 数组（未带班时为空数组）
     * @throws BusinessException 404 教师不存在
     */
    @Operation(summary = "教师详情")
    @GetMapping("/teachers/{id}")
    public ApiResponse<Map<String, Object>> teacherDetail(@PathVariable Long id) {
        return ApiResponse.ok(publicService.teacherDetail(id));
    }

    /**
     * 作品墙列表（仅 status=DISPLAYED，按 id 倒序）。
     *
     * @param type    书体类型 HARD（硬笔）/ SOFT（软笔），可选
     * @param keyword 作品标题模糊关键字，可选
     * @param month   月份 yyyy-MM，可选；按作品创建时间过滤到该自然月
     * @return 作品数组：学员名、班级、教师、标题、书体、内容字、图片、点评、点赞数
     */
    @Operation(summary = "作品列表")
    @GetMapping("/works")
    public ApiResponse<List<Map<String, Object>>> works(@RequestParam(required = false) String type,
                                                        @RequestParam(required = false) String keyword,
                                                        @RequestParam(required = false) String month) {
        return ApiResponse.ok(publicService.works(type, keyword, month));
    }

    /**
     * 作品详情 + 审核通过的留言。
     *
     * @param id 作品 ID
     * @return 作品字段追加 comments 数组（仅 APPROVED）
     * @throws BusinessException 404 作品不存在或未展示
     */
    @Operation(summary = "作品详情")
    @GetMapping("/works/{id}")
    public ApiResponse<Map<String, Object>> workDetail(@PathVariable Long id) {
        return ApiResponse.ok(publicService.workDetail(id));
    }

    /**
     * 作品点赞，点赞数 +1。
     *
     * <p>本期不做防重复点赞，同一设备可多次调用，前端做本地节流。</p>
     *
     * @param id 作品 ID
     * @return 最新 likeCount（按读到的旧值 + 1 返回）
     * @throws BusinessException 404 作品不存在
     */
    @Operation(summary = "作品点赞")
    @PostMapping("/works/{id}/like")
    public ApiResponse<Map<String, Object>> like(@PathVariable Long id) {
        return ApiResponse.ok(publicService.like(id));
    }

    /**
     * 公告列表（仅 PUBLISHED，按发布日期倒序）。
     *
     * @return 公告数组：id、标题、类型、正文、发布日期
     */
    @Operation(summary = "公告列表")
    @GetMapping("/notices")
    public ApiResponse<List<Map<String, Object>>> notices() {
        return ApiResponse.ok(publicService.notices());
    }

    /**
     * 提交试听预约。
     *
     * <p>无需登录：游客填写姓名与手机号即可留资，教务在后台跟进。
     * 手机号需为 11 位（Service 用正则二次校验），课程 ID 若传则必须存在。</p>
     *
     * <p>副作用：插入 {@code trial_booking}，状态 {@code NEW}。</p>
     *
     * @param request 预约表单（学员姓名、手机号必填）
     * @return bookingNo、studentName、courseName、slotName
     * @throws BusinessException 400 手机号格式不正确或意向课程不存在
     */
    @Operation(summary = "试听预约提交")
    @PostMapping("/trial")
    public ApiResponse<Map<String, Object>> trial(@Valid @RequestBody TrialRequest request) {
        return ApiResponse.ok(publicService.trial(request));
    }
}
