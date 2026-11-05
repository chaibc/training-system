package com.hanmo.calligraphy.admin.org;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hanmo.calligraphy.common.ApiResponse;
import com.hanmo.calligraphy.common.BusinessException;
import com.hanmo.calligraphy.domain.entity.InstitutionInfo;
import com.hanmo.calligraphy.domain.mapper.InstitutionInfoMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端-机构资料")
@RestController
@RequestMapping("/api/admin/org")
@RequiredArgsConstructor
public class OrgAdminController {

    private final InstitutionInfoMapper institutionInfoMapper;

    @Operation(summary = "机构资料")
    @GetMapping("/info")
    public ApiResponse<InstitutionInfo> info() {
        InstitutionInfo info = institutionInfoMapper.selectOne(new LambdaQueryWrapper<InstitutionInfo>()
                .orderByAsc(InstitutionInfo::getId).last("limit 1"));
        if (info == null) {
            info = new InstitutionInfo();
            info.setName("翰墨学堂");
            info.setBrand("翰墨学堂");
            info.setStatus("OPEN");
        }
        return ApiResponse.ok(info);
    }

    @Operation(summary = "保存机构资料")
    @PutMapping("/info")
    public ApiResponse<InstitutionInfo> save(@RequestBody InstitutionInfo body) {
        InstitutionInfo info = institutionInfoMapper.selectOne(new LambdaQueryWrapper<InstitutionInfo>()
                .orderByAsc(InstitutionInfo::getId).last("limit 1"));
        if (info == null) {
            body.setId(null);
            institutionInfoMapper.insert(body);
            return ApiResponse.ok(body);
        }
        body.setId(info.getId());
        institutionInfoMapper.updateById(body);
        return ApiResponse.ok(body);
    }
}
