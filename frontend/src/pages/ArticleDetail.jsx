import { useEffect, useRef, useState } from 'react'
import {
  favoriteArticle,
  getArticleDetail,
  likeArticle,
  unfavoriteArticle,
  unlikeArticle,
  viewArticle,
} from '../api/articleApi.js'
import { listComments, publishComment } from '../api/commentApi.js'
import { reportArticle, reportComment } from '../api/reportApi.js'
import PageHeader from '../components/PageHeader.jsx'
import LoadingSpinner from '../components/LoadingSpinner.jsx'
import MessageBanner from '../components/MessageBanner.jsx'

function ArticleDetail({ articleId, currentUser, onBack }) {
  const [article, setArticle] = useState(null)
  const [comments, setComments] = useState([])
  const [commentContent, setCommentContent] = useState('')
  const [message, setMessage] = useState('')
  const [messageType, setMessageType] = useState('info')
  const [loading, setLoading] = useState(false)
  const [actionLoading, setActionLoading] = useState(false)

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
      if (!articleId) return

      setLoading(true)
      setMessage('')

      try {
        if (viewedArticleRef.current !== articleId) {
          viewedArticleRef.current = articleId
          await viewArticle(articleId)
        }

        await Promise.all([refreshDetail(), refreshComments()])
      } catch (error) {
        setMessage(error.message)
        setMessageType('error')
      } finally {
        setLoading(false)
      }
    }

    loadDetail()
  }, [articleId])

  async function handleAction(action, successMsg) {
    setActionLoading(true)
    setMessage('')

    try {
      const result = await action()
      await refreshDetail()
      setMessage(result.message || successMsg)
      setMessageType('info')
    } catch (error) {
      setMessage(error.message)
      setMessageType('error')
    } finally {
      setActionLoading(false)
    }
  }

  async function handleReportArticle() {
    if (!currentUser) {
      setMessage('请先登录，再举报文章。')
      setMessageType('error')
      return
    }

    const reason = window.prompt('请输入举报原因')
    if (!reason) return

    setActionLoading(true)
    setMessage('')

    try {
      await reportArticle({ articleId, reason })
      setMessage('举报已提交，等待管理员处理。')
      setMessageType('info')
    } catch (error) {
      setMessage(error.message)
      setMessageType('error')
    } finally {
      setActionLoading(false)
    }
  }

  async function handleReportComment(commentId) {
    if (!currentUser) {
      setMessage('请先登录，再举报评论。')
      setMessageType('error')
      return
    }

    const reason = window.prompt('请输入举报原因')
    if (!reason) return

    setActionLoading(true)
    setMessage('')

    try {
      await reportComment({ commentId, reason })
      setMessage(`评论 #${commentId} 举报已提交。`)
      setMessageType('info')
    } catch (error) {
      setMessage(error.message)
      setMessageType('error')
    } finally {
      setActionLoading(false)
    }
  }

  async function handleCommentSubmit(event) {
    event.preventDefault()

    if (!currentUser) {
      setMessage('请先登录，再发表评论。')
      setMessageType('error')
      return
    }

    setActionLoading(true)
    setMessage('')

    try {
      await publishComment({ articleId, content: commentContent })
      setCommentContent('')
      await refreshComments()
      setMessage('评论发布成功。')
      setMessageType('info')
    } catch (error) {
      setMessage(error.message)
      setMessageType('error')
    } finally {
      setActionLoading(false)
    }
  }

  return (
    <div className="content-view">
      <PageHeader
        title="文章详情"
        description="阅读全文，参与点赞、收藏和评论互动。"
        badge={
          <button className="ghost-button" type="button" onClick={onBack}>
            返回列表
          </button>
        }
      />

      {loading && <LoadingSpinner text="加载详情中..." />}
      <MessageBanner message={message} type={messageType} />

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
            <strong>{article.favoriteCount ?? 0}</strong>
            <span>收藏</span>
            <strong>{article.userId}</strong>
            <span>作者 ID</span>
          </div>

          <div className="action-row">
            <button
              className="primary-button"
              disabled={actionLoading || !currentUser}
              type="button"
              onClick={() => handleAction(() => likeArticle(articleId), '点赞成功。')}
            >
              点赞
            </button>
            <button
              className="ghost-button"
              disabled={actionLoading || !currentUser}
              type="button"
              onClick={() => handleAction(() => unlikeArticle(articleId), '已取消点赞。')}
            >
              取消点赞
            </button>
            <button
              className="primary-button"
              disabled={actionLoading || !currentUser}
              type="button"
              onClick={() => handleAction(() => favoriteArticle(articleId), '收藏成功。')}
            >
              收藏
            </button>
            <button
              className="ghost-button"
              disabled={actionLoading || !currentUser}
              type="button"
              onClick={() => handleAction(() => unfavoriteArticle(articleId), '已取消收藏。')}
            >
              取消收藏
            </button>
            <button
              className="ghost-button"
              disabled={actionLoading || !currentUser}
              type="button"
              onClick={handleReportArticle}
            >
              举报文章
            </button>
            {!currentUser && <span>登录后可以点赞、收藏、评论和举报。</span>}
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
          {comments.map((comment) => (
            <article className="comment-item" key={comment.id}>
              <div>
                <strong>用户 {comment.userId}</strong>
                <span>{comment.createTime}</span>
              </div>
              <p>{comment.content}</p>
              <button
                className="ghost-button compact-button"
                disabled={actionLoading || !currentUser}
                type="button"
                onClick={() => handleReportComment(comment.id)}
              >
                举报评论
              </button>
            </article>
          ))}
          {comments.length === 0 && <p>暂无评论，来说两句吧。</p>}
        </div>
      </section>
    </div>
  )
}

export default ArticleDetail
