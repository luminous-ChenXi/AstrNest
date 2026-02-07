/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{vue,js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        'surface-body': '#05060c',
        'surface-panel': 'rgba(22, 25, 36, 0.85)',
        'surface-strong': 'rgba(35, 40, 62, 0.9)',
        'brand-primary': '#7f7bff',
        'brand-accent': '#ff5f8f',
        'brand-emerald': '#4ade80',
      },
      fontFamily: {
        sans: ['Plus Jakarta Sans', 'Noto Sans SC', 'system-ui', 'sans-serif'],
      },
      boxShadow: {
        card: '0 20px 60px rgba(2,6,23,0.45)',
      },
      backdropBlur: {
        glass: '30px',
      },
    },
  },
  plugins: [],
}
