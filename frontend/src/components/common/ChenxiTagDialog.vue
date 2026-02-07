<template>
  <el-dialog
    :model-value="visibleState"
    :title="title"
    width="480px"
    class="chenxi-tag-dialog"
    append-to-body
    @close="handleClose"
  >
    <div class="space-y-4">
      <el-input
        v-model="keyword"
        placeholder="搜索现有标签"
        clearable
        @clear="performSearch"
        @keyup.enter.native="performSearch"
      >
        <template #suffix>
          <el-button link type="primary" @click="performSearch">搜索</el-button>
        </template>
      </el-input>

      <div class="flex flex-wrap gap-2">
        <el-tag
          v-for="tag in selected"
          :key="tag"
          type="info"
          closable
          @close="removeTag(tag)"
        >
          {{ tag }}
        </el-tag>
        <span v-if="!selected.length" class="text-xs text-white/50">尚未添加标签，最多可选 {{ max }} 个</span>
      </div>

      <div class="tag-result-shell">
        <div class="flex items-center justify-between text-xs text-white/60">
          <span>推荐标签</span>
          <el-button link type="primary" @click="performSearch">刷新</el-button>
        </div>
        <div class="tag-result-list">
          <button
            v-for="option in tagOptions"
            :key="option.id || option.name"
            type="button"
            class="tag-pill"
            :class="{ 'tag-pill--active': isSelected(option.name) }"
            @click="toggleTag(option.name)"
          >
            <span>{{ option.name }}</span>
            <small v-if="option.description">{{ option.description }}</small>
          </button>
          <p v-if="!tagOptions.length && !loading" class="text-xs text-white/60">暂无匹配的标签</p>
          <p v-if="loading" class="text-xs text-white/60">加载中...</p>
        </div>
      </div>

      <div class="new-tag-form">
        <el-input
          v-model="newTagName"
          placeholder="输入新的标签名称"
          maxlength="60"
          @keyup.enter.native="handleCreate"
        />
        <el-button type="primary" :loading="creating" class="gradient-btn" @click="handleCreate">新增标签</el-button>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" :disabled="!selected.length" @click="handleConfirm">确定</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { createTag, searchTags } from '../../services/tags'

const props = defineProps({
  visible: { type: Boolean, default: false },
  tags: { type: Array, default: () => [] },
  title: { type: String, default: '选择标签' },
  max: { type: Number, default: 10 },
})

const emit = defineEmits(['update:visible', 'update:tags'])

const visibleState = ref(false)
const selected = ref([])
const keyword = ref('')
const tagOptions = ref([])
const loading = ref(false)
const newTagName = ref('')
const creating = ref(false)

watch(
  () => props.visible,
  (value) => {
    visibleState.value = value
    if (value) {
      selected.value = [...props.tags]
      keyword.value = ''
      loadTags()
    }
  },
  { immediate: true }
)

watch(
  () => props.tags,
  (value) => {
    if (!visibleState.value) {
      selected.value = [...value]
    }
  }
)

const isSelected = (name) => selected.value.some((item) => item.toLowerCase() === name.toLowerCase())

const toggleTag = (name) => {
  if (!name) return
  const normalized = name.trim()
  if (!normalized) return
  if (isSelected(normalized)) {
    removeTag(normalized)
    return
  }
  if (selected.value.length >= props.max) {
    ElMessage.warning(`最多只能选择 ${props.max} 个标签`)
    return
  }
  selected.value = [...selected.value, normalized]
}

const removeTag = (name) => {
  selected.value = selected.value.filter((tag) => tag.toLowerCase() !== name.toLowerCase())
}

const handleClose = () => {
  emit('update:visible', false)
}

const handleConfirm = () => {
  emit('update:tags', [...selected.value])
  handleClose()
}

const performSearch = () => {
  loadTags()
}

const loadTags = async () => {
  loading.value = true
  try {
    const data = await searchTags({ keyword: keyword.value, limit: 30 })
    tagOptions.value = data || []
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '加载标签失败')
  } finally {
    loading.value = false
  }
}

const handleCreate = async () => {
  const normalized = newTagName.value.trim()
  if (!normalized) {
    ElMessage.warning('请输入标签名称')
    return
  }
  if (isSelected(normalized)) {
    ElMessage.info('标签已在选择列表中')
    newTagName.value = ''
    return
  }
  creating.value = true
  try {
    const data = await createTag({ name: normalized })
    if (data?.name) {
      tagOptions.value = [data, ...tagOptions.value.filter((tag) => tag.name !== data.name)]
      toggleTag(data.name)
      newTagName.value = ''
      ElMessage.success('标签已创建')
    }
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '创建失败')
  } finally {
    creating.value = false
  }
}
</script>

<style scoped>
.chenxi-tag-dialog :deep(.el-dialog__body) {
  background: rgba(5, 7, 18, 0.92);
}

.chenxi-tag-dialog :deep(.el-dialog__title) {
  color: #fff;
  letter-spacing: 0.1em;
}

.tag-result-shell {
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 18px;
  padding: 1rem;
  background: rgba(15, 23, 42, 0.4);
}

.tag-result-list {
  margin-top: 0.75rem;
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.tag-pill {
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 999px;
  padding: 0.35rem 0.9rem;
  color: rgba(255, 255, 255, 0.8);
  background: transparent;
  font-size: 0.85rem;
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  transition: all 0.2s ease;
}

.tag-pill small {
  font-size: 0.7rem;
  color: rgba(255, 255, 255, 0.5);
}

.tag-pill--active {
  border-color: #7f7bff;
  background: rgba(127, 123, 255, 0.1);
  color: #fff;
}

.new-tag-form {
  display: flex;
  gap: 0.75rem;
}

.gradient-btn {
  background: linear-gradient(135deg, #7f7bff, #ff5f8f);
  border: none;
  color: #fff;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}
</style>
