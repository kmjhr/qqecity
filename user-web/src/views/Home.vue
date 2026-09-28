<template>
  <div class="home-page">
    <!-- 官网感 Hero 区域 -->
    <section class="hero-section">
      <div class="hero-bg">
        <div class="mesh mesh-1"></div>
        <div class="mesh mesh-2"></div>
        <div class="mesh mesh-3"></div>
        <div class="grid-overlay"></div>
      </div>

      <div class="hero-content">
        <div class="hero-left">
          <h1 class="hero-title">
            让青春在城市
            <span class="gradient-text">轻启金融可能</span>
          </h1>
          <p class="hero-desc">
            青启e城为青年租房、创业、消费、经营提供一站式智能金融服务。
            保函、信贷、预算、风控、经营赋能，安全可信赖。
          </p>
          <div class="hero-actions">
            <el-button type="primary" size="large" round @click="$router.push('/guarantee')">
              <el-icon><Wallet /></el-icon>
              <span>安居保函</span>
            </el-button>
            <el-button size="large" round plain @click="$router.push('/loan')">
              <el-icon><Money /></el-icon>
              <span>青创e贷</span>
            </el-button>
          </div>
          <div class="hero-stats">
            <div class="stat-item">
              <div class="stat-num">6</div>
              <div class="stat-label">核心服务</div>
            </div>
            <div class="stat-item">
              <div class="stat-num">3</div>
              <div class="stat-label">金融场景</div>
            </div>
            <div class="stat-item">
              <div class="stat-num">1</div>
              <div class="stat-label">安全账户</div>
            </div>
          </div>
        </div>

        <div class="hero-right">
          <div class="hero-card hero-card-main">
            <div class="card-glow"></div>
            <div class="card-head">
              <div class="card-dots">
                <span></span><span></span><span></span>
              </div>
              <div class="card-title">青启e城</div>
            </div>
            <div class="card-body">
              <div class="mini-stat">
                <div class="mini-stat-icon blue"><el-icon><Wallet /></el-icon></div>
                <div>
                  <div class="mini-stat-label">保函额度</div>
                  <div class="mini-stat-value">¥ 8,000</div>
                </div>
              </div>
              <div class="mini-stat">
                <div class="mini-stat-icon green"><el-icon><Money /></el-icon></div>
                <div>
                  <div class="mini-stat-label">授信额度</div>
                  <div class="mini-stat-value">¥ 50,000</div>
                </div>
              </div>
              <div class="mini-chart">
                <div class="bar" style="height: 40%"></div>
                <div class="bar" style="height: 65%"></div>
                <div class="bar" style="height: 45%"></div>
                <div class="bar active" style="height: 85%"></div>
                <div class="bar" style="height: 60%"></div>
              </div>
            </div>
          </div>

          <div class="floating-badges">
            <div class="float-badge badge-1">
              <el-icon><Shield /></el-icon>
              <span>金融安全守护</span>
            </div>
            <div class="float-badge badge-2">
              <el-icon><DataLine /></el-icon>
              <span>AI 智能风控</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 核心服务入口 -->
    <section class="features-section">
      <div class="section-header">
        <div class="section-tag">核心服务</div>
        <h2 class="section-title">为青年城市生活量身打造</h2>
        <p class="section-desc">覆盖安居、创业、消费、安全、经营全场景，让金融服务像生活一样简单</p>
      </div>

      <div class="feature-grid">
        <div
          v-for="item in features"
          :key="item.path"
          class="feature-card"
          @click="$router.push(item.path)"
        >
          <div class="feature-top" :style="{ '--card-color': item.color }">
            <div class="feature-icon">
              <AppIcons :name="item.iconKey" set="flat" :size="52" />
            </div>
            <el-icon class="arrow-icon"><ArrowRight /></el-icon>
          </div>
          <h3 class="feature-title">{{ item.title }}</h3>
          <p class="feature-desc">{{ item.desc }}</p>
          <div class="feature-tags">
            <el-tag v-for="tag in item.tags" :key="tag" size="small" effect="plain" round>
              {{ tag }}
            </el-tag>
          </div>
        </div>
      </div>
    </section>

    <!-- 价值亮点 -->
    <section class="highlights-section">
      <div class="highlight-card">
        <div class="highlight-icon blue"><el-icon><ChatLineSquare /></el-icon></div>
        <h3>智能中台</h3>
        <p>AI 政策匹配、信用画像、风险预警、反诈教学，全流程智能化陪伴</p>
      </div>
      <div class="highlight-card">
        <div class="highlight-icon green"><el-icon><Lock /></el-icon></div>
        <h3>安全可信</h3>
        <p>模拟银行级风控，敏感操作多重校验，关键金融能力明确标注</p>
      </div>
      <div class="highlight-card">
        <div class="highlight-icon orange"><el-icon><Pointer /></el-icon></div>
        <h3>极简体验</h3>
        <p>一个账户串联租房、创业、消费、经营，线上申请一键直达</p>
      </div>
    </section>

    <!-- 底部 CTA -->
    <section class="cta-section">
      <div class="cta-bg">
        <div class="cta-blob blob-a"></div>
        <div class="cta-blob blob-b"></div>
      </div>
      <div class="cta-content">
        <h2>准备好开启你的城市金融新生活？</h2>
        <p>登录即可体验安居保函、青创e贷、预算消费等全部能力</p>
        <div class="cta-actions">
          <el-button type="primary" size="large" round @click="$router.push('/budget')">
            <el-icon><PieChart /></el-icon>
            <span>开始预算管理</span>
          </el-button>
          <el-button size="large" round @click="$router.push('/policy')">
            <el-icon><Document /></el-icon>
            <span>匹配扶持政策</span>
          </el-button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { useUserStore } from '@/store/user'
import AppIcons from '@/components/AppIcons.vue'

const userStore = useUserStore()

// 业务模块入口配置，对齐《需求清单》三大场景 + 金融安全
// 仅视觉样式调整，未改动标题/描述/路由/字段
const features = [
  {
    title: '安居保函',
    desc: '租房履约保函，降低租房押金压力',
    iconKey: 'guarantee',
    path: '/guarantee',
    color: '#764ba2',
    tags: ['押金替代', '电子签约']
  },
  {
    title: '青创e贷',
    desc: '青年创业智能授信，助力城市轻创业',
    iconKey: 'loan',
    path: '/loan',
    color: '#f5576c',
    tags: ['A/B 双轨', '受托支付']
  },
  {
    title: '预算消费',
    desc: '分类预算管理，结余一键转储蓄',
    iconKey: 'budget',
    path: '/budget',
    color: '#00c6fb',
    tags: ['MCC 归类', '三级提醒']
  },
  {
    title: '金融安全',
    desc: '反诈财商教学，骗局智能甄别',
    iconKey: 'safety',
    path: '/safety',
    color: '#f39c12',
    tags: ['征信解读', '情景教学']
  },
  {
    title: '经营赋能',
    desc: 'AI 简易记账，现金流风险预警',
    iconKey: 'business',
    path: '/bookkeeping',
    color: '#11998e',
    tags: ['现金流', '保险代销']
  },
  {
    title: '消息中心',
    desc: '预算提醒、业务通知，一览无余',
    iconKey: 'business',
    path: '/message',
    color: '#0ea5e9',
    tags: ['站内信', '风险预警']
  }
]
</script>

<style scoped>
.home-page {
  max-width: 1280px;
  margin: 0 auto;
  padding-bottom: 40px;
}

/* Hero Section */
.hero-section {
  position: relative;
  border-radius: var(--qq-radius-xl);
  overflow: hidden;
  background: linear-gradient(135deg, #ffffff 0%, #f0f9ff 100%);
  border: 1px solid var(--qq-border);
  box-shadow: var(--qq-shadow-lg);
  margin-bottom: 40px;
  min-height: 520px;
  display: flex;
  align-items: center;
}

.hero-bg {
  position: absolute;
  inset: 0;
  overflow: hidden;
  border-radius: var(--qq-radius-xl);
  pointer-events: none;
}

.mesh {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  opacity: 0.45;
}

.mesh-1 {
  width: 360px;
  height: 360px;
  background: #bae6fd;
  top: -120px;
  right: -80px;
}

.mesh-2 {
  width: 280px;
  height: 280px;
  background: #a7f3d0;
  bottom: -80px;
  left: 10%;
}

.mesh-3 {
  width: 180px;
  height: 180px;
  background: #fed7aa;
  top: 30%;
  left: -60px;
}

.grid-overlay {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(14, 165, 233, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(14, 165, 233, 0.04) 1px, transparent 1px);
  background-size: 40px 40px;
}

.hero-content {
  position: relative;
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: 48px;
  align-items: center;
  padding: 56px 64px;
  width: 100%;
}

.hero-title {
  font-size: 44px;
  line-height: 1.2;
  font-weight: 800;
  color: var(--qq-text);
  margin: 0 0 18px;
  letter-spacing: -0.5px;
}

.gradient-text {
  background: var(--qq-gradient-hero);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-desc {
  font-size: 16px;
  line-height: 1.7;
  color: var(--qq-text-secondary);
  max-width: 520px;
  margin: 0 0 28px;
}

.hero-actions {
  display: flex;
  gap: 14px;
  margin-bottom: 36px;
}

.hero-actions .el-button {
  padding: 0 28px;
  font-weight: 600;
}

.hero-actions .el-icon {
  margin-right: 6px;
}

.hero-stats {
  display: flex;
  gap: 40px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-num {
  font-size: 32px;
  font-weight: 800;
  background: var(--qq-gradient-hero);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  line-height: 1;
}

.stat-label {
  font-size: 13px;
  color: var(--qq-text-secondary);
}

/* Hero Right */
.hero-right {
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 360px;
}

.hero-card-main {
  position: relative;
  width: 320px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(12px);
  border-radius: var(--qq-radius-xl);
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 24px 50px rgba(14, 165, 233, 0.18);
  padding: 22px;
  z-index: 2;
}

.card-glow {
  position: absolute;
  inset: -1px;
  border-radius: var(--qq-radius-xl);
  background: linear-gradient(135deg, rgba(14, 165, 233, 0.3), rgba(16, 185, 129, 0.2));
  z-index: -1;
  filter: blur(8px);
  opacity: 0.6;
}

.card-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
}

.card-dots {
  display: flex;
  gap: 6px;
}

.card-dots span {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #e2e8f0;
}

.card-dots span:nth-child(1) { background: #f87171; }
.card-dots span:nth-child(2) { background: #fbbf24; }
.card-dots span:nth-child(3) { background: #34d399; }

.card-title {
  font-weight: 700;
  font-size: 15px;
  color: var(--qq-text);
}

.mini-stat {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  background: #f8fafc;
  border-radius: var(--qq-radius-md);
  margin-bottom: 12px;
}

.mini-stat-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
}

.mini-stat-icon.blue { background: linear-gradient(135deg, #0ea5e9, #38bdf8); }
.mini-stat-icon.green { background: linear-gradient(135deg, #10b981, #34d399); }

.mini-stat-label {
  font-size: 12px;
  color: var(--qq-text-muted);
  margin-bottom: 2px;
}

.mini-stat-value {
  font-size: 16px;
  font-weight: 700;
  color: var(--qq-text);
}

.mini-chart {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 8px;
  height: 70px;
  padding: 12px;
  background: #f8fafc;
  border-radius: var(--qq-radius-md);
}

.bar {
  flex: 1;
  background: #e2e8f0;
  border-radius: 4px;
  transition: all 0.3s;
}

.bar.active {
  background: linear-gradient(180deg, #0ea5e9, #10b981);
}

.floating-badges {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.float-badge {
  position: absolute;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: var(--qq-radius-full);
  box-shadow: var(--qq-shadow-lg);
  font-size: 13px;
  font-weight: 600;
  color: var(--qq-text);
  z-index: 3;
}

.float-badge .el-icon {
  color: var(--qq-primary);
}

.badge-1 {
  top: 20px;
  right: 0;
  animation: float 4s ease-in-out infinite;
}

.badge-2 {
  bottom: 30px;
  left: 0;
  animation: float 4s ease-in-out infinite 1s;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

/* Features Section */
.features-section {
  margin-bottom: 48px;
}

.section-header {
  text-align: center;
  margin-bottom: 32px;
}

.section-tag {
  display: inline-block;
  padding: 6px 14px;
  background: var(--qq-primary-light);
  color: var(--qq-primary-dark);
  border-radius: var(--qq-radius-full);
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 12px;
}

.section-title {
  font-size: 28px;
  font-weight: 800;
  color: var(--qq-text);
  margin: 0 0 10px;
}

.section-desc {
  font-size: 15px;
  color: var(--qq-text-secondary);
  margin: 0;
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}

.feature-card {
  background: var(--qq-surface);
  border: 1px solid var(--qq-border);
  border-radius: var(--qq-radius-xl);
  padding: 28px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.feature-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: var(--card-color);
  opacity: 0.8;
  transition: height 0.3s;
}

.feature-card:hover {
  transform: translateY(-6px);
  box-shadow: var(--qq-shadow-xl);
}

.feature-card:hover::before {
  height: 6px;
}

.feature-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 18px;
}

.feature-icon {
  border-radius: var(--qq-radius-md);
}

.arrow-icon {
  color: var(--qq-text-muted);
  transition: all 0.3s;
}

.feature-card:hover .arrow-icon {
  color: var(--card-color);
  transform: translateX(4px);
}

.feature-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--qq-text);
  margin: 0 0 8px;
}

.feature-desc {
  font-size: 14px;
  color: var(--qq-text-secondary);
  line-height: 1.6;
  margin: 0 0 16px;
}

.feature-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.feature-tags .el-tag {
  color: var(--qq-text-secondary);
  border-color: var(--qq-border);
}

/* Highlights */
.highlights-section {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
  margin-bottom: 48px;
}

.highlight-card {
  background: var(--qq-surface);
  border: 1px solid var(--qq-border);
  border-radius: var(--qq-radius-xl);
  padding: 28px;
  text-align: center;
  transition: all 0.3s;
}

.highlight-card:hover {
  box-shadow: var(--qq-shadow-md);
}

.highlight-icon {
  width: 60px;
  height: 60px;
  border-radius: var(--qq-radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
  font-size: 26px;
  color: #fff;
}

.highlight-icon.blue { background: linear-gradient(135deg, #0ea5e9, #38bdf8); }
.highlight-icon.green { background: linear-gradient(135deg, #10b981, #34d399); }
.highlight-icon.orange { background: linear-gradient(135deg, #f97316, #fbbf24); }

.highlight-card h3 {
  font-size: 17px;
  font-weight: 700;
  color: var(--qq-text);
  margin: 0 0 8px;
}

.highlight-card p {
  font-size: 14px;
  color: var(--qq-text-secondary);
  line-height: 1.6;
  margin: 0;
}

/* CTA Section */
.cta-section {
  position: relative;
  border-radius: var(--qq-radius-xl);
  overflow: hidden;
  background: linear-gradient(135deg, #0ea5e9 0%, #10b981 100%);
  padding: 56px;
  text-align: center;
  color: #fff;
}

.cta-bg {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
}

.cta-blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(50px);
  opacity: 0.35;
  background: #fff;
}

.blob-a {
  width: 300px;
  height: 300px;
  top: -100px;
  left: -80px;
}

.blob-b {
  width: 240px;
  height: 240px;
  bottom: -80px;
  right: -60px;
}

.cta-content {
  position: relative;
  z-index: 1;
}

.cta-content h2 {
  font-size: 28px;
  font-weight: 800;
  margin: 0 0 10px;
}

.cta-content p {
  font-size: 15px;
  opacity: 0.92;
  margin: 0 0 28px;
}

.cta-actions {
  display: flex;
  justify-content: center;
  gap: 14px;
}

.cta-actions .el-button--primary {
  background: #fff;
  color: var(--qq-primary);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
}

.cta-actions .el-button--primary:hover {
  background: #f0f9ff;
}

.cta-actions .el-button:not(.el-button--primary) {
  background: rgba(255, 255, 255, 0.18);
  border: 1px solid rgba(255, 255, 255, 0.4);
  color: #fff;
}

.cta-actions .el-button:not(.el-button--primary):hover {
  background: rgba(255, 255, 255, 0.28);
}

/* Responsive */
@media (max-width: 1024px) {
  .hero-content {
    grid-template-columns: 1fr;
    padding: 40px;
  }

  .hero-right {
    order: -1;
    min-height: 300px;
  }

  .feature-grid,
  .highlights-section {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .home-page {
    padding-bottom: 24px;
  }

  .hero-section {
    border-radius: var(--qq-radius-lg);
    margin-bottom: 24px;
  }

  .hero-content {
    padding: 28px 24px;
  }

  .hero-title {
    font-size: 32px;
  }

  .hero-actions {
    flex-direction: column;
  }

  .hero-actions .el-button {
    width: 100%;
  }

  .hero-stats {
    gap: 24px;
  }

  .hero-card-main {
    width: 100%;
    max-width: 320px;
  }

  .badge-1 {
    right: -10px;
    top: 0;
  }

  .badge-2 {
    left: -10px;
    bottom: 10px;
  }

  .feature-grid,
  .highlights-section {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .cta-section {
    padding: 36px 24px;
  }

  .cta-actions {
    flex-direction: column;
  }

  .cta-actions .el-button {
    width: 100%;
  }
}
</style>
