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

  onLoad() {
    if (!auth.requireParent()) {
      this.setData({ blocked: true, loading: false });
      return;
    }
    this.load();
  },

  onShow() {
    if (this.data.account && !this.data.saving) this.load(true);
  },

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

  openAdd() {
    this.setData({ addOpen: true });
  },

  closeAdd() {
    this.setData({ addOpen: false });
  },

  switchMode(e) {
    this.setData({ mode: e.currentTarget.dataset.mode });
  },

  onField(e) {
    this.setData({ ['form.' + e.currentTarget.dataset.field]: e.detail.value });
  },

  onRelation(e) {
    this.setData({ relationIndex: Number(e.detail.value) });
  },

  onGender(e) {
    this.setData({ 'form.gender': this.data.genders[Number(e.detail.value)] });
  },

  onBirth(e) {
    this.setData({ 'form.birthDate': e.detail.value });
  },

  async submitBinding() {
    if (this.data.saving) return;
    const form = this.data.form;
    const relation = this.data.relations[this.data.relationIndex].value;
    let payload;
    if (this.data.mode === 'EXISTING') {
      if (!form.studentNo.trim()) return wx.showToast({ title: '请输入学员编号', icon: 'none' });
      if (!/^\d{4}$/.test(form.phoneTail.trim())) return wx.showToast({ title: '请输入监护人手机号后四位', icon: 'none' });
      payload = {
        studentNo: form.studentNo.trim(),
        phoneTail: form.phoneTail.trim(),
        relation
      };
    } else {
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
