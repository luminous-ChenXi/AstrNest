<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { ElMessage } from 'element-plus'
import {
  createAnnouncement,
  fetchAdminAnnouncementDetail,
  updateAnnouncement,
} from '../../services/announcements'

const route = useRoute()
const router = useRouter()
const isCreate = computed(() => route.name === 'admin-announcement-create')
const loading = ref(false)
const saving = ref(false)

const form = reactive({
  title: '',
  summary: '',
  level: 'NOTICE',
  status: 'DRAFT',
  pinned: false,
  contentMarkdown: '',
  publishedAt: '',
  author: '',
})

const previewHtml = computed(() => {
  if (!form.contentMarkdown) return ''
  const html = marked.parse(form.contentMarkdown)
  return DOMPurify.sanitize(html)
})

const load = async () => {
  if (isCreate.value) return
  loading.value = true
  try {
    const { id } = route.params
    const { data } = await fetchAdminAnnouncementDetail(id)
    form.title = data.title
    form.summary = data.summary || ''
    form.level = data.level || 'NOTICE'
    form.status = data.status || 'DRAFT'
    form.pinned = !!data.pinned
    form.contentMarkdown = data.contentMarkdown || ''
    form.publishedAt = data.publishedAt ? dayjs(data.publishedAt).format('YYYY-MM-DDTHH:mm') : ''
    form.author = data.author || ''
  } catch (error) {
    console.error('加载公告失败', error)
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(load)

const submit = async () => {
  saving.value = true
  try {
    const payload = {
      ...form,
      publishedAt: form.publishedAt ? new Date(form.publishedAt).toISOString() : null,
    }
    if (isCreate.value) {
      await createAnnouncement(payload)
      ElMessage.success('创建成功')
    } else {
      await updateAnnouncement(route.params.id, payload)
      ElMessage.success('保存成功')
    }
    router.push({ name: 'admin-announcements' })
  } catch (error) {
    console.error('保存公告失败', error)
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <p class="text-sm text-white/70">公告配置</p>
        <h1 class="text-2xl font-semibold text-white">{{ isCreate ? '新建公告' : '编辑公告' }}</h1>
      </div>
      <div class="flex gap-3">
        <button
          class="rounded-full border border-white/20 px-4 py-2 text-sm text-white hover:border-brand-primary"
          @click="router.push({ name: 'admin-announcements' })"
        >
          返回列表
        </button>
        <button
          class="rounded-full bg-brand-primary px-5 py-2 text-sm font-semibold text-white shadow-lg shadow-brand-primary/40 transition hover:-translate-y-0.5"
          :disabled="saving"
          @click="submit"
        >
          {{ saving ? '保存中...' : '保存' }}
        </button>
      </div>
    </div>

    <div class="grid gap-6 lg:grid-cols-2">
      <div class="glass-panel space-y-4 p-5">
        <div class="space-y-1">
          <label class="text-sm text-white/70">标题</label>
          <input
            v-model="form.title"
            type="text"
            placeholder="请输入公告标题"
            class="w-full rounded-2xl border border-white/10 bg-white/5 px-4 py-2.5 text-sm text-white outline-none focus:border-brand-primary"
          />
        </div>

        <div class="space-y-1">
          <label class="text-sm text-white/70">摘要</label>
          <textarea
            v-model="form.summary"
            rows="2"
            placeholder="简要说明公告内容"
            class="w-full rounded-2xl border border-white/10 bg-white/5 px-4 py-2.5 text-sm text-white outline-none focus:border-brand-primary"
          ></textarea>
        </div>

        <div class="grid grid-cols-2 gap-3">
          <div class="space-y-1">
            <label class="text-sm text-white/70">等级</label>
            <select
              v-model="form.level"
              class="w-full rounded-2xl border border-white/10 bg-white/5 px-4 py-2.5 text-sm text-white outline-none focus:border-brand-primary appearance-none cursor-pointer transition-all hover:bg-white/10"
              style="background-image: url('data:image/svg+xml,%3Csvg xmlns=%22http://www.w3.org/2000/svg%22 width=%2216%22 height=%2216%22 viewBox=%220 0 24 24%22 fill=%22none%22 stroke=%22%23ff6b9d%22 stroke-width=%222%22 stroke-linecap=%22round%22 stroke-linejoin=%22round%22%3E%3Cpath d=%22m6 9 6 6 6-6%22/%3E%3C/svg%3E'); background-repeat: no-repeat; background-position: right 12px center; padding-right: 40px;"
            >
              <option value="EMERGENCY" style="background: #1a1a2e; color: #ff6b9d;">🔴 紧急</option>
              <option value="NOTICE" style="background: #1a1a2e; color: #4ecdc4;">🔵 注意</option>
            </select>
          </div>
          <div class="space-y-1">
            <label class="text-sm text-white/70">状态</label>
            <select
              v-model="form.status"
              class="w-full rounded-2xl border border-white/10 bg-white/5 px-4 py-2.5 text-sm text-white outline-none focus:border-brand-primary appearance-none cursor-pointer transition-all hover:bg-white/10"
              style="background-image: url('data:image/svg+xml,%3Csvg xmlns=%22http://www.w3.org/2000/svg%22 width=%2216%22 height=%2216%22 viewBox=%220 0 24 24%22 fill=%22none%22 stroke=%22%234ecdc4%22 stroke-width=%222%22 stroke-linecap=%22round%22 stroke-linejoin=%22round%22%3E%3Cpath d=%22m6 9 6 6 6-6%22/%3E%3C/svg%3E'); background-repeat: no-repeat; background-position: right 12px center; padding-right: 40px;"
            >
              <option value="PUBLISHED" style="background: #1a1a2e; color: #2ecc71;">✅ 已发布</option>
              <option value="DRAFT" style="background: #1a1a2e; color: #f59e0b;">📝 草稿</option>
            </select>
          </div>
        </div>

        <div class="grid grid-cols-2 gap-3">
          <label class="flex items-center gap-2 text-sm text-white/80">
            <input type="checkbox" v-model="form.pinned" class="h-4 w-4 rounded border-white/30 bg-white/10" />
            置顶展示
          </label>
          <div class="space-y-1">
            <label class="text-sm text-white/70">发布时间（可选）</label>
            <input
              v-model="form.publishedAt"
              type="datetime-local"
              class="w-full rounded-2xl border border-white/10 bg-white/5 px-4 py-2.5 text-sm text-white outline-none focus:border-brand-primary"
            />
          </div>
        </div>

        <div class="space-y-1">
          <label class="text-sm text-white/70">作者（可选）</label>
          <input
            v-model="form.author"
            type="text"
            placeholder="展示给读者的署名"
            class="w-full rounded-2xl border border-white/10 bg-white/5 px-4 py-2.5 text-sm text-white outline-none focus:border-brand-primary"
          />
        </div>

        <div class="space-y-1">
          <label class="text-sm text-white/70">正文（Markdown）</label>
          <textarea
            v-model="form.contentMarkdown"
            rows="12"
            placeholder="支持 Markdown 语法，输入公告正文"
            class="w-full rounded-2xl border border-white/10 bg-white/5 px-4 py-3 text-sm text-white outline-none focus:border-brand-primary"
          ></textarea>
        </div>
      </div>

      <div class="glass-panel space-y-3 p-5">
        <div class="flex items-center justify-between">
          <h2 class="text-lg font-semibold text-white">实时预览</h2>
          <p class="text-xs text-white/60">渲染时已进行 DOMPurify 清理</p>
        </div>
        <div
          class="prose max-w-none rounded-2xl bg-white/5 p-5 prose-invert prose-headings:text-white"
          v-html="previewHtml"
        ></div>
      </div>
    </div>
  </div>
</template>
