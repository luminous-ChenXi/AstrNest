import { defineStore } from 'pinia'

export const useUiStore = defineStore('ui', {
  state: () => ({
    isSidebarCollapsed: false,
    lastVisitedRoute: 'dashboard',
    pageLoading: false,
  }),
  actions: {
    toggleSidebar() {
      this.isSidebarCollapsed = !this.isSidebarCollapsed
    },
    rememberRoute(name) {
      this.lastVisitedRoute = name
    },
    setPageLoading(isLoading) {
      this.pageLoading = !!isLoading
    },
  },
})
