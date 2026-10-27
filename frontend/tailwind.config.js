const c = (name) => `rgb(var(--${name}) / <alpha-value>)`;
/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  darkMode: ['class', '[data-theme="dark"]'],
  theme: {
    extend: {
      colors: {
        bg: c('bg'), sidebar: c('sidebar'), card: c('card'), surface: c('surface'),
        primary: c('primary'), cyan: c('cyan'), violet: c('violet'),
        fg: c('fg'), muted: c('muted'), line: c('line'),
        safe: c('safe'), warn: c('warn'), danger: c('danger'),
      },
      fontFamily: { sans: ['"Inter Variable"', 'Inter', 'system-ui', 'sans-serif'] },
      borderRadius: { xl: '14px' },
    },
  },
  plugins: [],
};
