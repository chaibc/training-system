package com.hanmo.calligraphy.admin;

import com.hanmo.calligraphy.common.BaseCrudController;
import com.hanmo.calligraphy.domain.entity.BusinessRule;
import com.hanmo.calligraphy.domain.mapper.BusinessRuleMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端-业务规则")
@RestController
@RequestMapping("/api/admin/rules")
public class BusinessRuleController extends BaseCrudController<BusinessRule> {
    public BusinessRuleController(BusinessRuleMapper mapper) {
        super(mapper, BusinessRule.class);
    }
}
