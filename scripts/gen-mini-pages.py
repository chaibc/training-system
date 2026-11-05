import json
from pathlib import Path

base = Path(r"E:\deepseek\项目\书法培训\miniprogram\pages")
pages = {
    "login": ("登录", False),
    "home": ("翰墨学堂", True),
    "courses": ("课程体系", False),
    "course-detail": ("课程详情", False),
    "trial": ("试听预约", False),
    "trial-done": ("预约成功", False),
    "enroll": ("报名登记", False),
    "enroll-done": ("报名成功", False),
    "enrollments": ("报名记录", True),
    "schedule": ("我的课表", True),
    "lesson-detail": ("课次详情", False),
    "leave": ("请假申请", False),
    "leave-done": ("请假成功", False),
    "leave-records": ("请假记录", True),
    "works": ("作品墙", True),
    "work-detail": ("作品详情", False),
    "me": ("我的", True),
    "child-profile": ("学员档案", False),
    "hours": ("课时账户", True),
    "attendance": ("上课记录", True),
    "growth": ("成长相册", False),
    "about": ("机构介绍", False),
    "teachers": ("师资团队", False),
    "teacher-detail": ("教师详情", False),
    "teacher-home": ("教师工作台", True),
    "rollcall": ("点名与消课", False),
    "teacher-leave": ("请假审批", True),
    "upload-work": ("上传作品", False),
    "account": ("账号与绑定", False),
}
for name, (title, pull) in pages.items():
    folder = base / name
    folder.mkdir(parents=True, exist_ok=True)
    data = {
        "navigationBarTitleText": title,
        "usingComponents": {}
    }
    if pull:
        data["enablePullDownRefresh"] = True
    (folder / f"{name}.json").write_text(
        json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print("generated", len(pages), "page json files")
