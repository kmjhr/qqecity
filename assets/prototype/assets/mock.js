// ========== 模拟数据 ==========

const MockData = {
  // 当前用户
  currentUser: {
    username: 'liuxiaoming',
    nickname: '刘小明',
    avatar: '刘',
    phone: '138****8888',
    email: 'liuxiaoming@example.com',
    realName: '刘小明',
    school: '清华大学',
    graduationDate: '2025-06-30',
    crowdType: '在校大学生',
    crowdTag: 'student'
  },

  // 功能入口
  features: [
    {
      id: 'guarantee',
      title: '安居保函',
      desc: '租房押金零压力，信用保函替你付',
      icon: '🏠',
      gradient: 'gradient-1',
      url: 'guarantee.html'
    },
    {
      id: 'loan',
      title: '青创e贷',
      desc: '青年创业授信，轻资产也能启航',
      icon: '🚀',
      gradient: 'gradient-2',
      url: 'loan.html'
    },
    {
      id: 'budget',
      title: '预算消费',
      desc: '碎片消费治理，心愿储蓄计划',
      icon: '💰',
      gradient: 'gradient-3',
      url: 'budget.html'
    },
    {
      id: 'safety',
      title: '金融安全',
      desc: '反诈防骗守护，征信知识科普',
      icon: '🛡️',
      gradient: 'gradient-4',
      url: 'safety.html'
    },
    {
      id: 'bookkeeping',
      title: '经营赋能',
      desc: '创业经营记账，现金流分析助手',
      icon: '📊',
      gradient: 'gradient-5',
      url: 'bookkeeping.html'
    },
    {
      id: 'message',
      title: '消息中心',
      desc: '业务通知、预算提醒、系统公告',
      icon: '📬',
      gradient: 'gradient-6',
      url: 'message.html'
    }
  ],

  // 近期动态
  news: [
    { title: '您的安居保函申请已通过审核，待缴费', time: '2小时前', unread: true },
    { title: '本月餐饮消费已达预算的80%，请注意控制', time: '昨天', unread: true },
    { title: '青创e贷B类预审结果已出，点击查看额度', time: '3天前', unread: false }
  ],

  // 保函列表
  guarantees: [
    {
      id: 'BH202509150001',
      address: '北京市海淀区中关村大街1号院3号楼501室',
      amount: 8000,
      status: 'active',
      statusText: '已开立',
      startDate: '2025-09-15',
      endDate: '2026-09-14',
      currentStep: 3
    },
    {
      id: 'BH202509200002',
      address: '北京市朝阳区建国路88号院2号楼302室',
      amount: 6500,
      status: 'payment',
      statusText: '待缴费',
      startDate: '2025-09-20',
      endDate: '2026-09-19',
      currentStep: 2
    },
    {
      id: 'BH202508100003',
      address: '北京市西城区金融街15号1号楼101室',
      amount: 5000,
      status: 'expired',
      statusText: '已失效',
      startDate: '2024-08-10',
      endDate: '2025-08-09',
      currentStep: 4
    }
  ],

  // 保函状态流转
  guaranteeSteps: ['申请中', '待确认', '待缴费', '已开立', '已失效'],

  // 授信额度
  creditInfo: {
    totalLimit: 150000,
    usedLimit: 45000,
    availableLimit: 105000,
    usedPercent: 30
  },

  // 还款记录
  repayRecords: [
    { id: 1, amount: 5000, date: '2025-09-10', status: '已还清', purpose: '设备采购' },
    { id: 2, amount: 8000, date: '2025-08-15', status: '已还清', purpose: '店铺租金' },
    { id: 3, amount: 12000, date: '2025-07-20', status: '已还清', purpose: '进货备货' }
  ],

  // 预算数据
  budgetInfo: {
    totalBudget: 5000,
    spent: 3200,
    remaining: 1800,
    percent: 64
  },

  // 分类预算
  categoryBudgets: [
    { name: '餐饮美食', icon: '🍜', budget: 1500, spent: 1200, color: '#fa709a' },
    { name: '交通出行', icon: '🚇', budget: 500, spent: 280, color: '#4facfe' },
    { name: '购物消费', icon: '🛍️', budget: 1000, spent: 850, color: '#f093fb' },
    { name: '休闲娱乐', icon: '🎮', budget: 600, spent: 350, color: '#43e97b' },
    { name: '生活缴费', icon: '💡', budget: 800, spent: 420, color: '#fee140' },
    { name: '学习充电', icon: '📚', budget: 600, spent: 100, color: '#667eea' }
  ],

  // 心愿储蓄
  savingsGoals: [
    { name: '毕业旅行', target: 5000, saved: 2800, percent: 56 },
    { name: '新电脑', target: 8000, saved: 3200, percent: 40 },
    { name: '创业启动金', target: 20000, saved: 6500, percent: 32.5 }
  ],

  // 反诈文章
  antiFraudArticles: [
    {
      id: 1,
      title: '警惕校园贷陷阱：这些套路你必须知道',
      summary: '近年来，不良校园贷在高校中滋生蔓延，不少学生因此背负巨额债务...',
      cover: '🎓',
      coverClass: 'cover-1',
      views: 12580,
      category: '反诈教学'
    },
    {
      id: 2,
      title: '刷单返利是骗局！手把手教你识别',
      summary: '"足不出户，日赚斗金"、"轻松刷单，月入过万"，你是否收到过这样的消息...',
      cover: '💸',
      coverClass: 'cover-2',
      views: 9876,
      category: '反诈教学'
    },
    {
      id: 3,
      title: '冒充公检法诈骗的典型套路分析',
      summary: '"你好，我是XX公安局的，你涉嫌洗钱犯罪，请配合调查..."接到这样的电话要警惕...',
      cover: '⚖️',
      coverClass: 'cover-3',
      views: 8456,
      category: '反诈教学'
    },
    {
      id: 4,
      title: '个人信息保护指南：守护你的金融安全',
      summary: '身份证号、银行卡号、手机号、验证码...这些信息如果泄露，可能导致严重后果...',
      cover: '🔒',
      coverClass: 'cover-4',
      views: 7234,
      category: '反诈教学'
    },
    {
      id: 5,
      title: '投资理财诈骗大盘点：高收益背后的陷阱',
      summary: '"稳赚不赔"、"高收益零风险"、"内幕消息"...面对这些诱人承诺，一定要保持清醒...',
      cover: '📈',
      coverClass: 'cover-5',
      views: 6543,
      category: '反诈教学'
    },
    {
      id: 6,
      title: '电信网络诈骗常见手法与防范措施',
      summary: '电信网络诈骗手法层出不穷，了解常见手法，提高防范意识，保护好自己的钱袋子...',
      cover: '📱',
      coverClass: 'cover-6',
      views: 5678,
      category: '反诈教学'
    }
  ],

  // 征信信息
  creditScore: {
    score: 785,
    level: '优秀',
    maxScore: 850
  },

  // 经营数据
  businessStats: {
    income: 28600,
    expense: 15200,
    profit: 13400
  },

  // 现金流图表数据
  cashFlowData: [
    { month: '4月', income: 22000, expense: 18000 },
    { month: '5月', income: 25000, expense: 16000 },
    { month: '6月', income: 28600, expense: 15200 },
    { month: '7月', income: 31000, expense: 17500 },
    { month: '8月', income: 29500, expense: 19000 },
    { month: '9月', income: 28600, expense: 15200 }
  ],

  // 记账记录
  bookkeepingRecords: [
    { id: 1, type: 'income', title: '产品销售收入', amount: 8500, date: '2025-09-20', category: '销售收入' },
    { id: 2, type: 'expense', title: '店铺租金', amount: 5000, date: '2025-09-18', category: '房租' },
    { id: 3, type: 'income', title: '服务咨询费', amount: 3200, date: '2025-09-15', category: '服务收入' },
    { id: 4, type: 'expense', title: '进货成本', amount: 4500, date: '2025-09-12', category: '采购' },
    { id: 5, type: 'income', title: '产品销售收入', amount: 6800, date: '2025-09-08', category: '销售收入' },
    { id: 6, type: 'expense', title: '推广费用', amount: 1200, date: '2025-09-05', category: '营销' }
  ],

  // 消息列表
  messages: [
    {
      id: 1,
      type: 'business',
      iconType: 'business',
      title: '安居保函申请已通过审核',
      desc: '您的保函申请(BH202509200002)已通过房东确认，请及时完成缴费',
      time: '2小时前',
      unread: true,
      category: '业务通知'
    },
    {
      id: 2,
      type: 'budget',
      iconType: 'budget',
      title: '餐饮消费预算提醒',
      desc: '本月餐饮消费已达预算的80%，建议合理控制后续支出',
      time: '昨天 18:30',
      unread: true,
      category: '预算提醒'
    },
    {
      id: 3,
      type: 'business',
      iconType: 'business',
      title: '青创e贷预审结果通知',
      desc: '您的B类免费预审已完成，预估额度区间为8-15万元，点击查看详情',
      time: '3天前',
      unread: true,
      category: '业务通知'
    },
    {
      id: 4,
      type: 'system',
      iconType: 'system',
      title: '系统安全升级通知',
      desc: '为保障您的账户安全，系统将于本周六凌晨进行安全升级，届时服务将短暂中断',
      time: '5天前',
      unread: false,
      category: '系统通知'
    },
    {
      id: 5,
      type: 'budget',
      iconType: 'budget',
      title: '月度预算报告已生成',
      desc: '8月消费报告已出炉，您的总支出较上月下降12%，继续保持！',
      time: '1周前',
      unread: false,
      category: '预算提醒'
    },
    {
      id: 6,
      type: 'security',
      iconType: 'security',
      title: '金融安全知识推送',
      desc: '【反诈提醒】近期冒充客服退款诈骗高发，请提高警惕，切勿轻易转账',
      time: '1周前',
      unread: false,
      category: '系统通知'
    },
    {
      id: 7,
      type: 'business',
      iconType: 'business',
      title: '保函即将到期提醒',
      desc: '您的保函(BH202408100003)将于2025-08-09到期，请及时关注续租事宜',
      time: '2周前',
      unread: false,
      category: '业务通知'
    },
    {
      id: 8,
      type: 'system',
      iconType: 'system',
      title: '注册成功欢迎通知',
      desc: '欢迎加入青启e城！您已获得新人专属权益，点击查看',
      time: '1个月前',
      unread: false,
      category: '系统通知'
    }
  ]
};

// ========== 通用工具函数 ==========

// Tab 切换
function initTabs(tabContainerSelector) {
  const containers = document.querySelectorAll(tabContainerSelector);
  containers.forEach(container => {
    const tabItems = container.querySelectorAll('.tab-item');
    const tabContents = container.querySelectorAll('.tab-content');
    
    tabItems.forEach((tab, index) => {
      tab.addEventListener('click', () => {
        tabItems.forEach(t => t.classList.remove('active'));
        tabContents.forEach(c => c.classList.remove('active'));
        tab.classList.add('active');
        if (tabContents[index]) {
          tabContents[index].classList.add('active');
        }
      });
    });
  });
}

// 设置当前页面导航高亮
function setActiveNav(pageId) {
  const navLinks = document.querySelectorAll('.nav-menu a');
  navLinks.forEach(link => {
    if (link.dataset.page === pageId) {
      link.classList.add('active');
    }
  });
}

// 格式化金额
function formatMoney(num) {
  return num.toLocaleString('zh-CN');
}

// 环形进度条计算
function setRingProgress(percent) {
  const circumference = 2 * Math.PI * 70; // r=70
  const offset = circumference - (percent / 100) * circumference;
  return offset;
}
