package com.hanmo.calligraphy.admin;

import com.hanmo.calligraphy.common.BaseCrudController;
import com.hanmo.calligraphy.domain.entity.ClassType;
import com.hanmo.calligraphy.domain.mapper.ClassTypeMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 班型管理：class_type 表（名称、容量区间 capacityMin/capacityMax、排序、启停）的增删改查。
 *
 * <p>业务模块：基础数据字典；接口前缀 {@code /api/admin/class-types}。</p>
 * <p>后台页面：管理端「基础数据」(/basic，BasicData.vue) 的班型页签（容量按区间录入）。</p>
 * <p>权限：GET 允许 ADMIN 与 ACADEMIC；POST/PUT/DELETE 由 SecurityConfig 限定仅 ADMIN。</p>
 * <p>通道：继承 {@link BaseCrudController}，六个通道全部开放；分页 keyword 命中 name 列；删除为物理删除，
 * 不校验 course_price.class_type_id 的引用。</p>
 */
@Tag(name = "管理端-班型")
@RestController
@RequestMapping("/api/admin/class-types")
public class ClassTypeController extends BaseCrudController<ClassType> {
    public ClassTypeController(ClassTypeMapper mapper) {
        super(mapper, ClassType.class);
    }
}
