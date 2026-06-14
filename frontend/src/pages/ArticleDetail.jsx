import { useEffect, useRef, useState } from 'react'
import { getArticleDetail, likeArticle, unlikeArticle, viewArticle } from '../api/articleApi.js'
import { listComments, publishComment } from '../api/commentApi.js'

function ArticleDetail({ articleId, currentUser, onBack }) {
  const [article, setArticle] = useState(null)
  const [comments, setComments] = useState([])
  const [commentContent, setCommentContent] = useState('')
  const [message, setMessage] = useState('')
  const [loading, setLoading] = useState(false)
  const [actionLoading, setActionLoading] = useState(false)

  // React 开发环境 StrictMode 可能重复执行 effect。
  // 这个 ref 用来保证同一篇文章详情页只调用一次浏览接口。
  const viewedArticleRef = useRef(null)

  async function refreshDetail() {
    const result = await getArticleDetail(articleId)
    setArticle(result.data)
  }

  async function refreshComments() {
    const result = await listComments(articleId)
    setComments(result.data || [])
  }

  useEffect(() => {
    async function loadDetail() {
      if (!articleId) {
        return
      }

      setLoading(true)
      setMessage('')

      try {
        if (viewedArticleRef.current !== articleId) {
          viewedArticleRef.current = articleId
          await viewArticle(articleId)
        }

        // 详情和评论分开请求：文章来自 article 接口，评论来自 comment 接口。
        await Promise.all([refreshDetail(), refreshComments()])
      } catch (error) {
        setMessage(error.message)
      } finally {
        setLoading(false)
      }
    }

    loadDetail()
  }, [articleId])

  async function handleLike() {
    setActionLoading(true)
    setMessage('')

    try {
      const result = await likeArticle(articleId)
      await refreshDetail()
      setMessage(result.message || '点赞状态已刷新。')
    } catch (error) {
      setMessage(error.message)
    } finally {
      setActionLoading(false)
    }
  }

  async function handleUnlike() {
    setActionLoading(true)
    setMessage('')

    try {
      const result = await unlikeArticle(articleId)
      await refreshDetail()
      setMessage(result.message || '点赞状态已刷新。')
    } catch (error) {
      setMessage(error.message)
    } finally {
      setActionLoading(false)
    }
  }

  async function handleCommentSubmit(event) {
    event.preventDefault()

    if (!currentUser) {
      setMessage('请先登录，再发表评论。')
      return
    }

    setActionLoading(true)
    setMessage('')

    try {
      // 评论发布只传 articleId/content；评论作者由后端从 JWT 中读取。
      await publishComment({
        articleId,
        content: commentContent,
      })
      setCommentContent('')
      await refreshComments()
      setMessage('评论发布成功。')
    } catch (error) {
      setMessage(error.message)
    } finally {
      setActionLoading(false)
    }
  }

  return (
    <div className="content-view">
      <div className="section-heading">
        <div>
          <h2>文章详情</h2>
          <p>进入详情时先调用浏览接口，再读取文章、点赞数和评论列表。</p>
        </div>
        <button className="ghost-button" type="button" onClick={onBack}>
          返回列表
        </button>
      </div>

      {loading && <p className="form-message">加载详情中...</p>}
      {message && <p className="form-message">{message}</p>}

      {article && (
        <article className="detail-card">
          <span>文章 #{article.id}</span>
          <h3>{article.title}</h3>
          <p>{article.content}</p>
          <div className="metric-row">
            <strong>{article.viewCount}</strong>
            <span>浏览</span>
            <strong>{article.likeCount}</strong>
            <span>点赞</span>
            <strong>{article.userId}</strong>
            <span>作者 ID</span>
          </div>

          <div className="action-row">
            <button
              className="primary-button"
              disabled={actionLoading || !currentUser}
              type="button"
              onClick={handleLike}
            >
              点赞
            </button>
            <button
              className="ghost-button"
              disabled={actionLoading || !currentUser}
              type="button"
              onClick={handleUnlike}
            >
              取消点赞
            </button>
            {!currentUser && <span>登录后可以点赞和评论。</span>}
          </div>
        </article>
      )}

      <section className="comment-section">
        <div className="sub-heading">
          <h3>评论</h3>
          <span>{comments.length} 条</span>
        </div>

        <form className="comment-form" onSubmit={handleCommentSubmit}>
          <textarea
            disabled={!currentUser || actionLoading}
            onChange={(event) => setCommentContent(event.target.value)}
            placeholder={currentUser ? '写一条评论' : '请先登录再评论'}
            value={commentContent}
          />
          <button
            className="primary-button"
            disabled={!currentUser || actionLoading}
            type="submit"
          >
            发表评论
          </button>
        </form>

        <div className="comment-list">
          {comments.length === 0 && <p>暂无评论。</p>}
          {comments.map((comment) => (
            <article className="comment-item" key={comment.id}>
              <div>
                <strong>用户 {comment.userId}</strong>
                <span>{comment.createTime}</span>
              </div>
              <p>{comment.content}</p>
            </article>
          ))}
        </div>
      </section>
    </div>
  )
}

export default ArticleDetail
