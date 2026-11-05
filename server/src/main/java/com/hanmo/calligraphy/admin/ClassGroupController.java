package com.hanmo.calligraphy.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hanmo.calligraphy.common.ApiResponse;
import com.hanmo.calligraphy.common.BaseCrudController;
import com.hanmo.calligraphy.common.PageResult;
import com.hanmo.calligraphy.domain.entity.ClassGroup;
import com.hanmo.calligraphy.domain.entity.ClassStudent;
import com.hanmo.calligraphy.domain.mapper.ClassGroupMapper;
import com.hanmo.calligraphy.domain.mapper.ClassStudentMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 班级管理：class_group 表（课程、任课教师、教室、星期与上下课时间、容量、状态）的增删改查。
 *
 * <p>业务模块：班级与排课；接口前缀 {@code /api/admin/classes}。同前缀下的学员名单
 * （/{classId}/students）由 admin.lesson.ClassStudentAdminController 提供；课次走另一个前缀
 * /api/admin/lessons（LessonController）。</p>
 * <p>后台页面：管理端「班级与排课」(/classes，Classes.vue)。</p>
 * <p>权限：该前缀不在「写操作仅 ADMIN」清单内，GET/POST/PUT/DELETE 均允许 ADMIN 与 ACADEMIC
 * （班级维护属于教务日常职责，因此写通道保持开放）。</p>
 * <p>通道：继承 {@link BaseCrudController}，page/list/{id}/create/update/delete 六个通道全部开放；
 * 分页 keyword 命中 name 列。删除为逻辑删除（class_group.deleted），但不会级联处理已有课次与班级学员关系，
 * 也不阻止删除仍有学员的班级。</p>
 * <p>分页接口在基类基础上额外聚合「在读人数」studentCount，避免前端逐班请求名单统计。</p>
 */
@Tag(name = "管理端-班级")
@RestController
@RequestMapping("/api/admin/classes")
public class ClassGroupController extends BaseCrudController<ClassGroup> {

    private final ClassStudentMapper classStudentMapper;

    public ClassGroupController(ClassGroupMapper mapper, ClassStudentMapper classStudentMapper) {
        super(mapper, ClassGroup.class);
        this.classStudentMapper = classStudentMapper;
    }

    /**
     * 班级分页（附带在读人数）。
     *
     * <p>先走基类分页拿到当前页班级，再按这批班级 ID 一次性查询 class_student，
     * 在内存里按 classId 汇总 ACTIVE 人数后回填，避免逐班查询造成 N+1。</p>
     *
     * @param current 页码，从 1 开始
     * @param size    每页条数，上限 200
     * @param keyword 可选关键字，命中班级名称
     * @return 分页结果，records 中每个班级带 studentCount
     */
    @Operation(summary = "班级分页（含在读人数）")
    @Override
    @GetMapping("/page")
    public ApiResponse<PageResult<ClassGroup>> page(@RequestParam(defaultValue = "1") long current,
                                                    @RequestParam(defaultValue = "10") long size,
                                                    @RequestParam(required = false) String keyword) {
        ApiResponse<PageResult<ClassGroup>> response = super.page(current, size, keyword);
        PageResult<ClassGroup> data = response.getData();
        List<ClassGroup> records = data == null ? null : data.getRecords();
        if (records == null || records.isEmpty()) {
            return response;
        }
        List<Long> classIds = records.stream().map(ClassGroup::getId).toList();
        // 只统计 ACTIVE 的在读关系，退班/移出的记录不计入人数
        List<ClassStudent> links = classStudentMapper.selectList(new LambdaQueryWrapper<ClassStudent>()
                .in(ClassStudent::getClassId, classIds)
                .eq(ClassStudent::getStatus, "ACTIVE"));
        Map<Long, Integer> counts = new HashMap<>();
        for (ClassStudent link : links) {
            counts.merge(link.getClassId(), 1, Integer::sum);
        }
        for (ClassGroup group : records) {
            group.setStudentCount(counts.getOrDefault(group.getId(), 0));
        }
        return response;
    }
}
