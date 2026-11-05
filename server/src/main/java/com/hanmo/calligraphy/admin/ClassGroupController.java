package com.hanmo.calligraphy.admin;

import com.hanmo.calligraphy.common.BaseCrudController;
import com.hanmo.calligraphy.domain.entity.ClassGroup;
import com.hanmo.calligraphy.domain.mapper.ClassGroupMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 班级管理：class_group 表（课程、任课教师、教室、星期与上下课时间、容量、状态）的增删改查。
 *
 * <p>业务模块：班级与排课；接口前缀 {@code /api/admin/classes}。同前缀下的学员名单
 * （/{classId}/students）由 admin.lesson.ClassStudentAdminController 提供；课次走另一个前缀
 * /api/admin/lessons（LessonController）。</p>
 * <p>后台页面：管理端「班级与排课」(/classes，Classes.vue)。</p>
 * <p>权限：该前缀不在「写操作仅 ADMIN」清单内，GET/POST/PUT/DELETE 均允许 ADMIN 与 ACADEMIC。</p>
 * <p>通道：继承 {@link BaseCrudController}，page/list/{id}/create/update/delete 六个通道全部开放；
 * 分页 keyword 命中 name 列。删除为逻辑删除（class_group.deleted，application.yml 全局 logic-delete-field 生效），
 * 但不会级联处理已有课次与班级学员关系，也不阻止删除仍有学员的班级。</p>
 */
@Tag(name = "管理端-班级")
@RestController
@RequestMapping("/api/admin/classes")
public class ClassGroupController extends BaseCrudController<ClassGroup> {
    public ClassGroupController(ClassGroupMapper mapper) {
        super(mapper, ClassGroup.class);
    }
}
