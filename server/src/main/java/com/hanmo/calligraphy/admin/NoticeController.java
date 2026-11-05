package com.hanmo.calligraphy.admin;

import com.hanmo.calligraphy.common.BaseCrudController;
import com.hanmo.calligraphy.domain.entity.Notice;
import com.hanmo.calligraphy.domain.mapper.NoticeMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端-公告")
@RestController
@RequestMapping("/api/admin/notices")
public class NoticeController extends BaseCrudController<Notice> {
    public NoticeController(NoticeMapper mapper) {
        super(mapper, Notice.class);
    }
}
