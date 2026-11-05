package com.hanmo.calligraphy.admin.student;

import com.hanmo.calligraphy.admin.student.StudentAdminDtos.AdjustHoursRequest;
import com.hanmo.calligraphy.admin.student.StudentAdminDtos.CreateStudentRequest;
import com.hanmo.calligraphy.admin.student.StudentAdminDtos.StudentDetail;
import com.hanmo.calligraphy.admin.student.StudentAdminDtos.StudentOption;
import com.hanmo.calligraphy.admin.student.StudentAdminDtos.StudentRow;
import com.hanmo.calligraphy.admin.student.StudentAdminDtos.UpdateStudentRequest;
import com.hanmo.calligraphy.common.ApiResponse;
import com.hanmo.calligraphy.common.PageResult;
import com.hanmo.calligraphy.security.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * 学员管理接口：分页、档案详情、新建、编辑、调整课时、下拉选项。
 *
 * <p>路由前缀 /api/admin/students。逻辑全部在 StudentAdminService，本类只做参数绑定与包装；
 * 写操作透传当前操作人用于审计。</p>
 * 落库表：student、parent_student、class_student、hour_account、hour_ledger、attendance、sys_audit_log（经 service 落库）
 */
@Tag(name = "管理端-学员管理")
@RestController
@RequestMapping("/api/admin/students")
@RequiredArgsConstructor
public class StudentAdminController {

    private final StudentAdminService service;

    /**
     * 学员分页。
     *
     * <p>只读；keyword 匹配姓名/编号，classId 按班级 ACTIVE 名单过滤。</p>
     */
    @Operation(summary = "学员分页")
    @GetMapping("/page")
    public ApiResponse<PageResult<StudentRow>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long classId) {
        return ApiResponse.ok(service.page(current, size, keyword, status, classId));
    }

    /**
     * 学员档案详情（含课时账户、家长、班级、最近流水与上课记录）。
     *
     * <p>只读。</p>
     *
     * @throws BusinessException 404 学员不存在
     * @return 学员档案聚合对象（含家长、班级、流水与上课记录）
     */
    @Operation(summary = "学员档案详情")
    @GetMapping("/{id}")
    public ApiResponse<StudentDetail> detail(@PathVariable Long id) {
        return ApiResponse.ok(service.detail(id));
    }

    /**
     * 新建学员。
     *
     * <p>副作用：写 student 与审计日志；不建课时账户、不入班。</p>
     *
     * @throws BusinessException 编号生成失败（500）
     */
    @Operation(summary = "新建学员")
    @PostMapping
    public ApiResponse<StudentRow> create(@Valid @RequestBody CreateStudentRequest request,
                                          @AuthenticationPrincipal LoginUser operator) {
        return ApiResponse.ok(service.create(request, operator));
    }

    /**
     * 编辑学员档案（局部更新，null 字段不改）。
     *
     * <p>副作用：更新 student 与审计日志。</p>
     *
     * @throws BusinessException 404 学员不存在
     */
    @Operation(summary = "编辑学员")
    @PutMapping("/{id}")
    public ApiResponse<StudentRow> update(@PathVariable Long id,
                                          @RequestBody UpdateStudentRequest request,
                                          @AuthenticationPrincipal LoginUser operator) {
        return ApiResponse.ok(service.update(id, request, operator));
    }

    /**
     * 调整课时（充值/扣课/修正）。
     *
     * <p>副作用：改 hour_account、写 hour_ledger 与审计日志，同一事务；调整后余额不能为负。</p>
     *
     * @return 调整后的剩余课时
     * @throws BusinessException 调整量为 0、类型未知、余额为负（400）
     */
    @Operation(summary = "调整课时")
    @PostMapping("/{id}/adjust-hours")
    public ApiResponse<BigDecimal> adjustHours(@PathVariable Long id,
                                               @Valid @RequestBody AdjustHoursRequest request,
                                               @AuthenticationPrincipal LoginUser operator) {
        return ApiResponse.ok(service.adjustHours(id, request, operator));
    }

    /**
     * 学员下拉（仅 ACTIVE）。
     *
     * <p>只读。</p>
     * @return 学员选项列表
     */
    @Operation(summary = "学员下拉")
    @GetMapping("/options")
    public ApiResponse<List<StudentOption>> options() {
        return ApiResponse.ok(service.options());
    }
}
