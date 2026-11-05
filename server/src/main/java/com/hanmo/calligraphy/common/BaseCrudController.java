package com.hanmo.calligraphy.common;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Field;
import java.util.List;

/**
 * 通用 CRUD 控制器。具体模块继承后只需声明 @RestController 与 @RequestMapping。
 * 复杂业务（审批、消课、缴费等）使用独立控制器。
 */
public abstract class BaseCrudController<T> {

    protected final BaseMapper<T> mapper;
    protected final Class<T> type;

    @Autowired
    protected ObjectMapper objectMapper;

    protected BaseCrudController(BaseMapper<T> mapper, Class<T> type) {
        this.mapper = mapper;
        this.type = type;
    }

    @GetMapping("/page")
    public ApiResponse<PageResult<T>> page(@RequestParam(defaultValue = "1") long current,
                                           @RequestParam(defaultValue = "10") long size,
                                           @RequestParam(required = false) String keyword) {
        Page<T> page = new Page<>(Math.max(1, current), Math.min(Math.max(1, size), 200));
        QueryWrapper<T> wrapper = new QueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            String field = keywordField();
            if (field != null) {
                wrapper.like(field, keyword.trim());
            }
        }
        wrapper.orderByDesc("id");
        mapper.selectPage(page, wrapper);
        return ApiResponse.ok(PageResult.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize()));
    }

    @GetMapping("/list")
    public ApiResponse<List<T>> list() {
        return ApiResponse.ok(mapper.selectList(new QueryWrapper<T>().orderByDesc("id")));
    }

    @GetMapping("/{id}")
    public ApiResponse<T> get(@PathVariable Long id) {
        T value = mapper.selectById(id);
        if (value == null) {
            throw new BusinessException(404, "记录不存在");
        }
        return ApiResponse.ok(value);
    }

    @PostMapping
    public ApiResponse<T> create(@RequestBody JsonNode body) {
        T value = objectMapper.convertValue(body, type);
        mapper.insert(value);
        return ApiResponse.ok(value);
    }

    @PutMapping("/{id}")
    public ApiResponse<T> update(@PathVariable Long id, @RequestBody JsonNode body) {
        T value = objectMapper.convertValue(body, type);
        try {
            Field idField = type.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(value, id);
        } catch (Exception e) {
            throw new BusinessException(500, "实体缺少 id 字段");
        }
        mapper.updateById(value);
        return ApiResponse.ok(value);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        mapper.deleteById(id);
        return ApiResponse.ok();
    }

    private String keywordField() {
        for (String candidate : List.of("name", "title", "studentName", "parentName", "phone", "ruleKey")) {
            try {
                Field field = type.getDeclaredField(candidate);
                if (field.getType() == String.class) {
                    return com.baomidou.mybatisplus.core.toolkit.StringUtils.camelToUnderline(candidate);
                }
            } catch (NoSuchFieldException ignored) {
            }
        }
        return null;
    }
}
