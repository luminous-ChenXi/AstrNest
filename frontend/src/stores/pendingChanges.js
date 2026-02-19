import { defineStore } from 'pinia'

export const usePendingChangesStore = defineStore('pendingChanges', {
  state: () => ({
    adminSystemConfigDirty: false,
  }),
  getters: {
    hasAdminSystemConfigChanges: (state) => state.adminSystemConfigDirty,
  },
  actions: {
    setAdminSystemConfigDirty(value) {
      this.adminSystemConfigDirty = Boolean(value)
    },
    resetAll() {
      this.adminSystemConfigDirty = false
    },
  },
})
