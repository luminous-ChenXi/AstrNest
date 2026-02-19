<template>
  <section class="space-y-6">
    <header>
      <p class="text-sm uppercase tracking-[0.4em] text-white/60">security</p>
      <h1 class="text-4xl font-semibold text-gradient">权限与安全</h1>
      <p class="text-white/70 mt-2 max-w-2xl">
        管理用户角色、域名白名单与内容审查策略。当前页面展示交互骨架，后续将接入真实数据。
      </p>
    </header>

    <div class="grid gap-5 lg:grid-cols-2">
      <article class="glass-panel p-6 space-y-4">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-white/60 text-sm">用户角色</p>
            <h2 class="text-xl font-semibold">权限矩阵</h2>
          </div>
          <button class="text-sm text-brand-primary">新建角色</button>
        </div>
        <div class="space-y-3">
          <div class="rounded-2xl bg-white/5 border border-white/5 px-4 py-3 flex items-center justify-between">
            <div>
              <p class="font-medium">管理员</p>
              <p class="text-xs text-white/60">全量权限</p>
            </div>
            <span class="text-xs text-emerald-300 bg-emerald-300/10 px-3 py-1 rounded-full">ACTIVE</span>
          </div>
          <div class="rounded-2xl bg-white/5 border border-white/5 px-4 py-3 flex items-center justify-between">
            <div>
              <p class="font-medium">审核员</p>
              <p class="text-xs text-white/60">内容审查 + 日志查看</p>
            </div>
            <span class="text-xs text-white/60">8 USERS</span>
          </div>
        </div>
      </article>

      <article class="glass-panel p-6 space-y-4">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-white/60 text-sm">访问策略</p>
            <h2 class="text-xl font-semibold">域名白名单</h2>
          </div>
          <button class="text-sm text-brand-primary">添加域名</button>
        </div>
        <div class="space-y-3">
          <div class="rounded-2xl bg-white/5 border border-white/5 px-4 py-3 flex items-center justify-between">
            <div>
              <p class="font-medium">cdn.luminouschenxi.net</p>
              <p class="text-xs text-white/60">默认 CDN 域</p>
            </div>
            <span class="text-xs text-emerald-300">允许</span>
          </div>
          <div class="rounded-2xl bg-white/5 border border-white/5 px-4 py-3 flex items-center justify-between opacity-60">
            <div>
              <p class="font-medium">typora.local</p>
              <p class="text-xs text-white/60">Typora 调试</p>
            </div>
            <span class="text-xs text-amber-300">审核中</span>
          </div>
        </div>
      </article>
    </div>

    <article class="glass-panel p-6">
      <div class="flex items-center justify-between">
        <div>
          <p class="text-white/60 text-sm">内容安全</p>
          <h2 class="text-xl font-semibold">审查引擎</h2>
        </div>
        <button class="text-sm text-brand-primary">配置回调</button>
      </div>
      <p class="text-white/60 mt-2">
        预留鉴黄/鉴暴接口占位，允许配置第三方内容安全供应商。
      </p>
      <div class="mt-4 grid gap-4 md:grid-cols-3">
        <div class="rounded-2xl bg-white/5 border border-white/5 p-4">
          <p class="text-xs text-white/60">今日审核</p>
          <p class="text-2xl font-semibold">312</p>
        </div>
        <div class="rounded-2xl bg-white/5 border border-white/5 p-4">
          <p class="text-xs text-white/60">拦截</p>
          <p class="text-2xl font-semibold text-amber-300">9</p>
        </div>
        <div class="rounded-2xl bg-white/5 border border-white/5 p-4">
          <p class="text-xs text-white/60">待人工复核</p>
          <p class="text-2xl font-semibold text-rose-300">3</p>
        </div>
      </div>
    </article>
  </section>
</template>
