<template>
  <div class="social-card-container">
    <!-- 背景旋转效果 -->
    <!-- <div class="main_back"></div> -->

    <!-- 主容器 -->
    <div class="main">
      <!-- 提示文字卡片 - 悬停时隐藏 -->
      <div class="hover-text-card">
        <p class="hover-text">
          在此轻触
        </p>
      </div>

      <!-- 社交图标网格 -->
      <div class="social-grid">
        <!-- GitHub -->
        <a href="https://github.com/luminous-ChenXi/AstrNest" target="_blank" rel="noopener noreferrer" class="card">
          <svg class="card_icon card_github" viewBox="0 0 24 24" fill="currentColor">
            <path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z"/>
          </svg>
        </a>

        <!-- 邮箱 -->
        <a href="mailto:chenxi@luminouschenxi.net" target="_blank" rel="noopener noreferrer" class="card">
          <svg class="card_icon card_email" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="m22 7-8.991 5.727a2 2 0 0 1-2.009 0L2 7"></path>
            <rect x="2" y="4" width="20" height="16" rx="2"></rect>
          </svg>
        </a>

        <!-- Discord -->
        <a href="https://discord.gg/hBsqcfwC9Q" target="_blank" rel="noopener noreferrer" class="card">
          <svg class="card_icon card_discord" viewBox="0 0 24 24" fill="currentColor">
            <path d="M20.317 4.37a19.791 19.791 0 0 0-4.885-1.515.074.074 0 0 0-.079.037c-.21.375-.444.864-.608 1.25a18.27 18.27 0 0 0-5.487 0 12.64 12.64 0 0 0-.617-1.25.077.077 0 0 0-.079-.037A19.736 19.736 0 0 0 3.677 4.37a.07.07 0 0 0-.032.027C.533 9.046-.32 13.58.099 18.057a.082.082 0 0 0 .031.057 19.9 19.9 0 0 0 5.993 3.03.078.078 0 0 0 .084-.028 14.09 14.09 0 0 0 1.226-1.994.076.076 0 0 0-.041-.106 13.107 13.107 0 0 1-1.872-.892.077.077 0 0 1-.008-.128 10.2 10.2 0 0 0 .372-.292.074.074 0 0 1 .077-.01c3.928 1.793 8.18 1.793 12.062 0a.074.074 0 0 1 .078.01c.12.098.246.198.373.292a.077.077 0 0 1-.006.127 12.299 12.299 0 0 1-1.873.892.077.077 0 0 0-.041.107c.36.698.772 1.362 1.225 1.993a.076.076 0 0 0 .084.028 19.839 19.839 0 0 0 6.002-3.03.077.077 0 0 0 .032-.054c.5-5.177-.838-9.674-3.549-13.66a.061.061 0 0 0-.031-.03zM8.02 15.33c-1.183 0-2.157-1.085-2.157-2.419 0-1.333.956-2.419 2.157-2.419 1.21 0 2.176 1.096 2.157 2.42 0 1.333-.956 2.418-2.157 2.418zm7.975 0c-1.183 0-2.157-1.085-2.157-2.419 0-1.333.955-2.419 2.157-2.419 1.21 0 2.176 1.096 2.157 2.42 0 1.333-.946 2.418-2.157 2.418z"/>
          </svg>
        </a>

        <!-- X (Twitter) -->
        <a href="https://x.com/luminous_chenxi" target="_blank" rel="noopener noreferrer" class="card">
          <svg class="card_icon card_x" viewBox="0 0 24 24" fill="currentColor">
            <path d="M18.244 2.25h3.308l-7.227 8.26 8.502 11.24H16.17l-5.214-6.817L4.99 21.75H1.68l7.73-8.835L1.254 2.25H8.08l4.713 6.231zm-1.161 17.52h1.833L7.084 4.126H5.117z"/>
          </svg>
        </a>

        <!-- 博客个人网站 -->
        <a href="https://luminouschenxi.com" target="_blank" rel="noopener noreferrer" class="card card_center">
          <svg class="card_icon card_blog" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M15 3h6v6"></path>
            <path d="M10 14 21 3"></path>
            <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"></path>
          </svg>
        </a>

        <!-- Bilibili -->
        <a href="https://space.bilibili.com/2001472338" target="_blank" rel="noopener noreferrer" class="card">
          <svg class="card_icon card_bilibili" viewBox="0 0 24 24" fill="currentColor">
            <path d="M17.813 4.653h.854c1.51.054 2.769.578 3.773 1.574 1.004.995 1.524 2.249 1.56 3.76v7.36c-.036 1.51-.556 2.769-1.56 3.773s-2.262 1.524-3.773 1.56H5.333c-1.51-.036-2.769-.556-3.773-1.56S.036 18.858 0 17.347v-7.36c.036-1.511.556-2.765 1.56-3.76 1.004-.996 2.262-1.52 3.773-1.574h.774l-1.174-1.12a1.234 1.234 0 0 1-.373-.906c0-.356.124-.658.373-.907l.027-.027c.267-.249.573-.373.92-.373.347 0 .653.124.92.373L9.653 4.44c.071.071.134.142.187.213h4.267a.836.836 0 0 1 .16-.213l2.853-2.747c.267-.249.573-.373.92-.373.347 0 .662.151.929.4.267.249.391.551.391.907 0 .355-.124.657-.373.906zM5.333 7.24c-.746.018-1.373.276-1.88.773-.506.498-.769 1.13-.786 1.894v7.52c.017.764.28 1.395.786 1.893.507.498 1.134.756 1.88.773h13.334c.746-.017 1.373-.275 1.88-.773.506-.498.769-1.129.786-1.893v-7.52c-.017-.765-.28-1.396-.786-1.894-.507-.497-1.134-.755-1.88-.773zM8 11.107c.373 0 .684.124.933.373.25.249.383.569.4.96v1.173c-.017.391-.15.711-.4.96-.249.25-.56.374-.933.374s-.684-.125-.933-.374c-.25-.249-.383-.569-.4-.96V12.44c0-.373.129-.689.386-.947.258-.257.574-.386.947-.386zm8 0c.373 0 .684.124.933.373.25.249.383.569.4.96v1.173c-.017.391-.15.711-.4.96-.249.25-.56.374-.933.374s-.684-.125-.933-.374c-.25-.249-.383-.569-.4-.96V12.44c.017-.391.15-.711.4-.96.249-.249.56-.373.933-.373z"/>
          </svg>
        </a>

        <!-- QQ -->
        <a href="" target="_blank" rel="noopener noreferrer" class="card">
          <svg class="card_icon card_qq" viewBox="0 0 24 24" fill="currentColor">
            <path d="M12 0C5.373 0 0 5.373 0 12c0 6.016 4.432 10.984 10.206 11.852V15.18H7.237v-3.154h2.969v-2.099c0-3.475 1.693-5 4.581-5 1.383 0 2.115.102 2.461.147v2.748h-1.97c-1.226 0-1.654 1.163-1.654 2.473v1.731h3.6l-.488 3.154h-3.112v8.822C19.568 22.984 24 18.016 24 12c0-6.627-5.373-12-12-12z"/>
          </svg>
        </a>

        <!-- gitee -->
        <a href="https://gitee.com/Lucky-917" target="_blank" rel="noopener noreferrer" class="card">
          <svg class="card_icon card_gitee" viewBox="0 0 1024 1024" fill="currentColor">
            <path d="M512 1024C229.222 1024 0 794.778 0 512S229.222 0 512 0s512 229.222 512 512-229.222 512-512 512z m259.149-568.883h-290.74a25.293 25.293 0 0 0-25.292 25.293l-0.026 63.206c0 13.952 11.315 25.293 25.267 25.293h177.024c13.978 0 25.293 11.315 25.293 25.267v12.646a75.853 75.853 0 0 1-75.853 75.853h-240.23a25.293 25.293 0 0 1-25.267-25.293V417.203a75.853 75.853 0 0 1 75.827-75.853h353.946a25.293 25.293 0 0 0 25.267-25.292l0.077-63.207a25.293 25.293 0 0 0-25.268-25.293H417.152a189.62 189.62 0 0 0-189.62 189.645V771.15c0 13.977 11.316 25.293 25.294 25.293h372.94a170.65 170.65 0 0 0 170.65-170.65V480.384a25.293 25.293 0 0 0-25.293-25.267z"/>
          </svg>
        </a>

        <!-- YouTube -->
        <a href="" target="_blank" rel="noopener noreferrer" class="card">
          <svg class="card_icon card_youtube" viewBox="0 0 1024 1024" fill="currentColor">
            <path d="M426.666667 682.666667V384l256 149.845333L426.666667 682.666667z m587.093333-355.541334s-10.026667-71.04-40.704-102.357333c-38.954667-41.088-82.602667-41.258667-102.613333-43.648C727.168 170.666667 512.213333 170.666667 512.213333 170.666667h-0.426666s-214.954667 0-358.229334 10.453333c-20.053333 2.389333-63.658667 2.56-102.656 43.648-30.677333 31.317333-40.661333 102.4-40.661333 102.4S0 410.538667 0 493.952v78.293333c0 83.456 10.24 166.912 10.24 166.912s9.984 71.04 40.661333 102.357334c38.997333 41.088 90.154667 39.765333 112.938667 44.074666C245.76 893.568 512 896 512 896s215.168-0.341333 358.442667-10.752c20.053333-2.432 63.658667-2.602667 102.613333-43.690667 30.72-31.317333 40.704-102.4 40.704-102.4s10.24-83.413333 10.24-166.869333v-78.250667c0-83.456-10.24-166.912-10.24-166.912z"/>
          </svg>
        </a>
      </div>
    </div>
  </div>
</template>

<style scoped>
.social-card-container {
  position: relative;
  width: 200px;
  height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.main_back {
  position: absolute;
  border-radius: 10px;
  transform: rotate(90deg);
  width: 176px;
  height: 176px;
  background: linear-gradient(270deg, #03a9f4, #cc39a4, #ffb5d2);
  z-index: 0;
  box-shadow: inset 0px 0px 180px 5px #ffffff;
  transition: opacity 0.3s ease;
}

.main:hover .main_back {
  opacity: 0;
}

.main {
  position: relative;
  width: 180px;
  height: 180px;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1;
}

/* 提示文字卡片 */
.hover-text-card {
  position: absolute;
  width: 100px;
  height: 100px;
  background: linear-gradient(180deg, #FADCE9 0%, #E8F4FD 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
  transition: opacity 0.3s ease, transform 0.3s ease;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.hover-text {
  margin: 0;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 2px;
  text-align: center;
  color: #333;
  line-height: 1.4;
}

/* 社交图标网格 */
.social-grid {
  display: grid;
  grid-template-columns: repeat(3, 50px);
  grid-template-rows: repeat(3, 50px);
  gap: 4px;
  opacity: 0;
  transform: scale(0.8);
  transition: opacity 0.3s ease, transform 0.3s ease;
}

/* 悬停效果 */
.main:hover .hover-text-card {
  opacity: 0;
  transform: scale(0.8);
}

.main:hover .social-grid {
  opacity: 1;
  transform: scale(1);
}

.card {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 50px;
  height: 50px;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(5px);
  -webkit-backdrop-filter: blur(5px);
  border: 1px solid transparent;
  border-radius: 10px;
  text-decoration: none;
  cursor: pointer;
  transition: all 0.3s ease;
}

.card:hover {
  background: rgba(255, 255, 255, 0.95);
  transform: scale(1.1);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.card_icon {
  width: 24px;
  height: 24px;
  transition: transform 0.2s ease;
}

/* 各图标颜色 */
.card_github {
  color: #000;
}

.card_email {
  color: #ea4335;
}

.card_discord {
  color: #8c9eff;
}

.card_x {
  color: #000;
}

.card_blog {
  color: #03a9f4;
}

.card_bilibili {
  color: #fb7299;
}

.card_qq {
  color: #12b7f5;
}

.card_gitee {
  color: #c71d23;
}

.card_youtube {
  color: #ff0000;
}

/* 圆角调整 */
.card:nth-child(1) {
  border-top-left-radius: 10px;
}

.card:nth-child(2) {
  border-radius: 0;
}

.card:nth-child(3) {
  border-top-right-radius: 10px;
}

.card:nth-child(4) {
  border-radius: 0;
}

.card:nth-child(5) {
  border-radius: 0;
}

.card:nth-child(6) {
  border-radius: 0;
}

.card:nth-child(7) {
  border-bottom-left-radius: 10px;
}

.card:nth-child(8) {
  border-radius: 0;
}

.card:nth-child(9) {
  border-bottom-right-radius: 10px;
}
</style>
