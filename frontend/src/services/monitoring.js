import http from './http'

export const fetchOverviewMetrics = async () => {
  const { data } = await http.get('/api/monitor/overview')
  return data
}

export const fetchOperationLogs = async () => {
  const { data } = await http.get('/api/monitor/logs')
  return data
}
