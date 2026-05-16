/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        // Status-aware semantic colors used across KPI cards and pie slices.
        risk:    { 50: '#fef2f2', 500: '#ef4444', 700: '#b91c1c' },
        warn:    { 50: '#fffbeb', 500: '#f59e0b', 700: '#b45309' },
        ok:      { 50: '#f0fdf4', 500: '#22c55e', 700: '#15803d' },
        info:    { 50: '#eff6ff', 500: '#3b82f6', 700: '#1d4ed8' },
      },
    },
  },
  plugins: [],
};
