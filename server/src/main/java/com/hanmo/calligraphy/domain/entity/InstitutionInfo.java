package com.hanmo.calligraphy.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.*;
import lombok.Data;

@Data
@TableName("institution_info")
public class InstitutionInfo {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String brand;
    private String slogan;
    private String heroIntro;
    private String intro;
    private String philosophy;
    private String phone;
    private String wechat;
    private String hours;
    private String address;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String traffic;
    private Integer sinceYear;
    private String status;
    private LocalDateTime updatedAt;
}
