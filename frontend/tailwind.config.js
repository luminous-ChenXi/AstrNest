/** @type {import('tailwindcss').Config} */
export default {
  // useTheme 通过 @vueuse/core 的 useColorMode 在 <html> 上切换 .dark 类，
  // 因此 dark: 变体必须跟随 class 而不是系统 prefers-color-scheme。
  darkMode: 'class',
  content: ['./index.html', './src/**/*.{vue,js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        // 表面 / 背景
        'surface-body': 'var(--color-bg-primary)',
        'surface-panel': 'var(--color-bg-secondary)',
        'surface-strong': 'var(--color-bg-strong)',
        'surface-overlay': 'var(--panel-overlay)',
        'surface-secondary': 'var(--color-bg-secondary)',
        // 正文文字层级（primary 最深 → faint 最浅）
        body: 'var(--color-border)',
        'body-primary': 'var(--color-text-primary)',
        'body-secondary': 'var(--text-muted)',
        'body-muted': 'var(--text-muted)',
        'body-soft': 'var(--text-soft)',
        'body-faint': 'var(--text-faint)',
        // 文本别名（text-text-primary / text-text-secondary）
        'text-primary': 'var(--color-text-primary)',
        'text-secondary': 'var(--color-text-secondary)',
        // 边框（border-border）
        border: 'var(--color-border)',
        // 品牌色
        'brand-primary': 'var(--color-brand-primary)',
        'brand-accent': 'var(--color-brand-accent)',
        'brand-success': 'var(--color-brand-success)',
        'brand-emerald': 'var(--color-brand-emerald)',
        'brand-mint': 'var(--color-brand-mint)',
        'brand-sky': 'var(--color-brand-sky)',
        'brand-coral': 'var(--color-brand-coral)',
        'brand-cream': 'var(--color-brand-cream)',
      },
      fontFamily: {
        sans: ['Plus Jakarta Sans', 'Noto Sans SC', 'system-ui', 'sans-serif'],
      },
      boxShadow: {
        card: 'var(--shadow-card)',
      },
      backdropBlur: {
        glass: '30px',
      },
      borderColor: {
        // 让所有不带颜色的 border 类在明暗主题下都使用主题边框色，
        // 而不是 tailwind 默认的固定 gray-200（暗色模式下不可见）。
        DEFAULT: 'var(--color-border)',
      },
    },
  },
  plugins: [],
}
