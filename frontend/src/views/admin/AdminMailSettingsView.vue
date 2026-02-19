<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  fetchMailConfig,
  updateMailConfig,
  testMailConfig,
  fetchMailTemplates,
  createMailTemplate,
  updateMailTemplate,
  deleteMailTemplate,
  testMailTemplate,
} from '../../services/chenxi'

const activeTab = ref('smtp')
const smtpFormRef = ref()
const templateFormRef = ref()
const smtpLoading = ref(false)
const templateLoading = ref(false)
const templateDialogVisible = ref(false)
const isTemplateEdit = ref(false)
const templateVariablesInput = ref('')
const testEmail = ref('')
const templates = ref([])

const smtpForm = reactive({
  smtpHost: 'smtpdm.aliyun.com',
  smtpPort: 465,
  smtpUsername: 'chenxi@luminouschenxi.com',
  smtpPassword: '',
  secureType: 'ssl',
  fromEmail: 'chenxi@luminouschenxi.com',
  fromName: '辰汐图床',
  enabled: true,
})

const templateForm = reactive({
  id: null,
  name: '',
  type: 'register',
  subject: '',
  content: '',
  variables: [],
})

const smtpRules = {
  smtpHost: [{ required: true, message: '请输入 SMTP 服务器', trigger: 'blur' }],
  smtpPort: [{ required: true, message: '请输入端口号', trigger: 'change' }],
  smtpUsername: [{ required: true, message: '请输入邮箱账号', trigger: 'blur' }],
  fromEmail: [{ required: true, message: '请输入发件人邮箱', trigger: 'blur' }],
  fromName: [{ required: true, message: '请输入发件人名称', trigger: 'blur' }],
}

const templateRules = {
  name: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择模板类型', trigger: 'change' }],
  subject: [{ required: true, message: '请输入邮件主题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入邮件内容', trigger: 'blur' }],
}

const templateTypes = [
  { value: 'register', label: '注册验证' },
  { value: 'reset', label: '密码重置' },
  { value: 'notification', label: '系统通知' },
  { value: 'welcome', label: '欢迎邮件' },
]

const encryptionOptions = [
  { value: 'ssl', label: 'SSL' },
  { value: 'tls', label: 'TLS' },
  { value: 'none', label: '不加密' },
]

const loadConfig = async () => {
  smtpLoading.value = true
  try {
    const { data } = await fetchMailConfig()
    if (data) {
      Object.assign(smtpForm, { ...data, smtpPassword: '' })
    }
  } catch (error) {
    ElMessage.error('加载 SMTP 配置失败')
  } finally {
    smtpLoading.value = false
  }
}

const saveSmtpConfig = async () => {
  try {
    if (smtpFormRef.value) {
      await smtpFormRef.value.validate()
    }
    await updateMailConfig({ ...smtpForm })
    ElMessage.success('SMTP 配置已保存')
  } catch (error) {
    if (error) {
      ElMessage.error(error.message || '保存失败')
    }
  }
}

const handleTestSmtp = async () => {
  if (!testEmail.value) {
    ElMessage.warning('请输入测试邮箱地址')
    return
  }
  try {
    await testMailConfig(testEmail.value)
    ElMessage.success('测试邮件发送成功')
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '测试发送失败')
  }
}

const loadTemplates = async () => {
  templateLoading.value = true
  try {
    const { data } = await fetchMailTemplates()
    templates.value = data || []
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '加载邮件模板失败')
  } finally {
    templateLoading.value = false
  }
}

const openTemplateDialog = (row = null) => {
  isTemplateEdit.value = !!row
  if (row) {
    Object.assign(templateForm, row)
    templateVariablesInput.value = (row.variables || []).join(', ')
  } else {
    Object.assign(templateForm, {
      id: null,
      name: '',
      type: 'register',
      subject: '',
      content: '',
      variables: [],
    })
    templateVariablesInput.value = ''
  }
  templateDialogVisible.value = true
}

const resolveVariables = () => {
  if (!templateVariablesInput.value) {
    return []
  }
  return templateVariablesInput.value
    .split(',')
    .map((item) => item.trim())
    .filter((item) => item.length)
}

const saveTemplate = async () => {
  try {
    if (templateFormRef.value) {
      await templateFormRef.value.validate()
    }
    const payload = {
      ...templateForm,
      variables: resolveVariables(),
    }
    if (isTemplateEdit.value) {
      await updateMailTemplate(templateForm.id, payload)
      ElMessage.success('模板已更新')
    } else {
      await createMailTemplate(payload)
      ElMessage.success('模板已创建')
    }
    templateDialogVisible.value = false
    await loadTemplates()
  } catch (error) {
    if (error) {
      ElMessage.error(error?.response?.data?.message || '保存失败')
    }
  }
}

const deleteTemplate = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除模板 “${row.name}” 吗？`, '删除模板', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
    await deleteMailTemplate(row.id)
    ElMessage.success('模板已删除')
    await loadTemplates()
    if (!templates.value.length) {
      ElMessage.warning('请在“模板列表”新增一个模板，确保邮件通知可用')
    }
  } catch (error) {
    if (error === 'cancel') {
      return
    }
    ElMessage.error(error?.response?.data?.message || '删除失败')
  }
}

const testTemplate = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入要接收测试邮件的邮箱地址', '发送测试邮件', {
      confirmButtonText: '发送',
      cancelButtonText: '取消',
      inputPlaceholder: 'demo@example.com',
      inputPattern: /.+@.+/, 
      inputErrorMessage: '请输入正确的邮箱地址',
    })
    await testMailTemplate(row.id, { targetEmail: value, params: {} })
    ElMessage.success('测试邮件已发送')
  } catch (error) {
    if (error === 'cancel') {
      return
    }
    ElMessage.error(error?.response?.data?.message || '发送失败')
  }
}

const formatDate = (value) => {
  if (!value) {
    return '—'
  }
  return new Date(value).toLocaleString()
}

onMounted(() => {
  loadConfig()
  loadTemplates()
})
</script>

<template>
  <div class="chenxi-mail-settings space-y-6">
    <div class="glass-panel rounded-3xl border border-white/10 bg-white/5 p-6">
      <div class="mb-6 flex flex-col gap-2">
        <p class="text-xs uppercase tracking-[0.4em] text-white/60">Messaging</p>
        <h2 class="text-2xl font-semibold text-white">邮件设置</h2>
        <p class="text-sm text-white/60">参考 SMTP 设置，集中管理 SMTP 与邮件模板</p>
      </div>

      <el-tabs v-model="activeTab" class="chenxi-mail-tabs">
        <el-tab-pane label="SMTP 配置" name="smtp">
          <div class="grid gap-6 lg:grid-cols-2">
            <div class="glass-panel rounded-2xl border border-white/5 bg-black/30 p-5 text-sm text-white/80">
              <p class="text-base font-semibold text-white">示例配置</p>
              <p class="mt-1 text-xs text-white/50">请参考此 SMTP 模板来配置你的 SMTP 服务</p>
              <dl class="mt-4 space-y-3">
                <div class="flex justify-between gap-4">
                  <dt class="text-white/60">SMTP 服务器</dt>
                  <dd class="font-mono">smtpdm.aliyun.com</dd>
                </div>
                <div class="flex justify-between gap-4">
                  <dt class="text-white/60">端口</dt>
                  <dd class="font-mono">465</dd>
                </div>
                <div class="flex justify-between gap-4">
                  <dt class="text-white/60">邮箱账号</dt>
                  <dd class="font-mono">chenxi@luminouschenxi.com</dd>
                </div>
                <div class="flex justify-between gap-4">
                  <dt class="text-white/60">发件人邮箱</dt>
                  <dd class="font-mono">chenxi@luminouschenxi.com</dd>
                </div>
                <div class="flex justify-between gap-4">
                  <dt class="text-white/60">发件人名称</dt>
                  <dd>辰汐图床</dd>
                </div>
              </dl>
              <p class="mt-4 text-xs text-white/50">提示：密码为邮箱授权码，若无修改请留空。</p>
            </div>

            <div class="rounded-2xl border border-white/5 bg-white/5 p-5">
              <el-form ref="smtpFormRef" :model="smtpForm" :rules="smtpRules" label-width="110px" :disabled="smtpLoading">
                <el-form-item label="SMTP 服务器" prop="smtpHost">
                  <el-input v-model="smtpForm.smtpHost" placeholder="smtpdm.aliyun.com" />
                </el-form-item>
                <el-form-item label="端口" prop="smtpPort">
                  <el-input-number v-model="smtpForm.smtpPort" :min="1" :max="65535" />
                </el-form-item>
                <el-form-item label="邮箱账号" prop="smtpUsername">
                  <el-input v-model="smtpForm.smtpUsername" placeholder="邮箱账号" />
                </el-form-item>
                <el-form-item label="邮箱密码">
                  <el-input v-model="smtpForm.smtpPassword" type="password" placeholder="留空则沿用原授权码" show-password />
                </el-form-item>
                <el-form-item label="加密方式">
                  <el-select v-model="smtpForm.secureType" placeholder="选择加密方式">
                    <el-option v-for="option in encryptionOptions" :key="option.value" :label="option.label" :value="option.value" />
                  </el-select>
                </el-form-item>
                <el-form-item label="发件人邮箱" prop="fromEmail">
                  <el-input v-model="smtpForm.fromEmail" placeholder="发件邮箱" />
                </el-form-item>
                <el-form-item label="发件人名称" prop="fromName">
                  <el-input v-model="smtpForm.fromName" placeholder="辰汐图床" />
                </el-form-item>
                <el-form-item label="启用状态">
                  <el-switch v-model="smtpForm.enabled" active-text="已启用" inactive-text="停用" />
                </el-form-item>
                <el-form-item label="测试邮箱">
                  <div class="flex w-full items-center gap-3">
                    <el-input v-model="testEmail" placeholder="demo@example.com" />
                    <el-button :loading="smtpLoading" @click="handleTestSmtp">发送测试</el-button>
                  </div>
                </el-form-item>
                <el-form-item>
                  <div class="flex gap-3">
                    <el-button type="primary" :loading="smtpLoading" @click="saveSmtpConfig">保存设置</el-button>
                    <el-button @click="loadConfig">恢复服务器配置</el-button>
                  </div>
                </el-form-item>
              </el-form>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="邮件模板" name="templates">
          <div class="flex items-center justify-between">
            <div>
              <h3 class="text-xl font-semibold text-white">模板列表</h3>
              <p class="text-sm text-white/60">自定义注册、重置等场景的邮件内容</p>
            </div>
            <el-button type="primary" class="chenxi-btn-primary" @click="openTemplateDialog()">新增模板</el-button>
          </div>
          <div class="template-table-shell">
            <div class="template-table-surface">
              <el-table
                :data="templates"
                v-loading="templateLoading"
                stripe
                border
                empty-text=" "
                class="template-table"
              >
                <el-table-column prop="name" label="模板名称" width="200" />
                <el-table-column prop="type" label="类型" width="160">
                  <template #default="{ row }">
                    <el-tag size="small">{{ templateTypes.find((item) => item.value === row.type)?.label || row.type }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="subject" label="邮件主题" min-width="280" />
                <el-table-column label="更新时间" width="220">
                  <template #default="{ row }">{{ formatDate(row.updatedAt) }}</template>
                </el-table-column>
                <el-table-column label="操作" width="240" fixed="right">
                  <template #default="{ row }">
                    <div class="template-table-actions">
                      <el-button size="small" type="primary" plain @click="openTemplateDialog(row)">编辑</el-button>
                      <el-button size="small" type="info" plain @click="testTemplate(row)">测试</el-button>
                      <el-button size="small" type="danger" plain @click="deleteTemplate(row)">删除</el-button>
                    </div>
                  </template>
                </el-table-column>
              </el-table>
            </div>
            <div v-if="!templateLoading && !templates.length" class="template-table-empty">
              暂无模板，请在“模板列表”新增一个模板，确保注册、重置等邮件可以发送。
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <el-dialog v-model="templateDialogVisible" :title="isTemplateEdit ? '编辑邮件模板' : '新增邮件模板'" width="760px" class="chenxi-dialog">
      <el-form ref="templateFormRef" :model="templateForm" :rules="templateRules" label-width="110px">
        <el-form-item label="模板名称" prop="name">
          <el-input v-model="templateForm.name" placeholder="如：注册验证" />
        </el-form-item>
        <el-form-item label="模板类型" prop="type">
          <el-select v-model="templateForm.type" placeholder="选择场景">
            <el-option v-for="item in templateTypes" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="邮件主题" prop="subject">
          <el-input v-model="templateForm.subject" placeholder="可包含 {{name}} 等变量" />
        </el-form-item>
        <el-form-item label="变量声明">
          <el-input v-model="templateVariablesInput" placeholder="多个变量用逗号分隔，例如 code,name,expireTime" />
        </el-form-item>
        <el-form-item label="邮件内容" prop="content">
          <el-input v-model="templateForm.content" type="textarea" :rows="10" placeholder="支持 HTML，变量使用 {{code}} 形式" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="templateDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveTemplate">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.chenxi-mail-settings {
  padding-bottom: 2rem;
}

.chenxi-mail-tabs :deep(.el-tabs__item) {
  color: rgba(255, 255, 255, 0.6);
  padding: 12px 24px;
}

.chenxi-mail-tabs :deep(.el-tabs__item.is-active) {
  color: #fff;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 999px;
}

.template-table-shell {
  margin-top: 1.5rem;
  border-radius: 1.5rem;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(5, 5, 15, 0.85);
  padding: 1.25rem;
  box-shadow: 0 25px 60px rgba(0, 0, 0, 0.35);
}

.template-table-surface {
  border-radius: 1.25rem;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(8, 12, 24, 0.92);
  overflow: hidden;
  backdrop-filter: blur(18px);
}

.template-table {
  --table-border: rgba(255, 255, 255, 0.12);
  --table-header: rgba(15, 23, 42, 0.96);
  --table-hover: rgba(255, 255, 255, 0.08);
  width: 100%;
  background: transparent;
}

.template-table :deep(.el-table__inner-wrapper),
.template-table :deep(.el-table__body-wrapper) {
  background: transparent;
}

.template-table :deep(.el-table__header-wrapper),
.template-table :deep(.el-table__header),
.template-table :deep(.el-table__header thead),
.template-table :deep(.el-table__header tr) {
  background: var(--table-header);
}

.template-table :deep(.el-table__header-wrapper th),
.template-table :deep(.el-table__header th) {
  background: var(--table-header) !important;
  color: rgba(248, 250, 252, 0.9);
  border-color: var(--table-border) !important;
}

.template-table :deep(.el-table__row) {
  background: transparent;
}

.template-table :deep(.el-table__cell),
.template-table :deep(td) {
  background-color: transparent !important;
  color: rgba(248, 250, 252, 0.9);
  border-color: var(--table-border);
}

.template-table :deep(.el-table__body tr:nth-child(even) > td) {
  background: rgba(255, 255, 255, 0.02);
}

.template-table :deep(.el-table__body tr:hover > td) {
  background: var(--table-hover);
}

.template-table :deep(.el-tag) {
  background: rgba(58, 84, 172, 0.35);
  border: 1px solid rgba(129, 140, 248, 0.4);
  color: #e0e7ff;
}

.template-table :deep(.el-button) {
  border-color: rgba(255, 255, 255, 0.24);
  color: rgba(255, 255, 255, 0.9);
  background: rgba(255, 255, 255, 0.05);
}

.template-table :deep(.el-button.is-plain.is-info) {
  border-color: rgba(59, 130, 246, 0.4);
  color: #93c5fd;
}

.template-table :deep(.el-button.is-plain.is-danger) {
  border-color: rgba(248, 113, 113, 0.5);
  color: #fecaca;
}

.template-table-actions {
  display: flex;
  gap: 0.5rem;
}

.template-table-empty {
  padding: 2rem;
  text-align: center;
  color: rgba(255, 255, 255, 0.65);
  font-size: 0.95rem;
}

.chenxi-dialog :deep(.el-dialog) {
  background: rgba(15, 23, 42, 0.95);
  border: 1px solid rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(18px);
}

.chenxi-btn-primary {
  border: none;
  background: linear-gradient(135deg, #7f7bff, #ff5f8f);
  color: white;
}

.chenxi-btn-primary:hover {
  filter: brightness(1.05);
}
</style>
