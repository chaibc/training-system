package com.hanmo.calligraphy.admin;

import com.hanmo.calligraphy.common.BaseCrudController;
import com.hanmo.calligraphy.domain.entity.Classroom;
import com.hanmo.calligraphy.domain.mapper.ClassroomMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 教室管理：classroom 表（名称、容量、排序、启停）的增删改查。
 *
 * <p>业务模块：基础数据字典；接口前缀 {@code /api/admin/classrooms}。</p>
 * <p>后台页面：管理端「基础数据」(/basic，BasicData.vue) 的教室页签。</p>
 * <p>权限：GET 允许 ADMIN 与 ACADEMIC（排课需要教室下拉）；POST/PUT/DELETE 由 SecurityConfig 限定仅 ADMIN。</p>
 * <p>通道：继承 {@link BaseCrudController}，六个通道全部开放；分页 keyword 命中 name 列；删除为物理删除 ——
 * 不校验是否已被 lesson 引用，历史课次的 classroom_id 会指向不存在的教室。</p>
 */
@Tag(name = "管理端-教室")
@RestController
@RequestMapping("/api/admin/classrooms")
public class ClassroomController extends BaseCrudController<Classroom> {
    public ClassroomController(ClassroomMapper mapper) {
        super(mapper, Classroom.class);
    }
}
