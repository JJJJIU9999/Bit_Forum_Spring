import request from './request'

// 分页查询文章，不需要登录。params 会被 axios 转成 URL 查询参数。
export function pageArticles(params = { pageNum: 1, pageSize: 10 }) {
  return request.get('/article/page', { params })
}

// 查询文章详情。后端会把 Redis 中的浏览量、点赞数装进返回数据。
export function getArticleDetail(articleId) {
  return request.get('/article/detail', {
    params: { articleId },
  })
}

// 浏览文章：这个接口本身会让 Redis 浏览量 +1。
export function viewArticle(articleId) {
  return request.get('/article/view', {
    params: { articleId },
  })
}

// 热门文章来自 Redis ZSet。
export function getHotArticles() {
  return request.get('/article/hot')
}

// 发布文章需要登录，Authorization 会由 request.js 自动添加。
export function publishArticle(payload) {
  return request.post('/article/publish', payload)
}

// 修改文章需要登录，且后端会校验只能改自己的文章。
export function updateArticle(payload) {
  return request.put('/article/update', payload)
}

// 删除文章需要登录，普通用户只能删除自己的文章。
export function deleteArticle(articleId) {
  return request.delete('/article/delete', {
    params: { articleId },
  })
}

// 点赞接口使用 query 参数 articleId；body 不需要传内容，所以第二个参数传 null。
export function likeArticle(articleId) {
  return request.post('/article/like', null, {
    params: { articleId },
  })
}

// 取消点赞和点赞的参数结构保持一致。
export function unlikeArticle(articleId) {
  return request.post('/article/unlike', null, {
    params: { articleId },
  })
}
