package com.hanmo.calligraphy.admin;

import com.hanmo.calligraphy.common.BaseCrudController;
import com.hanmo.calligraphy.domain.entity.HourAccount;
import com.hanmo.calligraphy.domain.mapper.HourAccountMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端-课时账户")
@RestController
@RequestMapping("/api/admin/hour-accounts")
public class HourAccountController extends BaseCrudController<HourAccount> {
    public HourAccountController(HourAccountMapper mapper) {
        super(mapper, HourAccount.class);
    }
}
