package com.hanmo.calligraphy.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.*;
import lombok.Data;

/**
 * 机构资料，对应表 institution_info。
 *
 * <p>单行表，保存机构名称、简介、联系方式、地址与经纬度等对外展示信息。</p>
 */
@Data
@TableName("institution_info")
public class InstitutionInfo {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    // 品牌名
    private String brand;
    // 宣传语
    private String slogan;
    // 首页引导简介
    private String heroIntro;
    // 机构介绍
    private String intro;
    // 教学理念
    private String philosophy;
    // 联系电话
    private String phone;
    // 微信号
    private String wechat;
    // 营业时间
    private String hours;
    // 机构地址
    private String address;
    // 经度（用于地图定位）
    private BigDecimal longitude;
    // 纬度（用于地图定位）
    private BigDecimal latitude;
    // 交通指引
    private String traffic;
    // 创办年份
    private Integer sinceYear;
    // 营业状态（默认 OPEN）
    private String status;
    private LocalDateTime updatedAt;
}
