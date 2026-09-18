import request from './request'

// AI 助手相关接口。baseURL 已是 /api，所以这里从 /ai 开始写。
// 所有接口都要求登录：后端 LoginInterceptor 拦截 /api/ai/**。

export function createConversation(title) {
  return request.post('/ai/conversations', title ? { title } : {})
}

export function listConversations() {
  return request.get('/ai/conversations')
}

export function listMessages(conversationId) {
  return request.get(`/ai/conversations/${conversationId}/messages`)
}

export function sendMessage(conversationId, content) {
  // AI 生成回答耗时明显长于普通接口，单独放宽超时时间（默认 10s 容易超时）
  return request.post(
    `/ai/conversations/${conversationId}/messages`,
    { content },
    { timeout: 60000 },
  )
}
