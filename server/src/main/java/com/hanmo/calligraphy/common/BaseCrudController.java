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
 *
 * <p>标准通道共六个，路径 = 子类 @RequestMapping 前缀 + 后缀：
 * GET /page、GET /list、GET /{id}、POST ""（新增）、PUT /{id}、DELETE /{id}。
 * 子类可在同前缀下另加定制接口（如 /upload、/{id}/review），与上述通道互不冲突。</p>
 *
 * <p>权限不在本类控制：由 SecurityConfig 按 URL 前缀统一裁决 —— /api/admin/** 默认允许 ADMIN 与 ACADEMIC，
 * 其中课程与定价、基础字典、机构内容、业务规则、师资等前缀的写操作被收窄为仅 ADMIN。</p>
 *
 * <p>本类只做透传：不校验业务规则、不写审计日志、不联动课时账户与状态机，
 * 因此走通用通道可以绕过 admin 子包中定制接口的业务校验（点名消课、缴费充值、作品授权复核等），
 * 各控制器注释里会写明该风险。</p>
 *
 * <p>为规避上述风险，{@link #writeChannelsEnabled()} 提供开关：默认开放写通道；
 * 对「必须走业务接口」的模块（作品、点名记录、课时账户/流水、请假、作品授权、补课、缴费、试听），
 * 子类覆盖为 false 后，POST/PUT/DELETE 一律返回 405，只保留 page/list/{id} 查询通道。</p>
 *
 * <p>提供 /page、/list、/{id} 与 POST / PUT / DELETE 六类通用端点；关键字模糊查询只作用于
 * name / title / studentName / parentName / phone / ruleKey 中首个存在的 String 字段。</p>
 */
public abstract class BaseCrudController<T> {

    /** 具体实体的 MyBatis-Plus Mapper，所有 SQL 都经它执行（含逻辑删除改写） */
    protected final BaseMapper<T> mapper;
    /** 实体类型：用于 JSON 反序列化、反射注入 id、探测 keyword 可用的列 */
    protected final Class<T> type;

    /**
     * Jackson ObjectMapper，由 Spring 注入；把请求体 JsonNode 转成实体时沿用全局的日期/时区配置。
     */
    @Autowired
    protected ObjectMapper objectMapper;

    /**
     * @param mapper 实体对应的 BaseMapper
     * @param type   实体类型，用于 JSON 反序列化与反射读取 id
     */
    protected BaseCrudController(BaseMapper<T> mapper, Class<T> type) {
        this.mapper = mapper;
        this.type = type;
    }

    /**
     * 是否开放通用写通道（POST / PUT / DELETE）。
     *
     * <p>默认开放。对「必须走业务接口」的模块，子类覆盖返回 false：这三个方法会直接抛 405，
     * 避免绕过定制接口里的授权校验、课时扣减、审批联动与审计留痕。</p>
     *
     * @return true 表示允许通用新增/修改/删除
     */
    protected boolean writeChannelsEnabled() {
        return true;
    }

    /**
     * 写通道开关校验：关闭时统一抛 405，提示改用业务接口。
     *
     * @throws BusinessException 该模块已关闭通用写通道（405）
     */
    private void assertWriteEnabled() {
        if (!writeChannelsEnabled()) {
            throw new BusinessException(405, "该模块不支持通用增删改，请使用对应的业务接口");
        }
    }

    /**
     * 分页查询。
     *
     * <p>current 小于 1 时按 1 处理，size 被夹到 [1,200]（防止前端传超大 size 拖库）。
     * keyword 非空时，按 name → title → studentName → parentName → phone → ruleKey
     * 的顺序取实体上第一个存在的 String 字段做全模糊匹配；一个都没有则忽略 keyword（不报错、不过滤）。
     * 结果统一按 id 倒序。</p>
     *
     * @param current 页码，从 1 开始
     * @param size    每页条数，缺省 10，上限 200
     * @param keyword 可选关键字，命中的列随实体而定（见上）
     * @return 分页结果，含 records、total、current、size
     */
    @GetMapping("/page")
    public ApiResponse<PageResult<T>> page(@RequestParam(defaultValue = "1") long current,
                                           @RequestParam(defaultValue = "10") long size,
                                           @RequestParam(required = false) String keyword) {
        // 页码至少为 1，每页条数限制在 1~200，避免一次拉取过多数据
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

    /**
     * 不分页列表：按 id 倒序返回全表，主要供管理端下拉与字典选择使用。
     *
     * <p>没有筛选、没有条数上限，表大时请改用 /page 或定制接口。</p>
     *
     * @return 记录列表
     */
    @GetMapping("/list")
    public ApiResponse<List<T>> list() {
        return ApiResponse.ok(mapper.selectList(new QueryWrapper<T>().orderByDesc("id")));
    }

    /**
     * 按主键查详情。
     *
     * <p>逻辑删除的数据同样按「不存在」处理。</p>
     *
     * @param id 主键
     * @return 实体
     * @throws BusinessException 记录不存在时返回 404「记录不存在」
     */
    @GetMapping("/{id}")
    public ApiResponse<T> get(@PathVariable Long id) {
        T value = mapper.selectById(id);
        if (value == null) {
            throw new BusinessException(404, "记录不存在");
        }
        return ApiResponse.ok(value);
    }

    /**
     * 新增：把请求体 JSON 直接反序列化为实体并插入。
     *
     * <p>副作用：单表 insert，主键由数据库自增后回填到返回体。不做字段校验、不填 createdBy 等审计字段
     * （需要就由请求体传入），也不触发任何业务联动（课时充值、状态流转、授权校验等）。</p>
     *
     * @param body 实体字段 JSON
     * @return 插入后的实体（含 id）
     */
    @PostMapping
    public ApiResponse<T> create(@RequestBody JsonNode body) {
        assertWriteEnabled();
        T value = objectMapper.convertValue(body, type);
        mapper.insert(value);
        return ApiResponse.ok(value);
    }

    /**
     * 修改：把请求体还原成实体，反射写入路径中的 id 后按主键更新。
     *
     * <p>副作用：单表 update。MyBatis-Plus 默认只更新非 null 字段，所以请求体里缺省或显式为 null 的列
     * 会保持原值（也就无法通过本接口把某列置空）。不校验记录是否存在：id 不存在时更新影响 0 行，
     * 仍返回成功和请求体内容。</p>
     *
     * @param id   主键
     * @param body 实体字段 JSON
     * @return 请求体对应的实体（id 已被路径值覆盖）
     * @throws BusinessException 实体类没有 id 字段时返回 500（正常实体不会触发）
     */
    @PutMapping("/{id}")
    public ApiResponse<T> update(@PathVariable Long id, @RequestBody JsonNode body) {
        assertWriteEnabled();
        T value = objectMapper.convertValue(body, type);
        // 以路径 id 覆盖请求体中的 id，防止请求体伪造主键改到其他记录
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

    /**
     * 删除：调用 deleteById。
     *
     * <p>副作用：实体带 deleted 字段时走逻辑删除（application.yml 全局 logic-delete-field=deleted，
     * 置 1，之后的查询自动过滤）；没有该字段的实体是物理删除，不可恢复。
     * 不做引用校验，也不级联处理关联数据。</p>
     *
     * @param id 主键
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        assertWriteEnabled();
        mapper.deleteById(id);
        return ApiResponse.ok();
    }

    // 依次探测候选列名，返回第一个真实存在的 String 字段并转成下划线列名；都不存在则返回 null（keyword 不生效）
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
