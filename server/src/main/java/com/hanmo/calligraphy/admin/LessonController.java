package com.hanmo.calligraphy.admin;

import com.hanmo.calligraphy.admin.lesson.LessonAdminDtos.LessonRequest;
import com.hanmo.calligraphy.admin.lesson.LessonAdminService;
import com.hanmo.calligraphy.common.ApiResponse;
import com.hanmo.calligraphy.common.PageResult;
import com.hanmo.calligraphy.domain.entity.Lesson;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 课次管理：lesson 表（班级、课次序号、日期与起止时间、任课教师、教室、状态）的增删改查。
 *
 * <p>业务模块：班级与排课；接口前缀 {@code /api/admin/lessons}。</p>
 * <p>后台页面：管理端「班级与排课」(/classes，Classes.vue) 的课次区块；点名页也用它取课次下拉。</p>
 * <p>权限：该前缀不在 SecurityConfig 的「写操作仅 ADMIN」清单内，GET/POST/PUT/DELETE 均允许 ADMIN 与 ACADEMIC。</p>
 * <p>与通用 CRUD 的差异：不复用 BaseCrudController，新增与修改统一走 LessonAdminService.save，
 * 依次校验班级存在、结束时间晚于开始时间、教师/教室存在、同一班级课次序号唯一，
 * 再校验班级、教师、教室、学员（跨班时间重叠）四维冲突；删除前检查是否已产生点名记录。</p>
 * <p>状态取值：SCHEDULED（默认）、DONE（点名后由 workflow 置位）、CANCELLED（取消；冲突校验时已取消的课次会被排除）。</p>
 */
@Tag(name = "管理端-课次")
@RestController
@RequestMapping("/api/admin/lessons")
@RequiredArgsConstructor
public class LessonController {

    private final LessonAdminService lessonService;

    /**
     * 课次分页：按 lesson_date 倒序、start_time 正序返回。
     *
     * <p>本方法只接收分页参数，没有 keyword / 班级 / 日期等筛选条件；size 由服务层夹到 [1,500]
     * （比通用 CRUD 的 200 宽，因为排课页一次要拉较多课次）。</p>
     *
     * @param current  页码，从 1 开始，小于 1 按 1 处理
     * @param size     每页条数，缺省 10，上限 500
     * @param dateFrom 可选起始上课日期（含），格式 yyyy-MM-dd
     * @param dateTo   可选结束上课日期（含），格式 yyyy-MM-dd
     * @param classId  可选班级 ID
     * @param status   可选课次状态（SCHEDULED/DONE/CANCELLED）
     */
    @Operation(summary = "课次分页")
    @GetMapping("/page")
    public ApiResponse<PageResult<Lesson>> page(@RequestParam(defaultValue = "1") long current,
                                                @RequestParam(defaultValue = "10") long size,
                                                @RequestParam(required = false)
                                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
                                                @RequestParam(required = false)
                                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
                                                @RequestParam(required = false) Long classId,
                                                @RequestParam(required = false) String status) {
        return ApiResponse.ok(lessonService.page(current, size, dateFrom, dateTo, classId, status));
    }

    /**
     * 课次全量列表（排序同分页），供点名、请假、班级页的课次下拉使用。
     */
    @Operation(summary = "课次列表")
    @GetMapping("/list")
    public ApiResponse<List<Lesson>> list() {
        return ApiResponse.ok(lessonService.list());
    }

    /**
     * 课次详情。
     *
     * @param id 课次 ID
     * @throws com.hanmo.calligraphy.common.BusinessException 课次不存在（404）
     */
    @Operation(summary = "课次详情")
    @GetMapping("/{id}")
    public ApiResponse<Lesson> get(@PathVariable Long id) {
        return ApiResponse.ok(lessonService.get(id));
    }

    /**
     * 新建课次。
     *
     * <p>teacherId / classroomId 为空时回落到班级上配置的教师与教室；status 为空默认 SCHEDULED。
     * 校验顺序见类注释，任一冲突都会整体拒绝（无部分写入）。</p>
     *
     * @param request 课次请求体（classId、lessonNo、lessonDate、startTime、endTime、teacherId、classroomId、status、note）
     * @return 落库后的课次（含自增 id）
     * @throws com.hanmo.calligraphy.common.BusinessException 404 班级不存在；400 时间倒置 / 教师或教室不存在 /
     *         课次序号重复 / 班级、教师、教室或学员时间冲突
     */
    @Operation(summary = "新建课次（自动校验教师/教室/学员冲突）")
    @PostMapping
    public ApiResponse<Lesson> create(@Valid @RequestBody LessonRequest request) {
        return ApiResponse.ok(lessonService.save(null, request));
    }

    /**
     * 编辑课次：校验口径与新建完全一致，比较冲突与课次序号时排除自身 id。
     *
     * @param id      课次 ID
     * @param request 课次请求体（全字段覆盖式保存，未传的字段按请求值落库）
     * @throws com.hanmo.calligraphy.common.BusinessException 404 课次或班级不存在；400 各类校验冲突
     */
    @Operation(summary = "编辑课次（自动校验冲突）")
    @PutMapping("/{id}")
    public ApiResponse<Lesson> update(@PathVariable Long id, @Valid @RequestBody LessonRequest request) {
        return ApiResponse.ok(lessonService.save(id, request));
    }

    /**
     * 删除课次。
     *
     * <p>lesson 表没有 deleted 列，因此是物理删除。已产生点名记录的课次会被拒绝（400），
     * 避免 attendance / hour_ledger 中的课次引用悬空 —— 这类课次应改为把状态置为 CANCELLED。</p>
     *
     * @param id 课次 ID
     * @throws com.hanmo.calligraphy.common.BusinessException 404 课次不存在；400 已有点名 / 消课记录
     */
    @Operation(summary = "删除课次")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        lessonService.delete(id);
        return ApiResponse.ok();
    }
}
