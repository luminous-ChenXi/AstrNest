/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{vue,js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        'surface-body': 'var(--color-bg-primary)',
        'surface-panel': 'var(--color-bg-secondary)',
        'surface-strong': 'var(--color-bg-strong)',
        'brand-primary': 'var(--color-brand-primary)',
        'brand-accent': 'var(--color-brand-accent)',
        'brand-emerald': 'var(--color-brand-emerald)',
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
    },
  },
  plugins: [],
}
