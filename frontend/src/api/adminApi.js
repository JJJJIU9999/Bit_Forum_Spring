import request from './request'

// 管理员健康检查，用来验证当前 token 是否具备 ADMIN 权限。
export function checkAdminHealth() {
  return request.get('/admin/health')
}

// 后台文章分页：管理员可以查看所有文章。
export function pageAdminArticles(params = { pageNum: 1, pageSize: 10 }) {
  return request.get('/admin/article/page', { params })
}

// 管理员删除文章不校验作者归属，权限由后端 AdminInterceptor 控制。
export function deleteAdminArticle(articleId) {
  return request.delete('/admin/article/delete', {
    params: { articleId },
  })
}

// 后台评论分页：管理员可以查看所有评论。
export function pageAdminComments(params = { pageNum: 1, pageSize: 10 }) {
  return request.get('/admin/comment/page', { params })
}

// 管理员删除评论。
export function deleteAdminComment(commentId) {
  return request.delete('/admin/comment/delete', {
    params: { commentId },
  })
}

// 后台用户分页：后端返回的是脱敏 DTO，不包含 password。
export function pageAdminUsers(params = { pageNum: 1, pageSize: 10 }) {
  return request.get('/admin/user/page', { params })
}

// 禁用用户；后端会阻止管理员禁用自己。
export function disableUser(userId) {
  return request.put('/admin/user/disable', null, {
    params: { userId },
  })
}

// 启用用户。
export function enableUser(userId) {
  return request.put('/admin/user/enable', null, {
    params: { userId },
  })
}
