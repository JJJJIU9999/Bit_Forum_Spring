import { useEffect, useState } from 'react'
import { useOutletContext } from 'react-router'
import { checkAdminHealth, getAdminDashboardSummary } from '../../api/adminApi.js'
import PageHeader from '../../components/PageHeader.jsx'
import LoadingSpinner from '../../components/LoadingSpinner.jsx'
import MessageBanner from '../../components/MessageBanner.jsx'
import { formatNumber } from './adminHelpers.js'

function Dashboard() {
  const { currentUser } = useOutletContext()
  const [dashboardSummary, setDashboardSummary] = useState(null)
  const [message, setMessage] = useState('')
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    async function load() {
      if (!currentUser) return
      setLoading(true)
      setMessage('')
      try {
        await checkAdminHealth()
        const result = await getAdminDashboardSummary()
        setDashboardSummary(result.data)
      } catch (error) {
        setMessage(error.message)
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [currentUser])

  if (loading) return <LoadingSpinner text="加载看板数据..." />
  if (message) return <MessageBanner message={message} type="error" />
  if (!dashboardSummary) return <MessageBanner message="暂无看板数据。" type="error" />

  const {
    userStats = {},
    articleStats = {},
    commentStats = {},
    categoryStats = {},
    favoriteStats = {},
    notificationStats = {},
    reportStats = {},
    hotArticles = [],
  } = dashboardSummary

  const headlineStats = [
    { title: '用户总数', value: userStats.total, detail: `启用 ${formatNumber(userStats.enabled)} / 禁用 ${formatNumber(userStats.disabled)}` },
    { title: '文章总数', value: articleStats.total, detail: `今日新增 ${formatNumber(articleStats.todayCreated)}` },
    { title: '评论总数', value: commentStats.total, detail: `近 7 天 ${formatNumber(commentStats.last7DaysCreated)}` },
    { title: '待处理举报', value: reportStats.pending, detail: `举报总数 ${formatNumber(reportStats.total)}` },
  ]

  const groups = [
    {
      title: '用户', metrics: [
        { label: '普通用户', value: userStats.normalUsers },
        { label: '管理员', value: userStats.admins },
        { label: '启用', value: userStats.enabled },
        { label: '禁用', value: userStats.disabled },
      ],
    },
    {
      title: '文章状态', metrics: [
        { label: '草稿', value: articleStats.draft },
        { label: '待审核', value: articleStats.pending },
        { label: '已发布', value: articleStats.published },
        { label: '已驳回', value: articleStats.rejected },
        { label: '已下架', value: articleStats.offline },
        { label: '近 7 天新增', value: articleStats.last7DaysCreated },
      ],
    },
    {
      title: '内容与互动', metrics: [
        { label: '今日评论', value: commentStats.todayCreated },
        { label: '近 7 天评论', value: commentStats.last7DaysCreated },
        { label: '收藏总数', value: favoriteStats.total },
        { label: '通知总数', value: notificationStats.total },
        { label: '未读通知', value: notificationStats.unread },
      ],
    },
    {
      title: '板块与治理', metrics: [
        { label: '板块总数', value: categoryStats.total },
        { label: '启用板块', value: categoryStats.enabled },
        { label: '禁用板块', value: categoryStats.disabled },
        { label: '已处理举报', value: reportStats.resolved },
        { label: '已驳回举报', value: reportStats.rejected },
        { label: '近 7 天举报', value: reportStats.last7DaysCreated },
      ],
    },
  ]

  return (
    <div className="content-view admin-view">
      <PageHeader title="数据看板" description="平台核心数据概览与统计。" />

      <div className="dashboard-board">
        <div className="dashboard-stat-grid">
          {headlineStats.map((s) => (
            <article className="dashboard-stat-card" key={s.title}>
              <span>{s.title}</span>
              <strong>{formatNumber(s.value)}</strong>
              {s.detail && <small>{s.detail}</small>}
            </article>
          ))}
        </div>

        <div className="dashboard-section-grid">
          {groups.map((g) => (
            <section className="dashboard-section" key={g.title}>
              <div className="sub-heading">
                <h3>{g.title}</h3>
                <span>{g.metrics.length} 项</span>
              </div>
              <div className="dashboard-mini-grid">
                {g.metrics.map((m) => (
                  <div className="dashboard-mini-stat" key={m.label}>
                    <span>{m.label}</span>
                    <strong>{formatNumber(m.value)}</strong>
                  </div>
                ))}
              </div>
            </section>
          ))}
        </div>

        <section className="dashboard-section">
          <div className="sub-heading">
            <h3>热门文章 Top 10</h3>
            <span>{hotArticles.length} 条</span>
          </div>
          <div className="dashboard-hot-list">
            {hotArticles.length === 0 ? (
              <p>暂无热门文章。</p>
            ) : (
              hotArticles.map((article, index) => (
                <div className="dashboard-hot-item" key={article.articleId}>
                  <span>{String(index + 1).padStart(2, '0')}</span>
                  <strong title={article.title}>{article.title}</strong>
                  <small>热度 {formatNumber(article.hotScore)}</small>
                  <small>浏览 {formatNumber(article.viewCount)}</small>
                  <small>点赞 {formatNumber(article.likeCount)}</small>
                </div>
              ))
            )}
          </div>
        </section>
      </div>
    </div>
  )
}

export default Dashboard
