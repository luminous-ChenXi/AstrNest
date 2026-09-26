<script setup>
// 授权提示横幅（chenxi.license.*，N2 占位）：仅在"授权校验已开启且超出离线宽限/授权无效"时展示。
// 提醒性质，不阻断任何功能——绝不锁数据。默认（enabled=false）完全不渲染、不发请求也只拿到 enabled=false。
import { ElAlert } from 'element-plus'
import { onMounted, ref } from 'vue'
import { fetchLicenseStatus } from '../../services/license'

const visible = ref(false)
const message = ref('')

onMounted(async () => {
  try {
    const status = await fetchLicenseStatus()
    if (status?.enabled && status?.needsBanner) {
      message.value = status.message || '站点授权校验未通过：功能不受影响，请联系作者完成正版授权。'
      visible.value = true
    }
  } catch (error) {
    // 授权状态获取失败（未安装/网络异常）一律静默，绝不影响页面
  }
})
</script>

<template>
  <div v-if="visible" class="license-banner">
    <ElAlert
      :title="message"
      type="warning"
      show-icon
      :closable="true"
      @close="visible = false"
    />
  </div>
</template>

<style scoped>
.license-banner {
  position: fixed;
  top: 12px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 3000;
  width: min(92vw, 680px);
}
</style>
