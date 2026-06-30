import { useEffect, useState } from 'react'
import { pageMyArticles, pageMyFavorites, submitArticle } from '../api/articleApi.js'
import { pageMyReports } from '../api/reportApi.js'

const statusOptions = [
  { value: '', label: '全部状态' },
  { value: 'DRAFT', label: '草稿' },
  { value: 'PENDING', label: '待审核' },
  { value: 'PUBLISHED', label: '已发布' },
  { value: 'REJECTED', label: '已驳回' },
  { value: 'OFFLINE', label: '已下架' },
]

const statusLabels = {
  DRAFT: '草稿',
  PENDING: '待审核',
  PUBLISHED: '已发布',
  REJECTED: '已驳回',
  OFFLINE: '已下架',
}

const reportStatusLabels = {
  PENDING: '待处理',
  RESOLVED: '已处理',
  REJECTED: '已驳回',
}

const targetTypeLabels = {
  ARTICLE: '文章',
  COMMENT: '评论',
}

function MyArticles({ currentUser, refreshKey }) {
  const [articles, setArticles] = useState([])
  const [activeTab, setActiveTab] = useState('articles')
  const [status, setStatus] = useState('')
  const [pageInfo, setPageInfo] = useState({ current: 1, pages: 1, total: 0 })
  const [pageNum, setPageNum] = useState(1)
  const [message, setMessage] = useState('')
  const [loading, setLoading] = useState(false)
  const [localRefreshKey, setLocalRefreshKey] = useState(0)

  useEffect(() => {
    async function loadMyArticles() {
      if (!currentUser) {
        setArticles([])
        setMessage('请先登录。')
        return
      }

      setLoading(true)
      setMessage('')

      try {
        const params = { pageNum, pageSize: 5 }
        if (activeTab === 'articles' && status) {
          params.status = status
        }
        let result
        if (activeTab === 'favorites') {
          result = await pageMyFavorites(params)
        } else if (activeTab === 'reports') {
          result = await pageMyReports(params)
        } else {
          result = await pageMyArticles(params)
        }
        setArticles(result.data.records || [])
        setPageInfo({
          current: result.data.current,
          pages: result.data.pages,
          total: result.data.total,
        })
      } catch (error) {
        setArticles([])
        setMessage(error.message)
      } finally {
        setLoading(false)
      }
    }

    loadMyArticles()
  }, [activeTab, currentUser, pageNum, refreshKey, localRefreshKey, status])

  function switchTab(tab) {
    setActiveTab(tab)
    setPageNum(1)
    setMessage('')
  }

  function changeStatus(event) {
    setStatus(event.target.value)
    setPageNum(1)
  }

  async function handleSubmit(articleId) {
    setLoading(true)
    setMessage('')

    try {
      await submitArticle(articleId)
      setMessage(`文章 #${articleId} 已提交审核。`)
      setLocalRefreshKey((current) => current + 1)
    } catch (error) {
      setMessage(error.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="content-view">
      <div className="section-heading">
        <div>
          <h2>{activeTab === 'favorites' ? '我的收藏' : '我的文章'}</h2>
          <p>
            {activeTab === 'reports'
              ? '这里记录你提交过的文章和评论举报，以及管理员处理状态。'
              : activeTab === 'favorites'
                ? '这里只展示当前用户收藏且仍处于已发布状态的文章。'
                : '草稿、待审核、已发布、已驳回和已下架都会显示在这里。'}
          </p>
        </div>
        <span>{pageInfo.total} 条</span>
      </div>

      <div className="tabs compact-tabs">
        <button
          className={activeTab === 'articles' ? 'active' : ''}
          type="button"
          onClick={() => switchTab('articles')}
        >
          我的文章
        </button>
        <button
          className={activeTab === 'favorites' ? 'active' : ''}
          type="button"
          onClick={() => switchTab('favorites')}
        >
          我的收藏
        </button>
        <button
          className={activeTab === 'reports' ? 'active' : ''}
          type="button"
          onClick={() => switchTab('reports')}
        >
          我的举报
        </button>
      </div>

      {activeTab === 'articles' && (
        <label className="filter-row">
          状态
          <select value={status} onChange={changeStatus}>
            {statusOptions.map((option) => (
              <option key={option.value || 'all'} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </label>
      )}

      {loading && <p className="form-message">加载数据中...</p>}
      {message && <p className="form-message">{message}</p>}

      <div className="article-list">
        {activeTab === 'reports' && articles.map((report) => (
          <article className="article-item report-item" key={report.id}>
            <div>
              <span>
                #{report.id} · {targetTypeLabels[report.targetType] || report.targetType} #{report.targetId}
              </span>
              <h3>{report.reason}</h3>
              <p>
                被举报用户 #{report.targetOwnerId}
                {report.handleResult ? ` · 处理说明：${report.handleResult}` : ''}
              </p>
            </div>
            <div className="article-actions">
              <span className={`status-badge status-${report.status?.toLowerCase()}`}>
                {reportStatusLabels[report.status] || report.status}
              </span>
            </div>
          </article>
        ))}

        {activeTab !== 'reports' && articles.map((article) => (
          <article className="article-item" key={article.id}>
            <div>
              <span>
                #{article.id} {article.categoryName ? `· ${article.categoryName}` : ''}
              </span>
              <h3>{article.title}</h3>
              <p>{article.content}</p>
            </div>
            <div className="article-actions">
              <span className={`status-badge status-${article.status?.toLowerCase()}`}>
                {statusLabels[article.status] || article.status}
              </span>
              {activeTab === 'favorites' && (
                <span className="status-badge status-published">已收藏</span>
              )}
              {activeTab === 'articles' && (article.status === 'DRAFT' || article.status === 'REJECTED') && (
                <button
                  className="ghost-button"
                  disabled={loading}
                  type="button"
                  onClick={() => handleSubmit(article.id)}
                >
                  提交审核
                </button>
              )}
            </div>
          </article>
        ))}
        {articles.length === 0 && <p className="form-message">暂无数据。</p>}
      </div>

      <div className="pager">
        <button
          className="ghost-button"
          disabled={pageInfo.current <= 1 || loading}
          type="button"
          onClick={() => setPageNum((current) => current - 1)}
        >
          上一页
        </button>
        <span>
          第 {pageInfo.current} / {pageInfo.pages || 1} 页
        </span>
        <button
          className="ghost-button"
          disabled={pageInfo.current >= pageInfo.pages || loading}
          type="button"
          onClick={() => setPageNum((current) => current + 1)}
        >
          下一页
        </button>
      </div>
    </div>
  )
}

export default MyArticles
