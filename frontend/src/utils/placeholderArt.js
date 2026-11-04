// 封面/图片加载失败时的降级占位图（data-URI SVG），供画廊与图集列表共用。
// data-URI 内无法引用 CSS 变量，色板取自 theme.css 的品牌粉两值
// （--color-brand-soft / --color-brand-strong），如调整品牌色请同步此处。
export const COVER_FALLBACK_ART =
  'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="400" height="300" viewBox="0 0 400 300"%3E%3Crect width="400" height="300" fill="url(%23grad)"/%3E%3Cdefs%3E%3ClinearGradient id="grad" x1="0%25" y1="0%25" x2="100%25" y2="100%25"%3E%3Cstop offset="0%25" style="stop-color:%23F9A8C8;stop-opacity:1" /%3E%3Cstop offset="100%25" style="stop-color:%23E87A9F;stop-opacity:1" /%3E%3C/linearGradient%3E%3C/defs%3E%3C/svg%3E'
