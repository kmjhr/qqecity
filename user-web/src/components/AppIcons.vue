<template>
  <!--
    青启e城 业务模块图标组件
    三套风格：gradient / outline / flat
    使用示例：
      <AppIcons name="loan" set="gradient" size="64" />
  -->
  <svg v-if="current === 'gradient-guarantee'" :width="size" :height="size" viewBox="0 0 64 64">
    <defs><linearGradient id="g" x1="0%" y1="0%" x2="100%" y2="100%"><stop offset="0%" stop-color="#667eea"/><stop offset="100%" stop-color="#764ba2"/></linearGradient></defs>
    <rect width="64" height="64" rx="16" fill="url(#g)"/>
    <path d="M20 28 L32 20 L44 28 V36 C44 44 32 50 32 50 C32 50 20 44 20 36 V28Z" fill="rgba(255,255,255,0.95)"/>
    <path d="M26 32 L30 36 L38 28" fill="none" stroke="#764ba2" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
    <path d="M14 28 L32 16 L50 28" fill="none" stroke="rgba(255,255,255,0.7)" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
  </svg>

  <svg v-else-if="current === 'gradient-loan'" :width="size" :height="size" viewBox="0 0 64 64">
    <defs><linearGradient id="l" x1="0%" y1="0%" x2="100%" y2="100%"><stop offset="0%" stop-color="#f093fb"/><stop offset="100%" stop-color="#f5576c"/></linearGradient></defs>
    <rect width="64" height="64" rx="16" fill="url(#l)"/>
    <circle cx="46" cy="44" r="10" fill="rgba(255,255,255,0.95)"/>
    <text x="46" y="48" text-anchor="middle" fill="#f5576c" font-size="12" font-weight="700">¥</text>
    <path d="M32 46 C24 46 20 38 22 30 C24 24 28 18 32 12 C36 18 40 24 42 30 C44 38 40 46 32 46Z" fill="rgba(255,255,255,0.95)"/>
    <path d="M32 22 V36 M32 22 L28 26 M32 22 L36 26" stroke="#f5576c" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" fill="none"/>
  </svg>

  <svg v-else-if="current === 'gradient-business'" :width="size" :height="size" viewBox="0 0 64 64">
    <defs><linearGradient id="b" x1="0%" y1="0%" x2="100%" y2="100%"><stop offset="0%" stop-color="#43e97b"/><stop offset="100%" stop-color="#38f9d7"/></linearGradient></defs>
    <rect width="64" height="64" rx="16" fill="url(#b)"/>
    <rect x="14" y="20" width="36" height="30" rx="4" fill="rgba(255,255,255,0.95)"/>
    <path d="M22 38 L28 32 L34 36 L42 26" stroke="#11998e" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" fill="none"/>
    <circle cx="20" cy="18" r="5" fill="rgba(255,255,255,0.7)"/>
  </svg>

  <svg v-else-if="current === 'gradient-budget'" :width="size" :height="size" viewBox="0 0 64 64">
    <defs><linearGradient id="bd" x1="0%" y1="0%" x2="100%" y2="100%"><stop offset="0%" stop-color="#4facfe"/><stop offset="100%" stop-color="#00f2fe"/></linearGradient></defs>
    <rect width="64" height="64" rx="16" fill="url(#bd)"/>
    <path d="M16 24 C16 20 20 18 24 18 H40 C44 18 48 20 48 24 V40 C48 44 44 46 40 46 H24 C20 46 16 44 16 40 V24Z" fill="rgba(255,255,255,0.95)"/>
    <circle cx="38" cy="26" r="3" fill="#00c6fb"/>
    <path d="M22 32 C22 32 28 38 42 32" stroke="#00c6fb" stroke-width="2.5" stroke-linecap="round" fill="none"/>
  </svg>

  <svg v-else-if="current === 'gradient-safety'" :width="size" :height="size" viewBox="0 0 64 64">
    <defs><linearGradient id="s" x1="0%" y1="0%" x2="100%" y2="100%"><stop offset="0%" stop-color="#fa709a"/><stop offset="100%" stop-color="#fee140"/></linearGradient></defs>
    <rect width="64" height="64" rx="16" fill="url(#s)"/>
    <path d="M32 12 C32 12 48 18 48 30 C48 42 32 52 32 52 C32 52 16 42 16 30 C16 18 32 12 32 12Z" fill="rgba(255,255,255,0.95)"/>
    <path d="M26 30 L30 34 L38 24" fill="none" stroke="#f5576c" stroke-width="3.5" stroke-linecap="round" stroke-linejoin="round"/>
  </svg>

  <svg v-else-if="current === 'outline-guarantee'" :width="size" :height="size" viewBox="0 0 64 64" fill="none" stroke="#764ba2" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
    <rect x="8" y="8" width="48" height="48" rx="12" stroke-width="2"/>
    <path d="M20 30 L32 22 L44 30 V36 C44 43 32 49 32 49 C32 49 20 43 20 36 V30Z"/>
    <path d="M26 32 L30 36 L38 28"/>
    <path d="M14 28 L32 17 L50 28"/>
  </svg>

  <svg v-else-if="current === 'outline-loan'" :width="size" :height="size" viewBox="0 0 64 64" fill="none" stroke="#f5576c" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
    <rect x="8" y="8" width="48" height="48" rx="12" stroke-width="2"/>
    <circle cx="46" cy="44" r="7" fill="rgba(245,87,108,0.1)"/>
    <path d="M43 44 H49 M46 41 V47"/>
    <path d="M32 46 C24 46 20 38 22 30 C24 24 28 18 32 12 C36 18 40 24 42 30 C44 38 40 46 32 46Z"/>
    <path d="M32 22 V36 M32 22 L28 26 M32 22 L36 26"/>
  </svg>

  <svg v-else-if="current === 'outline-business'" :width="size" :height="size" viewBox="0 0 64 64" fill="none" stroke="#11998e" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
    <rect x="8" y="8" width="48" height="48" rx="12" stroke-width="2"/>
    <rect x="16" y="22" width="32" height="26" rx="3"/>
    <path d="M22 38 L28 32 L34 36 L42 26"/>
    <path d="M18 18 H46"/>
  </svg>

  <svg v-else-if="current === 'outline-budget'" :width="size" :height="size" viewBox="0 0 64 64" fill="none" stroke="#00c6fb" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
    <rect x="8" y="8" width="48" height="48" rx="12" stroke-width="2"/>
    <path d="M18 26 C18 22 22 20 26 20 H38 C42 20 46 22 46 26 V38 C46 42 42 44 38 44 H26 C22 44 18 42 18 38 V26Z"/>
    <circle cx="38" cy="26" r="2.5" fill="#00c6fb"/>
    <path d="M22 32 C22 32 28 37 42 32"/>
  </svg>

  <svg v-else-if="current === 'outline-safety'" :width="size" :height="size" viewBox="0 0 64 64" fill="none" stroke="#f39c12" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
    <rect x="8" y="8" width="48" height="48" rx="12" stroke-width="2"/>
    <path d="M32 12 C32 12 48 18 48 30 C48 42 32 52 32 52 C32 52 16 42 16 30 C16 18 32 12 32 12Z"/>
    <path d="M26 30 L30 34 L38 24"/>
  </svg>

  <svg v-else-if="current === 'flat-guarantee'" :width="size" :height="size" viewBox="0 0 64 64">
    <rect width="64" height="64" rx="16" fill="#f3effa"/>
    <path d="M20 28 L32 20 L44 28 V36 C44 44 32 50 32 50 C32 50 20 44 20 36 V28Z" fill="#764ba2"/>
    <path d="M26 32 L30 36 L38 28" fill="none" stroke="#fff" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
    <path d="M14 28 L32 16 L50 28" fill="none" stroke="#9b7fd1" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
  </svg>

  <svg v-else-if="current === 'flat-loan'" :width="size" :height="size" viewBox="0 0 64 64">
    <rect width="64" height="64" rx="16" fill="#fff0f3"/>
    <circle cx="46" cy="44" r="9" fill="#f5576c"/>
    <text x="46" y="48" text-anchor="middle" fill="#fff" font-size="11" font-weight="700">¥</text>
    <path d="M32 46 C24 46 20 38 22 30 C24 24 28 18 32 12 C36 18 40 24 42 30 C44 38 40 46 32 46Z" fill="#f5576c"/>
    <path d="M32 22 V36 M32 22 L28 26 M32 22 L36 26" stroke="#fff" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" fill="none"/>
  </svg>

  <svg v-else-if="current === 'flat-business'" :width="size" :height="size" viewBox="0 0 64 64">
    <rect width="64" height="64" rx="16" fill="#e8faf1"/>
    <rect x="14" y="20" width="36" height="30" rx="4" fill="#11998e"/>
    <path d="M22 38 L28 32 L34 36 L42 26" stroke="#fff" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" fill="none"/>
    <circle cx="20" cy="18" r="5" fill="#38f9d7"/>
  </svg>

  <svg v-else-if="current === 'flat-budget'" :width="size" :height="size" viewBox="0 0 64 64">
    <rect width="64" height="64" rx="16" fill="#e6f7ff"/>
    <path d="M16 24 C16 20 20 18 24 18 H40 C44 18 48 20 48 24 V40 C48 44 44 46 40 46 H24 C20 46 16 44 16 40 V24Z" fill="#00c6fb"/>
    <circle cx="38" cy="26" r="3" fill="#fff"/>
    <path d="M22 32 C22 32 28 38 42 32" stroke="#fff" stroke-width="2.5" stroke-linecap="round" fill="none"/>
  </svg>

  <svg v-else-if="current === 'flat-safety'" :width="size" :height="size" viewBox="0 0 64 64">
    <rect width="64" height="64" rx="16" fill="#fff8e6"/>
    <path d="M32 12 C32 12 48 18 48 30 C48 42 32 52 32 52 C32 52 16 42 16 30 C16 18 32 12 32 12Z" fill="#f39c12"/>
    <path d="M26 30 L30 34 L38 24" fill="none" stroke="#fff" stroke-width="3.5" stroke-linecap="round" stroke-linejoin="round"/>
  </svg>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  name: { type: String, required: true }, // guarantee | loan | business | budget | safety
  set: { type: String, default: 'gradient' }, // gradient | outline | flat
  size: { type: [String, Number], default: 64 }
})

const current = computed(() => `${props.set}-${props.name}`)
</script>
