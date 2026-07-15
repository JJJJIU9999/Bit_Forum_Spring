import { useEffect, useRef, useState } from 'react'
import { Bookmark, ChevronLeft, Flag, Heart, MessageCircle, UserRound } from 'lucide-react'
import { useNavigate, useOutletContext, useParams } from 'react-router'
import { favoriteArticle, getArticleDetail, likeArticle, unfavoriteArticle, unlikeArticle, viewArticle } from '../api/articleApi.js'
import { listComments, publishComment } from '../api/commentApi.js'
import { reportArticle, reportComment } from '../api/reportApi.js'
import PageHeader from '../components/PageHeader.jsx'
import LoadingSpinner from '../components/LoadingSpinner.jsx'
import MessageBanner from '../components/MessageBanner.jsx'
import ConfirmDialog from '../components/ConfirmDialog.jsx'

function ArticleDetail() {
  const { articleId } = useParams()
  const { currentUser } = useOutletContext()
  const navigate = useNavigate()
  const [article, setArticle] = useState(null)
  const [comments, setComments] = useState([])
  const [commentContent, setCommentContent] = useState('')
  const [message, setMessage] = useState('')
  const [messageType, setMessageType] = useState('info')
  const [loading, setLoading] = useState(false)
  const [actionLoading, setActionLoading] = useState(false)
  const [reportTarget, setReportTarget] = useState(null)
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

  async function handleAction(action, successMessage) {
    if (!currentUser) {
      navigate(`/login?redirectTo=${encodeURIComponent(`/articles/${articleId}`)}`)
      return
    }
    setActionLoading(true)
    setMessage('')
    try {
      const result = await action()
      await refreshDetail()
      setMessage(result.message || successMessage)
      setMessageType('info')
    } catch (error) {
      setMessage(error.message)
      setMessageType('error')
    } finally {
      setActionLoading(false)
    }
  }

  async function handleReport(reason) {
    setActionLoading(true)
    setMessage('')
    try {
      if (reportTarget?.type === 'article') await reportArticle({ articleId, reason })
      else await reportComment({ commentId: reportTarget.id, reason })
      setMessage(reportTarget?.type === 'article' ? '举报已提交，等待管理员处理。' : `评论 #${reportTarget.id} 举报已提交。`)
      setMessageType('info')
      setReportTarget(null)
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
      navigate(`/login?redirectTo=${encodeURIComponent(`/articles/${articleId}`)}`)
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
    <div className="content-view article-detail-view">
      <PageHeader
        title="文章详情"
        description="阅读全文，参与观点交换。"
        actions={<button className="ghost-button icon-text-button" type="button" onClick={() => navigate(-1)}><ChevronLeft size={17} aria-hidden="true" />返回</button>}
      />
      {loading && <LoadingSpinner text="正在打开文章..." />}
      <MessageBanner message={message} type={messageType} />

      {article && (
        <article className="detail-card editorial-article">
          <header>
            <p className="article-card-kicker">{article.categoryName || '社区文章'}</p>
            <h2>{article.title}</h2>
            <div className="article-byline"><span>文章 #{article.id}</span><span>作者 #{article.userId}</span></div>
          </header>
          {article.coverUrl && <img className="detail-cover" alt={`${article.title} 封面`} src={article.coverUrl} />}
          <p className="article-body">{article.content}</p>
          <footer className="article-detail-footer">
            <div className="metric-row" aria-label="文章互动数据">
              <span><strong>{article.viewCount}</strong> 浏览</span>
              <span><strong>{article.likeCount}</strong> 点赞</span>
              <span><strong>{article.favoriteCount ?? 0}</strong> 收藏</span>
            </div>
            <div className="action-row">
              <button className="ghost-button icon-text-button" type="button" onClick={() => navigate(`/users/${article.userId}`)}><UserRound size={17} aria-hidden="true" />作者主页</button>
              <button className="primary-button icon-text-button" disabled={actionLoading} type="button" onClick={() => handleAction(() => likeArticle(articleId), '点赞成功。')}><Heart size={17} aria-hidden="true" />点赞</button>
              <button className="ghost-button icon-text-button" disabled={actionLoading} type="button" onClick={() => handleAction(() => favoriteArticle(articleId), '收藏成功。')}><Bookmark size={17} aria-hidden="true" />收藏</button>
              <details className="article-more-actions"><summary>更多操作</summary><button type="button" disabled={actionLoading} onClick={() => handleAction(() => unlikeArticle(articleId), '已取消点赞。')}>取消点赞</button><button type="button" disabled={actionLoading} onClick={() => handleAction(() => unfavoriteArticle(articleId), '已取消收藏。')}>取消收藏</button><button type="button" disabled={actionLoading || !currentUser} onClick={() => setReportTarget({ type: 'article' })}>举报文章</button></details>
            </div>
          </footer>
        </article>
      )}

      <section className="comment-section" aria-labelledby="comment-title">
        <div className="sub-heading"><div><p className="eyebrow">继续讨论</p><h2 id="comment-title"><MessageCircle size={19} aria-hidden="true" />评论</h2></div><span>{comments.length} 条</span></div>
        <form className="comment-form" onSubmit={handleCommentSubmit}>
          <label>
            <span className="sr-only">评论内容</span>
            <textarea disabled={actionLoading} onChange={(event) => setCommentContent(event.target.value)} placeholder={currentUser ? '写下你的看法…' : '登录后可以参与讨论'} value={commentContent} required />
          </label>
          <button className="primary-button" disabled={actionLoading} type="submit">发表评论</button>
        </form>
        <div className="comment-list">
          {comments.map((comment) => (
            <article className="comment-item" key={comment.id}>
              <header><strong>用户 #{comment.userId}</strong><span>{comment.createTime}</span></header>
              <p>{comment.content}</p>
              {currentUser && <button className="text-button" disabled={actionLoading} type="button" onClick={() => setReportTarget({ type: 'comment', id: comment.id })}><Flag size={15} aria-hidden="true" />举报评论</button>}
            </article>
          ))}
          {comments.length === 0 && <p className="empty-inline">暂无评论，来留下第一条观点吧。</p>}
        </div>
      </section>

      {reportTarget && <ConfirmDialog title={reportTarget.type === 'article' ? '举报文章' : '举报评论'} message="请说明具体原因，管理员会据此处理。" inputLabel="举报原因" onConfirm={handleReport} onCancel={() => setReportTarget(null)} confirmText="提交举报" danger />}
    </div>
  )
}

export default ArticleDetail
