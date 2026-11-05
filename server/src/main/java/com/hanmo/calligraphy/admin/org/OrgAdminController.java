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

/**
 * 机构资料管理接口：读写单行表 institution_info（品牌名、简介、联系方式、地址、营业状态等）。
 *
 * <p>路由前缀 /api/admin/org。没有 service 层，直接使用 InstitutionInfoMapper；
 * 接口不做字段级校验、不写审计日志、未开事务（均为单表单行操作）。</p>
 *
 * <p>依赖表：institution_info（约定只维护 id 最小的一行；教学环境图片另由 institution_image 管理）。</p>
 */
@Tag(name = "管理端-机构资料")
@RestController
@RequestMapping("/api/admin/org")
@RequiredArgsConstructor
public class OrgAdminController {

    private final InstitutionInfoMapper institutionInfoMapper;

    /**
     * 查询机构资料。
     *
     * <p>只读：取 id 最小的一行；表为空时返回一个未落库的默认对象（翰墨学堂 / OPEN），
     * 方便前端首次渲染，此时调用保存才会真正 insert。</p>
     *
     * @return 机构资料；无记录时为内存中的默认值
     */
    @Operation(summary = "机构资料")
    @GetMapping("/info")
    public ApiResponse<InstitutionInfo> info() {
        InstitutionInfo info = institutionInfoMapper.selectOne(new LambdaQueryWrapper<InstitutionInfo>()
                .orderByAsc(InstitutionInfo::getId).last("limit 1"));
        if (info == null) {
            info = new InstitutionInfo();
            info.setName("翰墨学堂");
            info.setBrand("翰墨学堂");
            // 默认壳只在内存返回，不 insert：首次保存走 save 的新增分支
            info.setStatus("OPEN");
        }
        return ApiResponse.ok(info);
    }

    /**
     * 保存机构资料（无记录则新增，有记录则更新）。
     *
     * <p>副作用：insert 或 update institution_info 一行；不写审计日志、不开事务。
     * MyBatis-Plus updateById 默认忽略 null 字段，因此请求体里没带的字段会保留原值，
     * 不会被清空。</p>
     *
     * @param body 机构资料，id 由服务端根据是否已存在覆盖
     * @return 保存后的资料（新增时含自增 id）
     */
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
        // 强制沿用已存在行的 id，防止请求体传其它 id 导致改错行
        body.setId(info.getId());
        institutionInfoMapper.updateById(body);
        return ApiResponse.ok(body);
    }
}
