import { useEffect, useState } from 'react'
import { pageMyArticles, pageMyFavorites, submitArticle } from '../api/articleApi.js'
import { pageMyReports } from '../api/reportApi.js'
import PageHeader from '../components/PageHeader.jsx'
import ArticleCard from '../components/ArticleCard.jsx'
import StatusBadge from '../components/StatusBadge.jsx'
import Pagination from '../components/Pagination.jsx'
import EmptyState from '../components/EmptyState.jsx'
import LoadingSpinner from '../components/LoadingSpinner.jsx'
import MessageBanner from '../components/MessageBanner.jsx'

const statusOptions = [
  { value: '', label: '全部状态' },
  { value: 'DRAFT', label: '草稿' },
  { value: 'PENDING', label: '待审核' },
  { value: 'PUBLISHED', label: '已发布' },
  { value: 'REJECTED', label: '已驳回' },
  { value: 'OFFLINE', label: '已下架' },
]

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
  const [messageType, setMessageType] = useState('info')
  const [loading, setLoading] = useState(false)
  const [localRefreshKey, setLocalRefreshKey] = useState(0)

  useEffect(() => {
    async function loadMyArticles() {
      if (!currentUser) {
        setArticles([])
        setMessage('请先登录。')
        setMessageType('error')
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
        setMessageType('error')
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
      setMessageType('info')
      setLocalRefreshKey((k) => k + 1)
    } catch (error) {
      setMessage(error.message)
      setMessageType('error')
    } finally {
      setLoading(false)
    }
  }

  const tabTitles = {
    articles: '我的文章',
    favorites: '我的收藏',
    reports: '我的举报',
  }

  const tabDescriptions = {
    articles: '草稿、待审核、已发布、已驳回和已下架的文章都会显示在这里。',
    favorites: '这里展示你收藏且仍处于已发布状态的文章。',
    reports: '你提交过的文章和评论举报，以及管理员处理状态。',
  }

  return (
    <div className="content-view">
      <PageHeader
        title={tabTitles[activeTab]}
        description={tabDescriptions[activeTab]}
        badge={`${pageInfo.total} 条`}
      />

      <div className="tabs">
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

      {loading && <LoadingSpinner text="加载数据中..." />}
      <MessageBanner message={message} type={messageType} />

      <div className="article-list">
        {activeTab === 'reports' &&
          articles.map((report) => (
            <article className="article-item report-item" key={report.id}>
              <div>
                <span>
                  #{report.id} · {targetTypeLabels[report.targetType] || report.targetType} #
                  {report.targetId}
                </span>
                <h3>{report.reason}</h3>
                <p>
                  被举报用户 #{report.targetOwnerId}
                  {report.handleResult ? ` · 处理说明：${report.handleResult}` : ''}
                </p>
              </div>
              <div className="article-actions">
                <StatusBadge
                  status={report.status}
                  label={reportStatusLabels[report.status]}
                />
              </div>
            </article>
          ))}

        {activeTab !== 'reports' &&
          articles.map((article) => (
            <ArticleCard
              key={article.id}
              article={article}
              statusLabel={activeTab === 'favorites' ? '已收藏' : undefined}
            >
              {activeTab === 'articles' &&
                (article.status === 'DRAFT' || article.status === 'REJECTED') && (
                  <button
                    className="ghost-button"
                    disabled={loading}
                    type="button"
                    onClick={() => handleSubmit(article.id)}
                  >
                    提交审核
                  </button>
                )}
            </ArticleCard>
          ))}

        {!loading && articles.length === 0 && <EmptyState />}
      </div>

      {pageInfo.total > 0 && (
        <Pagination pageInfo={pageInfo} onPageChange={setPageNum} loading={loading} />
      )}
    </div>
  )
}

export default MyArticles
