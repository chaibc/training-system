from pathlib import Path

root = Path(__file__).resolve().parents[1]
base = root / 'server' / 'src' / 'main' / 'java' / 'com' / 'hanmo' / 'calligraphy' / 'admin'
base.mkdir(parents=True, exist_ok=True)

items = [
    ('CourseCategory', 'categories', '课程分类'),
    ('CourseStage', 'stages', '课程阶段'),
    ('ClassType', 'class-types', '班型'),
    ('Classroom', 'classrooms', '教室'),
    ('LeaveReason', 'leave-reasons', '请假原因'),
    ('PaymentMethod', 'payment-methods', '缴费方式'),
    ('TrialSlot', 'trial-slots', '试听时段'),
    ('Course', 'courses', '课程'),
    ('CoursePrice', 'course-prices', '班型价格'),
    ('CourseOutline', 'course-outlines', '内容大纲'),
    ('TeacherProfile', 'teachers', '教师'),
    ('Qualification', 'qualifications', '资质'),
    ('Notice', 'notices', '公告'),
    ('InstitutionImage', 'institution-images', '机构图片'),
    ('BusinessRule', 'rules', '业务规则'),
    ('TrialBooking', 'trials', '试听预约'),
    ('PaymentRecord', 'payments', '缴费记录'),
    ('ClassGroup', 'classes', '班级'),
    ('Lesson', 'lessons', '课次'),
    ('Work', 'works', '作品'),
    ('WorkComment', 'work-comments', '作品评论'),
    ('WorkAuth', 'work-auths', '作品授权'),
    ('HourAccount', 'hour-accounts', '课时账户'),
    ('HourLedger', 'hour-ledger', '课时流水'),
    ('MakeupArrangement', 'makeup-arrangements', '补课安排'),
]

template = '''package com.hanmo.calligraphy.admin;

import com.hanmo.calligraphy.common.BaseCrudController;
import com.hanmo.calligraphy.domain.entity.{cls};
import com.hanmo.calligraphy.domain.mapper.{cls}Mapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端-{label}")
@RestController
@RequestMapping("/api/admin/{route}")
public class {cls}Controller extends BaseCrudController<{cls}> {{
    public {cls}Controller({cls}Mapper mapper) {{
        super(mapper, {cls}.class);
    }}
}}
'''

for cls, route, label in items:
    path = base / f'{cls}Controller.java'
    path.write_text(template.format(cls=cls, route=route, label=label), encoding='utf-8')

print(f'generated {len(items)} controllers')
