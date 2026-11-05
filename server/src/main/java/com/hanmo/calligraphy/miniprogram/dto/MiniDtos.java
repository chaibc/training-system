package com.hanmo.calligraphy.miniprogram.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 小程序端请求体 DTO 集合。
 *
 * <p>小程序接口的<b>响应</b>没有独立 DTO，统一由 Service 组装成 {@code Map<String, Object>}
 * （字段名即前端契约），所以这里只放请求入参。使用 record 省去 getter/setter，
 * Bean Validation 注解直接标在组件上。</p>
 *
 * <p>字段名被小程序页面直接使用，改动前需同步 {@code miniprogram/} 下的调用方。</p>
 */
public class MiniDtos {

    /**
     * 试听预约请求体（{@code POST /api/public/trial}，无需登录）。
     *
     * @param studentName 学员姓名，必填
     * @param age         年龄段文本，如“6 岁”，可选
     * @param parentName  家长称呼，可选
     * @param phone       联系手机号，必填；Service 再用 1 开头 11 位正则校验
     * @param courseId    意向课程 ID，可空
     * @param slotId      试听时段 ID（{@code trial_slot}），可空
     * @param note        备注，如期望时间，可选
     */
    public record TrialRequest(
            @NotBlank(message = "请输入学员姓名") String studentName,
            String age,
            String parentName,
            @NotBlank(message = "请输入手机号") String phone,
            Long courseId,
            Long slotId,
            String note
    ) {}

    /**
     * 提交家长-学员绑定申请（{@code POST /api/mini/parent/bindings}）。
     *
     * <p>两种用法：绑定已有学员传 {@code studentId} 或 {@code studentNo} + {@code phoneTail}；
     * 新建学员传 {@code newStudent=true} 加学员信息。两种都要经教务审核，落库状态 PENDING。</p>
     *
     * @param studentId   已存在学员 ID，与 studentNo 二选一
     * @param studentName 新建学员姓名（newStudent=true 时必填）
     * @param studentNo   已有学员编号，用于按编号查档案
     * @param phoneTail   主监护人手机号后四位，绑定已有学员时的安全校验
     * @param relation    与学员关系：MOTHER/FATHER/GRANDPARENT/OTHER，非法值按 OTHER 落库
     * @param newStudent  true 表示新建学员并申请绑定
     * @param gender      新建学员性别，可选
     * @param birthDate   新建学员出生日期，可选
     * @param school      新建学员学校，可选
     * @param grade       新建学员年级，可选
     */
    public record BindingRequest(
            Long studentId,
            String studentName,
            String studentNo,
            String phoneTail,
            String relation,
            Boolean newStudent,
            String gender,
            LocalDate birthDate,
            String school,
            String grade
    ) {}

    /**
     * 家长可自行修改的学员基础资料（{@code PUT /api/mini/parent/children/{studentId}}）。
     *
     * <p>只开放四项；字段为 null 表示不修改。姓名、学号等关键信息需联系教务。</p>
     *
     * @param gender    性别，null 表示不修改
     * @param birthDate 出生日期，null 表示不修改
     * @param school    学校，null 表示不修改
     * @param grade     年级，null 表示不修改
     */
    public record ChildProfileRequest(
            String gender,
            LocalDate birthDate,
            String school,
            String grade
    ) {}

    /**
     * 提交请假申请（{@code POST /api/mini/parent/leaves}）。
     *
     * @param studentId  学员 ID，必须是当前家长 ACTIVE 绑定的子女
     * @param lessonId   课次 ID，必须属于该学员在读班级且尚未开始
     * @param reasonId   请假原因字典 ID，可空
     * @param reasonText 具体说明；为空时用原因字典名称兜底
     */
    public record LeaveCreateRequest(
            @NotNull(message = "请选择学员") Long studentId,
            @NotNull(message = "请选择课次") Long lessonId,
            Long reasonId,
            String reasonText
    ) {}

    /**
     * 提交报名申请（{@code POST /api/mini/parent/enrollments}）。
     *
     * @param studentId 学员 ID，必须是当前家长 ACTIVE 绑定的子女
     * @param courseId  课程 ID，必须为上架课程
     * @param classId   班级 ID，可空；传了则校验班级与课程匹配、班级状态与名额
     */
    public record EnrollmentCreateRequest(
            @NotNull(message = "请选择学员") Long studentId,
            @NotNull(message = "请选择课程") Long courseId,
            Long classId
    ) {}

    /**
     * 点名名单中的一位学员。
     *
     * @param studentId 学员 ID，必须在课次所属班级名单中（或已有该课次出勤记录）
     * @param status    出勤状态：PRESENT 出勤 / LEAVE 请假 / ABSENT 缺勤
     * @param note      备注，为空时由 Service 按状态生成默认文案
     */
    public record AttendanceItem(Long studentId, String status, String note) {}

    /**
     * 点名提交请求（{@code POST /api/mini/teacher/lessons/{id}/rollcall}）。
     *
     * @param items 点名明细，不能为空；同一 studentId 不能出现两次
     */
    public record RollcallRequest(List<AttendanceItem> items) {}

    /**
     * 请假审批请求（{@code POST /api/mini/teacher/leaves/{id}/approve}）。
     *
     * @param approve      是否通过；只有显式传 false 才算驳回，null 视为通过
     * @param rejectReason 驳回原因，驳回时必填
     */
    public record ApproveLeaveRequest(Boolean approve, String rejectReason) {}

    /**
     * 教师上传学员作品（{@code POST /api/mini/teacher/works}）。
     *
     * @param studentId   学员 ID，必须在当前教师所带班级中
     * @param classId     班级 ID，可空；为空时自动取该学员在自己班中的第一个班
     * @param title       作品标题，必填
     * @param workType    书体：HARD 硬笔 / SOFT 软笔，为空默认 HARD
     * @param contentChar 作品内容字，可选
     * @param imageUrl    图片地址，可选
     * @param comment     教师点评，可选
     * @param authorized  是否已取得家长展示授权，必须为 true；且后台需有 ACTIVE 的 work_auth
     */
    public record WorkCreateRequest(
            @NotNull(message = "请选择学员") Long studentId,
            Long classId,
            @NotBlank(message = "请输入作品标题") String title,
            String workType,
            String contentChar,
            String imageUrl,
            String comment,
            Boolean authorized
    ) {}

    /**
     * 家长作品留言（{@code POST /api/mini/parent/works/{workId}/comments}）。
     *
     * @param content 留言正文，必填；Service 还会限制不超过 300 字
     */
    public record CommentRequest(@NotBlank(message = "请输入留言内容") String content) {}

    /**
     * 金额入参（预留 DTO；当前代码中未见引用）。
     *
     * @param amount 金额
     */
    public record AmountRequest(BigDecimal amount) {}
}
