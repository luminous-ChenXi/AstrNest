export default {
  mounted(el) {
    const target = el instanceof HTMLInputElement || el instanceof HTMLTextAreaElement
      ? el
      : el.querySelector('input, textarea');
    if (target) {
      target.focus();
    }
  },
}
