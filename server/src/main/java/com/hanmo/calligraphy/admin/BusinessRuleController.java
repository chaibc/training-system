package com.hanmo.calligraphy.admin;

import com.hanmo.calligraphy.common.BaseCrudController;
import com.hanmo.calligraphy.domain.entity.BusinessRule;
import com.hanmo.calligraphy.domain.mapper.BusinessRuleMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 业务规则管理：维护 business_rule 表中的键值型配置（ruleKey / ruleValue）。
 *
 * <p>业务模块：业务规则；接口前缀 {@code /api/admin/rules}。</p>
 * <p>后台页面：管理端「业务规则」(/settings，Settings.vue) —— 该页只读列表并逐条修改 ruleValue 与 description，
 * 因此 POST / DELETE 通道虽被基类开放，前端并未使用。</p>
 * <p>权限：GET 允许 ADMIN 与 ACADEMIC；POST/PUT/DELETE 由 SecurityConfig 限定仅 ADMIN。</p>
 * <p>通道：继承 {@link com.hanmo.calligraphy.common.BaseCrudController}，page/list/{id}/create/update/delete
 * 六个通道全部开放，未做定制或禁用；分页 keyword 命中 rule_key 列（全模糊匹配）。</p>
 */
@Tag(name = "管理端-业务规则")
@RestController
@RequestMapping("/api/admin/rules")
public class BusinessRuleController extends BaseCrudController<BusinessRule> {
    public BusinessRuleController(BusinessRuleMapper mapper) {
        super(mapper, BusinessRule.class);
    }
}
