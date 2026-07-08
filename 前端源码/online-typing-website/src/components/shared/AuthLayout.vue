<template>
  <div class="auth-page">
    <!-- 左侧品牌区 -->
    <div class="auth-brand">
      <div class="brand-bg">
        <div class="bg-orb orb-1"></div>
        <div class="bg-orb orb-2"></div>
        <div class="bg-orb orb-3"></div>
        <div class="bg-grid"></div>
      </div>

      <div class="brand-content">
        <div class="brand-header">
          <div class="logo-mark">
            <svg width="44" height="44" viewBox="0 0 44 44" fill="none">
              <rect width="44" height="44" rx="12" fill="white" fill-opacity="0.15"/>
              <path d="M13 15h18M13 22h12M13 29h15" stroke="white" stroke-width="2.5" stroke-linecap="round"/>
            </svg>
          </div>
          <h1 class="brand-title">Define.XML</h1>
          <p class="brand-subtitle">Clinical Data Standards Platform</p>
        </div>

        <div class="feature-cards">
          <div v-for="(feat, i) in features" :key="i" class="feature-card">
            <div class="feature-icon">
              <slot :name="`feature-icon-${i}`">
                <svg width="22" height="22" viewBox="0 0 22 22" fill="none">
                  <path d="M11 2L13.09 8.26L20 9.27L15 13.97L16.18 20.72L11 17.77L5.82 20.72L7 13.97L2 9.27L8.91 8.26L11 2Z" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </slot>
            </div>
            <div class="feature-text">
              <span class="feature-name">{{ feat.title }}</span>
              <span class="feature-desc">{{ feat.desc }}</span>
            </div>
          </div>
        </div>

        <p class="brand-slogan">" 让临床研究数据管理更简单、更高效 "</p>
      </div>
    </div>

    <!-- 右侧表单区 -->
    <div class="auth-form-side">
      <div class="form-container">
        <slot />
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  features: {
    type: Array,
    default: () => []
  }
})
</script>

<style scoped lang="less">
.auth-page {
  display: flex;
  min-height: 100vh;
}

/* ===== 左侧品牌区 ===== */
.auth-brand {
  width: 50%;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: linear-gradient(160deg, #1e1b4b 0%, #312e81 40%, #4338ca 100%);
}

.brand-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;

  .bg-orb {
    position: absolute;
    border-radius: 50%;
    filter: blur(100px);
  }

  .orb-1 {
    width: 600px;
    height: 600px;
    background: rgba(99, 102, 241, 0.35);
    top: -20%;
    left: -15%;
    animation: drift 12s ease-in-out infinite;
  }

  .orb-2 {
    width: 400px;
    height: 400px;
    background: rgba(129, 140, 248, 0.25);
    bottom: -10%;
    right: -10%;
    animation: drift 15s ease-in-out infinite reverse;
  }

  .orb-3 {
    width: 300px;
    height: 300px;
    background: rgba(165, 180, 252, 0.2);
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    animation: drift 18s ease-in-out infinite 3s;
  }

  .bg-grid {
    position: absolute;
    inset: 0;
    background-image:
      linear-gradient(rgba(255,255,255,0.03) 1px, transparent 1px),
      linear-gradient(90deg, rgba(255,255,255,0.03) 1px, transparent 1px);
    background-size: 40px 40px;
  }
}

@keyframes drift {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(30px, -25px) scale(1.05); }
  66% { transform: translate(-20px, 20px) scale(0.95); }
}

.brand-content {
  position: relative;
  z-index: 1;
  max-width: 420px;
  padding: 0 40px;
}

.brand-header {
  margin-bottom: 48px;

  .logo-mark {
    margin-bottom: 24px;
  }

  .brand-title {
    font-size: 36px;
    font-weight: 800;
    color: #fff;
    letter-spacing: -0.03em;
    margin: 0 0 8px;
  }

  .brand-subtitle {
    font-size: 15px;
    color: rgba(255, 255, 255, 0.55);
    letter-spacing: 0.02em;
    margin: 0;
  }
}

.feature-cards {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 48px;
}

.feature-card {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 14px;
  padding: 18px 20px;
  backdrop-filter: blur(8px);
  transition: background 0.3s, border-color 0.3s;

  &:hover {
    background: rgba(255, 255, 255, 0.12);
    border-color: rgba(255, 255, 255, 0.18);
  }

  .feature-icon {
    width: 42px;
    height: 42px;
    border-radius: 10px;
    background: rgba(255, 255, 255, 0.1);
    display: flex;
    align-items: center;
    justify-content: center;
    color: rgba(255, 255, 255, 0.9);
    flex-shrink: 0;
  }

  .feature-text {
    display: flex;
    flex-direction: column;
    gap: 4px;

    .feature-name {
      font-size: 15px;
      font-weight: 600;
      color: #fff;
    }

    .feature-desc {
      font-size: 13px;
      color: rgba(255, 255, 255, 0.6);
      line-height: 1.5;
    }
  }
}

.brand-slogan {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.4);
  text-align: center;
  margin: 0;
  font-style: italic;
}

/* ===== 右侧表单区 ===== */
.auth-form-side {
  width: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--saas-bg-page);
  padding: 40px;
}

.form-container {
  width: 100%;
  max-width: 400px;

  :deep(h2) {
    font-size: 28px;
    font-weight: 700;
    color: var(--saas-primary);
    margin: 0 0 8px;
    letter-spacing: -0.02em;
  }

  :deep(.subtitle) {
    font-size: 14px;
    color: var(--saas-text-secondary);
    margin: 0 0 36px;
  }
}

@media (max-width: 1024px) {
  .auth-page {
    flex-direction: column;
  }

  .auth-brand {
    width: 100%;
    min-height: 280px;
    padding: 40px 20px;
  }

  .brand-content {
    max-width: 100%;
    padding: 0 20px;
  }

  .feature-cards {
    display: none;
  }

  .brand-slogan {
    display: none;
  }

  .auth-form-side {
    width: 100%;
    flex: 1;
    padding: 32px 20px;
  }
}
</style>
