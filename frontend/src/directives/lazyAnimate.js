import { gsap } from 'gsap'

const defaults = {
  fromY: 26,
  fromX: 0,
  fromScale: 1,
  opacity: 0,
  duration: 0.6,
  delay: 0,
  ease: 'power2.out',
  threshold: 0.2,
  once: true,
}

export default {
  mounted(el, binding) {
    const options = { ...defaults, ...(binding?.value || {}) }

    gsap.set(el, {
      opacity: options.opacity,
      y: options.fromY,
      x: options.fromX,
      scale: options.fromScale,
    })

    const observer = new IntersectionObserver((entries) => {
      entries.forEach((entry) => {
        if (!entry.isIntersecting) return

        gsap.to(el, {
          opacity: 1,
          x: 0,
          y: 0,
          scale: 1,
          duration: options.duration,
          delay: options.delay,
          ease: options.ease,
          clearProps: 'all',
        })

        if (options.once !== false) {
          observer.unobserve(el)
        }
      })
    }, {
      threshold: options.threshold,
    })

    observer.observe(el)
    el.__lazyAnimateObserver = observer
  },
  unmounted(el) {
    if (el.__lazyAnimateObserver) {
      el.__lazyAnimateObserver.disconnect()
    }
  },
}
