<template>
  <Teleport to="body">
    <Transition name="chenxi-loader-fade">
      <div v-if="active" class="chenxi-loader-backdrop">
        <div class="chenxi-loader-card">
          <p class="loader-text">页面加载中，请稍候…</p>
          <div class="boxes" aria-hidden="true">
            <div class="box">
              <div></div>
              <div></div>
              <div></div>
              <div class="face-hidden"></div>
            </div>
            <div class="box">
              <div></div>
              <div></div>
              <div></div>
              <div class="face-hidden"></div>
            </div>
            <div class="box">
              <div></div>
              <div></div>
              <div></div>
              <div class="face-hidden"></div>
            </div>
            <div class="box">
              <div></div>
              <div></div>
              <div></div>
              <div class="face-hidden"></div>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
const props = defineProps({
  active: {
    type: Boolean,
    default: false,
  },
})
</script>

<style scoped>
.chenxi-loader-backdrop {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: grid;
  place-items: center;
  background: radial-gradient(circle at 20% 20%, rgba(127, 123, 255, 0.16), transparent 45%),
    radial-gradient(circle at 80% 0%, rgba(255, 95, 143, 0.14), transparent 40%),
    rgba(5, 6, 12, 0.65);
  backdrop-filter: blur(10px);
}

.chenxi-loader-card {
  min-width: 260px;
  padding: 26px 32px 26px;
  border-radius: 24px;
  background: linear-gradient(145deg, rgba(23, 26, 39, 0.9), rgba(12, 13, 26, 0.92));
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 20px 80px rgba(0, 0, 0, 0.35);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
}

.loader-text {
  margin: 0;
  color: #dbeafe;
  font-weight: 600;
  letter-spacing: 0.4px;
}

.chenxi-loader-fade-enter-active,
.chenxi-loader-fade-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}

.chenxi-loader-fade-enter-from,
.chenxi-loader-fade-leave-to {
  opacity: 0;
  transform: scale(0.98);
}

/* ===== Box loader (3D cubes) ===== */
.boxes {
  --size: 26px;
  --duration: 840ms;
  height: calc(var(--size) * 2);
  width: calc(var(--size) * 3);
  position: relative;
  transform-style: preserve-3d;
  transform-origin: 50% 50%;
  margin-top: 6px;
  margin-bottom: 6px;
  transform: rotateX(60deg) rotateZ(45deg) rotateY(0deg) translateZ(0px);
}

.boxes .box {
  width: var(--size);
  height: var(--size);
  top: 0;
  left: 0;
  position: absolute;
  transform-style: preserve-3d;
}

.boxes .box:nth-child(1) {
  transform: translate(100%, 0);
  animation: box1 var(--duration) linear infinite;
}

.boxes .box:nth-child(2) {
  transform: translate(0, 100%);
  animation: box2 var(--duration) linear infinite;
}

.boxes .box:nth-child(3) {
  transform: translate(100%, 100%);
  animation: box3 var(--duration) linear infinite;
}

.boxes .box:nth-child(4) {
  transform: translate(200%, 0);
  animation: box4 var(--duration) linear infinite;
}

.boxes .box > div {
  --background: #5c8df6;
  --top: auto;
  --right: auto;
  --bottom: auto;
  --left: auto;
  --translateZ: calc(var(--size) / 2);
  --rotateY: 0deg;
  --rotateX: 0deg;
  position: absolute;
  width: 100%;
  height: 100%;
  background: var(--background);
  top: var(--top);
  right: var(--right);
  bottom: var(--bottom);
  left: var(--left);
  transform: rotateY(var(--rotateY)) rotateX(var(--rotateX)) translateZ(var(--translateZ));
  border-radius: 6px;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.08);
}

.boxes .box > div:nth-child(1) {
  --top: 0;
  --left: 0;
}

.boxes .box > div:nth-child(2) {
  --background: #145af2;
  --right: 0;
  --rotateY: 90deg;
}

.boxes .box > div:nth-child(3) {
  --background: #447cf5;
  --rotateX: -90deg;
}

.boxes .box > div:nth-child(4) {
  display: none;
}

@keyframes box1 {
  0%,
  50% {
    transform: translate(100%, 0);
  }

  100% {
    transform: translate(200%, 0);
  }
}

@keyframes box2 {
  0% {
    transform: translate(0, 100%);
  }

  50% {
    transform: translate(0, 0);
  }

  100% {
    transform: translate(100%, 0);
  }
}

@keyframes box3 {
  0%,
  50% {
    transform: translate(100%, 100%);
  }

  100% {
    transform: translate(0, 100%);
  }
}

@keyframes box4 {
  0% {
    transform: translate(200%, 0);
  }

  50% {
    transform: translate(200%, 100%);
  }

  100% {
    transform: translate(100%, 100%);
  }
}
</style>
