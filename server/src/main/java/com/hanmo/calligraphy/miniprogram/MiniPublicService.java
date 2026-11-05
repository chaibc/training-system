package com.hanmo.calligraphy.miniprogram;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hanmo.calligraphy.common.BusinessException;
import com.hanmo.calligraphy.domain.entity.ClassGroup;
import com.hanmo.calligraphy.domain.entity.ClassStudent;
import com.hanmo.calligraphy.domain.entity.Course;
import com.hanmo.calligraphy.domain.entity.CourseCategory;
import com.hanmo.calligraphy.domain.entity.CourseOutline;
import com.hanmo.calligraphy.domain.entity.CoursePrice;
import com.hanmo.calligraphy.domain.entity.CourseStage;
import com.hanmo.calligraphy.domain.entity.InstitutionImage;
import com.hanmo.calligraphy.domain.entity.InstitutionInfo;
import com.hanmo.calligraphy.domain.entity.Notice;
import com.hanmo.calligraphy.domain.entity.Qualification;
import com.hanmo.calligraphy.domain.entity.Student;
import com.hanmo.calligraphy.domain.entity.TeacherProfile;
import com.hanmo.calligraphy.domain.entity.TrialBooking;
import com.hanmo.calligraphy.domain.entity.TrialSlot;
import com.hanmo.calligraphy.domain.entity.Work;
import com.hanmo.calligraphy.domain.entity.WorkComment;
import com.hanmo.calligraphy.domain.mapper.ClassGroupMapper;
import com.hanmo.calligraphy.domain.mapper.ClassStudentMapper;
import com.hanmo.calligraphy.domain.mapper.CourseCategoryMapper;
import com.hanmo.calligraphy.domain.mapper.CourseMapper;
import com.hanmo.calligraphy.domain.mapper.CourseOutlineMapper;
import com.hanmo.calligraphy.domain.mapper.CoursePriceMapper;
import com.hanmo.calligraphy.domain.mapper.CourseStageMapper;
import com.hanmo.calligraphy.domain.mapper.InstitutionImageMapper;
import com.hanmo.calligraphy.domain.mapper.InstitutionInfoMapper;
import com.hanmo.calligraphy.domain.mapper.NoticeMapper;
import com.hanmo.calligraphy.domain.mapper.QualificationMapper;
import com.hanmo.calligraphy.domain.mapper.StudentMapper;
import com.hanmo.calligraphy.domain.mapper.TeacherProfileMapper;
import com.hanmo.calligraphy.domain.mapper.TrialBookingMapper;
import com.hanmo.calligraphy.domain.mapper.TrialSlotMapper;
import com.hanmo.calligraphy.domain.mapper.WorkCommentMapper;
import com.hanmo.calligraphy.domain.mapper.WorkMapper;
import com.hanmo.calligraphy.miniprogram.dto.MiniDtos.TrialRequest;
import com.hanmo.calligraphy.system.entity.SysUser;
import com.hanmo.calligraphy.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 小程序公开内容服务：首页聚合、课程/师资/作品/公告查询、试听预约与作品点赞。
 *
 * <p>本服务不接收登录身份，所有查询都必须自带“对外可见”过滤，不能依赖调用方传参：
 * 课程 {@code status=ON}、教师 {@code ACTIVE}、作品 {@code DISPLAYED}、公告 {@code PUBLISHED}、
 * 资质 {@code visible=1 且 VERIFIED}。</p>
 *
 * <p>返回的 Map 键名就是小程序前端直接读取的字段契约，改名/删字段会直接打断页面；
 * 公开侧之所以无需额外脱敏，是因为这里根本没有把手机号、学号、家长账号等字段放进 Map。</p>
 */
@Service
@RequiredArgsConstructor
public class MiniPublicService {

    private final InstitutionInfoMapper institutionMapper;
    private final InstitutionImageMapper imageMapper;
    private final CourseMapper courseMapper;
    private final CourseCategoryMapper categoryMapper;
    private final CourseStageMapper stageMapper;
    private final CoursePriceMapper priceMapper;
    private final CourseOutlineMapper outlineMapper;
    private final TeacherProfileMapper teacherMapper;
    private final ClassGroupMapper classGroupMapper;
    private final ClassStudentMapper classStudentMapper;
    private final StudentMapper studentMapper;
    private final WorkMapper workMapper;
    private final WorkCommentMapper commentMapper;
    private final NoticeMapper noticeMapper;
    private final QualificationMapper qualificationMapper;
    private final TrialBookingMapper trialMapper;
    private final TrialSlotMapper trialSlotMapper;
    private final SysUserMapper userMapper;

    /**
     * 首页聚合：机构信息、推荐课程、师资、作品、公告、资质。
     *
     * <p>各部分都有条数上限（课程 3、师资 4、作品 6、公告 2），避免首页一次拉太多数据。
     * 机构信息取 id 最小的一条；未配置时返回体不含 {@code institution} 键。</p>
     *
     * @return 聚合 Map，键为 institution/courses/teachers/works/notices/qualifications
     */
    public Map<String, Object> home() {
        Map<String, Object> data = new LinkedHashMap<>();
        InstitutionInfo info = institutionMapper.selectOne(new LambdaQueryWrapper<InstitutionInfo>()
                .orderByAsc(InstitutionInfo::getId).last("limit 1"));
        // 机构信息取 id 最小的一条；未配置时返回体里不含 institution 键
        if (info != null) {
            Map<String, Object> inst = new LinkedHashMap<>();
            inst.put("name", info.getName());
            inst.put("brand", info.getBrand());
            inst.put("slogan", info.getSlogan());
            inst.put("heroIntro", info.getHeroIntro());
            inst.put("intro", info.getIntro());
            inst.put("phone", info.getPhone());
            inst.put("wechat", info.getWechat());
            inst.put("hours", info.getHours());
            inst.put("address", info.getAddress());
            inst.put("longitude", info.getLongitude());
            inst.put("latitude", info.getLatitude());
            inst.put("traffic", info.getTraffic());
            inst.put("sinceYear", info.getSinceYear());
            inst.put("images", imageMapper.selectList(new LambdaQueryWrapper<InstitutionImage>()
                    .orderByAsc(InstitutionImage::getSort)));
            data.put("institution", inst);
        }
        // 首页推荐：仅上架且 featured=1 的课程，最多 3 门
        data.put("courses", courseMapper.selectList(new LambdaQueryWrapper<Course>()
                        .eq(Course::getStatus, "ON").eq(Course::getFeatured, 1)
                        .orderByAsc(Course::getSort).last("limit 3"))
                .stream().map(this::courseMap).toList());
        // 首页师资最多 4 位，展示字段见 teacherMap
        data.put("teachers", teacherMapper.selectList(new LambdaQueryWrapper<TeacherProfile>()
                        .eq(TeacherProfile::getStatus, "ACTIVE").orderByAsc(TeacherProfile::getSort).last("limit 4"))
                .stream().map(this::teacherMap).toList());
        // 首页作品墙最多 6 件，仅已展示作品
        data.put("works", workMapper.selectList(new LambdaQueryWrapper<Work>()
                        .eq(Work::getStatus, "DISPLAYED").orderByDesc(Work::getId).last("limit 6"))
                .stream().map(this::workMap).toList());
        // 首页公告最多 2 条，仅已发布
        data.put("notices", noticeMapper.selectList(new LambdaQueryWrapper<Notice>()
                        .eq(Notice::getStatus, "PUBLISHED").orderByDesc(Notice::getPublishDate).last("limit 2"))
                .stream().map(this::noticeMap).toList());
        // 资质仅展示已核验且勾选展示的记录，且只放出名称/编号/有效期/核验状态
        data.put("qualifications", qualificationMapper.selectList(new LambdaQueryWrapper<Qualification>()
                        .eq(Qualification::getVisible, 1)
                        .eq(Qualification::getVerifyStatus, "VERIFIED")
                        .orderByAsc(Qualification::getSort))
                .stream().map(q -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", q.getId());
                    item.put("name", q.getName());
                    item.put("code", q.getCode());
                    item.put("validUntil", q.getValidUntil());
                    item.put("verifyStatus", q.getVerifyStatus());
                    return item;
                }).toList());
        return data;
    }

    /**
     * 上架课程查询。
     *
     * @param categoryId 分类 ID，null 表示不限
     * @param stageId    阶段 ID，null 表示不限
     * @param keyword    课程名模糊关键字，null/空表示不限
     * @return 课程 Map 列表，按 sort 升序、id 倒序
     */
    public List<Map<String, Object>> courses(Long categoryId, Long stageId, String keyword) {
        LambdaQueryWrapper<Course> qw = new LambdaQueryWrapper<Course>().eq(Course::getStatus, "ON");
        if (categoryId != null) qw.eq(Course::getCategoryId, categoryId);
        if (stageId != null) qw.eq(Course::getStageId, stageId);
        if (StringUtils.hasText(keyword)) qw.like(Course::getName, keyword);
        qw.orderByAsc(Course::getSort).orderByDesc(Course::getId);
        return courseMapper.selectList(qw).stream().map(this::courseMap).toList();
    }

    /**
     * 课程详情：课程字段 + 有效价格 + 可见大纲。
     *
     * @param id 课程 ID
     * @return 课程 Map 追加 prices（status=ON）与 outlines（visible=1）
     * @throws BusinessException 404 课程不存在或已下架
     */
    public Map<String, Object> courseDetail(Long id) {
        Course c = courseMapper.selectById(id);
        // 下架课程按不存在处理，避免旧链接继续暴露内容
        if (c == null || !"ON".equals(c.getStatus())) throw new BusinessException(404, "课程不存在或已下架");
        Map<String, Object> map = courseMap(c);
        map.put("prices", priceMapper.selectList(new LambdaQueryWrapper<CoursePrice>()
                        .eq(CoursePrice::getCourseId, id).eq(CoursePrice::getStatus, "ON")
                        .orderByAsc(CoursePrice::getSort))
                .stream().map(p -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", p.getId());
                    item.put("classTypeId", p.getClassTypeId());
                    item.put("lessons", p.getLessons());
                    item.put("price", p.getPrice());
                    item.put("unit", p.getUnit());
                    return item;
                }).toList());
        map.put("outlines", outlineMapper.selectList(new LambdaQueryWrapper<CourseOutline>()
                        .eq(CourseOutline::getCourseId, id).eq(CourseOutline::getVisible, 1)
                        .orderByAsc(CourseOutline::getSort))
                .stream().map(o -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("title", o.getTitle());
                    item.put("lessonRange", o.getLessonRange());
                    item.put("description", o.getDescription());
                    return item;
                }).toList());
        return map;
    }

    /**
     * 师资列表。
     *
     * @return 教师 Map 列表，仅 ACTIVE，按 sort 升序
     */
    public List<Map<String, Object>> teachers() {
        return teacherMapper.selectList(new LambdaQueryWrapper<TeacherProfile>()
                        .eq(TeacherProfile::getStatus, "ACTIVE").orderByAsc(TeacherProfile::getSort))
                .stream().map(this::teacherMap).toList();
    }

    /**
     * 教师详情：展示字段 + 所带班级对应的课程（去重）。
     *
     * @param id 教师档案 ID
     * @return 教师 Map 追加 courses
     * @throws BusinessException 404 教师不存在
     */
    public Map<String, Object> teacherDetail(Long id) {
        TeacherProfile t = teacherMapper.selectById(id);
        if (t == null) throw new BusinessException(404, "教师不存在");
        Map<String, Object> map = teacherMap(t);
        List<Long> courseIds = classGroupMapper.selectList(new LambdaQueryWrapper<ClassGroup>()
                        .eq(ClassGroup::getTeacherId, id)).stream()
                .map(ClassGroup::getCourseId).distinct().toList();
        map.put("courses", courseIds.isEmpty() ? List.of() :
                courseMapper.selectBatchIds(courseIds).stream().map(this::courseMap).toList());
        return map;
    }

    /**
     * 作品墙查询。
     *
     * @param type    书体类型，null/空表示不限
     * @param keyword 标题模糊关键字
     * @return 作品 Map 列表，仅 DISPLAYED，按 id 倒序
     */
    public List<Map<String, Object>> works(String type, String keyword) {
        LambdaQueryWrapper<Work> qw = new LambdaQueryWrapper<Work>().eq(Work::getStatus, "DISPLAYED");
        if (StringUtils.hasText(type)) qw.eq(Work::getWorkType, type);
        if (StringUtils.hasText(keyword)) qw.like(Work::getTitle, keyword);
        qw.orderByDesc(Work::getId);
        return workMapper.selectList(qw).stream().map(this::workMap).toList();
    }

    /**
     * 作品详情 + 已审核留言。
     *
     * @param id 作品 ID
     * @return 作品 Map 追加 comments（仅 APPROVED，家长显示名缺失时兜底“家长”）
     * @throws BusinessException 404 作品不存在或未展示
     */
    public Map<String, Object> workDetail(Long id) {
        Work w = workMapper.selectById(id);
        if (w == null || !"DISPLAYED".equals(w.getStatus())) throw new BusinessException(404, "作品不存在或未展示");
        Map<String, Object> map = workMap(w);
        map.put("comments", commentMapper.selectList(new LambdaQueryWrapper<WorkComment>()
                        .eq(WorkComment::getWorkId, id).eq(WorkComment::getStatus, "APPROVED")
                        .orderByDesc(WorkComment::getId))
                .stream().map(c -> {
                    SysUser u = c.getParentUserId() == null ? null : userMapper.selectById(c.getParentUserId());
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", c.getId());
                    item.put("content", c.getContent());
                    // 公开页只展示家长显示名，不暴露家长账号 ID；显示名缺失时兜底“家长”
                    item.put("userName", u == null ? "家长" : u.getName());
                    item.put("createdAt", c.getCreatedAt());
                    return item;
                }).toList());
        return map;
    }

    /**
     * 公告列表。
     *
     * @return 公告 Map 列表，仅 PUBLISHED，按发布日期倒序
     */
    public List<Map<String, Object>> notices() {
        return noticeMapper.selectList(new LambdaQueryWrapper<Notice>()
                        .eq(Notice::getStatus, "PUBLISHED").orderByDesc(Notice::getPublishDate))
                .stream().map(this::noticeMap).toList();
    }

    /**
     * 提交试听预约。
     *
     * <p>副作用：插入一条 {@code trial_booking}，状态 {@code NEW}，单号按
     * “ST + yyyyMMdd + 4 位随机数”生成（仅便于人工核对，不是全局唯一序列）。</p>
     *
     * @param request 预约表单
     * @return bookingNo / studentName / courseName / slotName
     * @throws BusinessException 400 手机号不是 11 位或意向课程不存在
     */
    @Transactional
    public Map<String, Object> trial(TrialRequest request) {
        // 手机号必须为 1 开头共 11 位；前端校验不可信，服务端再兜一次
        if (!request.phone().matches("^1\\d{10}$")) throw new BusinessException(400, "请填写正确的手机号");
        if (request.courseId() != null && courseMapper.selectById(request.courseId()) == null) {
            throw new BusinessException(400, "意向课程不存在");
        }
        TrialBooking booking = new TrialBooking();
        // 预约单号：ST + 日期 + 4 位随机数，仅用于展示与人工核对
        booking.setBookingNo("ST" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000)));
        booking.setStudentName(request.studentName());
        booking.setAge(request.age());
        booking.setParentName(request.parentName());
        booking.setPhone(request.phone());
        booking.setCourseId(request.courseId());
        booking.setSlotId(request.slotId());
        booking.setNote(request.note());
        booking.setStatus("NEW");
        trialMapper.insert(booking);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("bookingNo", booking.getBookingNo());
        map.put("studentName", booking.getStudentName());
        map.put("courseName", booking.getCourseId() == null ? "" : courseName(booking.getCourseId()));
        TrialSlot slot = booking.getSlotId() == null ? null : trialSlotMapper.selectById(booking.getSlotId());
        map.put("slotName", slot == null ? "" : slot.getName());
        return map;
    }

    /**
     * 作品点赞 +1。
     *
     * <p>副作用：{@code work.like_count} 自增 1；本期不做去重，重复调用会一直累加。</p>
     *
     * @param id 作品 ID
     * @return 最新点赞数（读到的旧值 + 1）
     * @throws BusinessException 404 作品不存在
     */
    @Transactional
    public Map<String, Object> like(Long id) {
        Work work = workMapper.selectById(id);
        if (work == null) throw new BusinessException(404, "作品不存在");
        workMapper.update(null, new LambdaUpdateWrapper<Work>().eq(Work::getId, id)
                // 用 SQL 自增避免并发覆盖；下面的返回值仍是“读到的旧值 + 1”
                .setSql("like_count = like_count + 1"));
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("likeCount", (work.getLikeCount() == null ? 0 : work.getLikeCount()) + 1);
        return map;
    }

    /**
     * 把课程实体组装成小程序展示用的 Map。
     *
     * <p>键名即前端契约：{@code price} 取最低有效价格，{@code priceNote} 是直接展示的计价文案，
     * 无有效价格时 price 为 null、priceNote 为“待定价”。</p>
     *
     * @param c 课程实体
     * @return 课程展示 Map
     */
    public Map<String, Object> courseMap(Course c) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", c.getId());
        map.put("name", c.getName());
        map.put("categoryId", c.getCategoryId());
        map.put("categoryName", categoryName(c.getCategoryId()));
        map.put("stageId", c.getStageId());
        map.put("stageName", stageName(c.getStageId()));
        map.put("ageRange", c.getAgeRange());
        map.put("lessons", c.getLessons());
        map.put("frequency", c.getFrequency());
        map.put("durationMinutes", c.getDurationMinutes());
        map.put("coverChar", c.getCoverChar());
        map.put("coverColor", c.getCoverColor());
        map.put("description", c.getDescription());
        map.put("goal", c.getGoal());
        map.put("tags", c.getTags());
        map.put("featured", c.getFeatured());
        map.put("status", c.getStatus());
        CoursePrice price = minPrice(c.getId());
        map.put("price", price == null ? null : price.getPrice());
        // priceNote 把课时数拼成前端直接显示的文案；TIME 计价单位的量词是“次”
        map.put("priceNote", price == null ? "待定价" : price.getLessons() + (price.getUnit().equals("TIME") ? " 次" : " 课时"));
        return map;
    }

    // 展示价优先取整期（TERM）价，没有整期价再取全部有效价格中的最低价
    private CoursePrice minPrice(Long courseId) {
        List<CoursePrice> list = priceMapper.selectList(new LambdaQueryWrapper<CoursePrice>()
                .eq(CoursePrice::getCourseId, courseId).eq(CoursePrice::getStatus, "ON"));
        if (list.isEmpty()) return null;
        List<CoursePrice> terms = list.stream().filter(p -> "TERM".equals(p.getUnit())).toList();
        List<CoursePrice> pool = terms.isEmpty() ? list : terms;
        return pool.stream().filter(p -> p.getPrice() != null)
                .min((a, b) -> a.getPrice().compareTo(b.getPrice())).orElse(null);
    }

    /**
     * 教师展示信息（公开接口与教师端账号信息共用）。
     *
     * @param t 教师档案
     * @return 教师 Map：id/name/title/years/styles/coursesText/bio/quote/tags/avatar/status
     */
    public Map<String, Object> teacherMap(TeacherProfile t) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", t.getId());
        map.put("name", t.getName());
        map.put("title", t.getTitle());
        map.put("years", t.getYears());
        map.put("styles", t.getStyles());
        map.put("coursesText", t.getCoursesText());
        map.put("bio", t.getBio());
        map.put("quote", t.getQuote());
        map.put("tags", t.getTags());
        map.put("avatar", t.getAvatar());
        map.put("status", t.getStatus());
        return map;
    }

    /**
     * 作品展示信息（公开接口与家长成长相册共用）。
     *
     * <p>学员名/班级/教师名由关联表实时补齐，未存冗余字段，避免改名后展示不一致。</p>
     *
     * @param w 作品实体
     * @return 作品 Map
     */
    public Map<String, Object> workMap(Work w) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", w.getId());
        map.put("title", w.getTitle());
        map.put("workType", w.getWorkType());
        map.put("contentChar", w.getContentChar());
        map.put("imageUrl", w.getImageUrl());
        map.put("comment", w.getComment());
        map.put("likeCount", w.getLikeCount());
        map.put("createdAt", w.getCreatedAt());
        Student s = studentMapper.selectById(w.getStudentId());
        map.put("studentName", s == null ? "" : s.getName());
        ClassGroup group = w.getClassId() == null ? null : classGroupMapper.selectById(w.getClassId());
        map.put("className", group == null ? "" : group.getName());
        TeacherProfile t = w.getTeacherId() == null ? null : teacherMapper.selectById(w.getTeacherId());
        map.put("teacherName", t == null ? "" : t.getName());
        // 作品未记录班级时，用学员当前在读班级兜底，保证列表能显示班级名
        if (group == null && s != null) {
            List<ClassStudent> links = classStudentMapper.selectList(new LambdaQueryWrapper<ClassStudent>()
                    .eq(ClassStudent::getStudentId, s.getId()).eq(ClassStudent::getStatus, "ACTIVE").last("limit 1"));
            if (!links.isEmpty()) {
                ClassGroup g = classGroupMapper.selectById(links.get(0).getClassId());
                map.put("className", g == null ? "" : g.getName());
            }
        }
        return map;
    }

    /**
     * 公告展示信息（首页、公告列表、家长工作台共用）。
     *
     * @param n 公告实体
     * @return 公告 Map：id/title/type/content/publishDate
     */
    public Map<String, Object> noticeMap(Notice n) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", n.getId());
        map.put("title", n.getTitle());
        map.put("type", n.getType());
        map.put("content", n.getContent());
        map.put("publishDate", n.getPublishDate());
        return map;
    }

    // 字典名缺失时返回空串，避免前端渲染出 null
    private String categoryName(Long id) {
        CourseCategory c = id == null ? null : categoryMapper.selectById(id);
        return c == null ? "" : c.getName();
    }

    // 同上：阶段名缺失返回空串
    private String stageName(Long id) {
        CourseStage s = id == null ? null : stageMapper.selectById(id);
        return s == null ? "" : s.getName();
    }

    // 课程名缺失返回空串（试听预约可能指向已删除课程）
    private String courseName(Long id) {
        Course c = courseMapper.selectById(id);
        return c == null ? "" : c.getName();
    }
}
