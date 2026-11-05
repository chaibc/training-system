/**
 * 家长账号页：查看账号资料与已绑定学员，提交新学员绑定/解绑申请，退出登录。
 * 接口：GET /api/mini/parent/account；POST /api/mini/parent/bindings；
 *       POST /api/mini/parent/bindings/{id}/unbind
 * 参数：无
 * 需登录：是，且必须是家长（PARENT）
 */

const api = require('../../utils/request');
const auth = require('../../utils/auth');
const view = require('../../utils/view');

Page({
  data: {
    loading: true,
    blocked: false,
    account: null,
    bindings: [],
    addOpen: false,
    mode: 'EXISTING',
    relationIndex: 0,
    relations: [
      { value: 'MOTHER', label: '母亲' },
      { value: 'FATHER', label: '父亲' },
      { value: 'GRANDPARENT', label: '祖辈' },
      { value: 'OTHER', label: '其他' }
    ],
    genders: ['男', '女'],
    form: {
      studentNo: '',
      phoneTail: '',
      studentName: '',
      gender: '',
      birthDate: '',
      school: '',
      grade: ''
    },
    saving: false
  },

  // 家长守卫：未登录/教师账号置 blocked 展示无权限态，不再发请求
  onLoad() {
    if (!auth.requireParent()) {
      this.setData({ blocked: true, loading: false });
      return;
    }
    this.load();
  },

  // 从其他页返回时静默刷新绑定列表；saving 中跳过，避免提交结果被旧数据覆盖
  onShow() {
    if (this.data.account && !this.data.saving) this.load(true);
  },

  // 拉账号与绑定列表；silent=true 时不显示整页 loading（onShow 回刷用）
  async load(silent) {
    if (!silent) this.setData({ loading: true });
    try {
      const account = await api.get('/mini/parent/account');
      account.avatarChar = (account.name || '家').slice(0, 1);
      this.setData({
        account,
        bindings: (account.bindings || []).map(view.binding),
        loading: false
      });
    } catch (error) {
      this.setData({ loading: false });
    }
  },

  // 打开「添加学员」弹层
  openAdd() {
    this.setData({ addOpen: true });
  },

  // 关闭弹层（不清表单，便于用户改完再提交）
  closeAdd() {
    this.setData({ addOpen: false });
  },

  // 切换绑定模式：EXISTING 用学员编号核验已有档案，NEW 登记新学员等待审核
  switchMode(e) {
    this.setData({ mode: e.currentTarget.dataset.mode });
  },

  // 通用字段输入：按 data-field 写入 form 对应键
  onField(e) {
    this.setData({ ['form.' + e.currentTarget.dataset.field]: e.detail.value });
  },

  // 选择与学员的关系（母亲/父亲/祖辈/其他），提交时转成 value
  onRelation(e) {
    this.setData({ relationIndex: Number(e.detail.value) });
  },

  // 选择性别：picker 返回下标，这里存「男/女」原文供后端直接使用
  onGender(e) {
    this.setData({ 'form.gender': this.data.genders[Number(e.detail.value)] });
  },

  // 选择出生日期，格式 yyyy-MM-dd
  onBirth(e) {
    this.setData({ 'form.birthDate': e.detail.value });
  },

  // 提交绑定申请：两种模式各自校验后组装 payload；成功后弹审核提示并刷新列表
  async submitBinding() {
    if (this.data.saving) return;
    const form = this.data.form;
    const relation = this.data.relations[this.data.relationIndex].value;
    let payload;
    if (this.data.mode === 'EXISTING') {
      if (!form.studentNo.trim()) return wx.showToast({ title: '请输入学员编号', icon: 'none' });
      // 学员编号 + 监护人手机号后四位是后端核验归属的凭据，前端先做非空与 4 位数字校验
      if (!/^\d{4}$/.test(form.phoneTail.trim())) return wx.showToast({ title: '请输入监护人手机号后四位', icon: 'none' });
      payload = {
        studentNo: form.studentNo.trim(),
        phoneTail: form.phoneTail.trim(),
        relation
      };
    } else {
      // 新建学员模式只有姓名必填，性别/生日/学校/年级都可后补
      if (!form.studentName.trim()) return wx.showToast({ title: '请输入学员姓名', icon: 'none' });
      payload = {
        newStudent: true,
        studentName: form.studentName.trim(),
        gender: form.gender,
        birthDate: form.birthDate || null,
        school: form.school.trim(),
        grade: form.grade.trim(),
        relation
      };
    }
    this.setData({ saving: true });
    try {
      const result = await api.post('/mini/parent/bindings', payload);
      this.setData({ addOpen: false, saving: false });
      wx.showModal({
        title: '绑定申请已提交',
        content: result.message || '请等待教务审核。',
        showCancel: false,
        success: () => this.load()
      });
    } catch (error) {
      this.setData({ saving: false });
    }
  },

  // 申请解绑：需教务审核，审核期间仍可查看学员，所以只提示「已提交」并刷新状态
  async unbind(e) {
    const id = e.currentTarget.dataset.id;
    const name = e.currentTarget.dataset.name;
    const res = await wx.showModal({
      title: '申请解绑',
      content: '确认申请解除与「' + name + '」的绑定？解绑需教务审核，审核期间仍可查看学员信息。',
      confirmText: '提交申请',
      confirmColor: '#B4473C'
    });
    if (!res.confirm) return;
    try {
      await api.post('/mini/parent/bindings/' + id + '/unbind');
      wx.showToast({ title: '解绑申请已提交', icon: 'success' });
      this.load();
    } catch (error) {
      // request 已提示
    }
  },

  // 退出登录：清凭证与 globalData 后 reLaunch 回首页
  async logout() {
    const res = await wx.showModal({
      title: '退出登录',
      content: '退出后需重新微信登录。',
      confirmText: '退出',
      confirmColor: '#B4473C'
    });
    if (!res.confirm) return;
    auth.clear();
    getApp().globalData.user = null;
    wx.reLaunch({ url: '/pages/home/home' });
  }
});
