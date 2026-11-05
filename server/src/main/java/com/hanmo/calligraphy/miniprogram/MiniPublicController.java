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

    @Operation(summary = "首页聚合数据")
    @GetMapping("/home")
    public ApiResponse<Map<String, Object>> home() {
        return ApiResponse.ok(publicService.home());
    }

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

    @Operation(summary = "课程列表")
    @GetMapping("/courses")
    public ApiResponse<List<Map<String, Object>>> courses(@RequestParam(required = false) Long categoryId,
                                                          @RequestParam(required = false) Long stageId,
                                                          @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(publicService.courses(categoryId, stageId, keyword));
    }

    @Operation(summary = "课程详情")
    @GetMapping("/courses/{id}")
    public ApiResponse<Map<String, Object>> courseDetail(@PathVariable Long id) {
        return ApiResponse.ok(publicService.courseDetail(id));
    }

    @Operation(summary = "师资列表")
    @GetMapping("/teachers")
    public ApiResponse<List<Map<String, Object>>> teachers() {
        return ApiResponse.ok(publicService.teachers());
    }

    @Operation(summary = "教师详情")
    @GetMapping("/teachers/{id}")
    public ApiResponse<Map<String, Object>> teacherDetail(@PathVariable Long id) {
        return ApiResponse.ok(publicService.teacherDetail(id));
    }

    @Operation(summary = "作品列表")
    @GetMapping("/works")
    public ApiResponse<List<Map<String, Object>>> works(@RequestParam(required = false) String type,
                                                        @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(publicService.works(type, keyword));
    }

    @Operation(summary = "作品详情")
    @GetMapping("/works/{id}")
    public ApiResponse<Map<String, Object>> workDetail(@PathVariable Long id) {
        return ApiResponse.ok(publicService.workDetail(id));
    }

    @Operation(summary = "作品点赞")
    @PostMapping("/works/{id}/like")
    public ApiResponse<Map<String, Object>> like(@PathVariable Long id) {
        return ApiResponse.ok(publicService.like(id));
    }

    @Operation(summary = "公告列表")
    @GetMapping("/notices")
    public ApiResponse<List<Map<String, Object>>> notices() {
        return ApiResponse.ok(publicService.notices());
    }

    @Operation(summary = "试听预约提交")
    @PostMapping("/trial")
    public ApiResponse<Map<String, Object>> trial(@Valid @RequestBody TrialRequest request) {
        return ApiResponse.ok(publicService.trial(request));
    }
}
