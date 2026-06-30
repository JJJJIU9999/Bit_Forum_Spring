import { useEffect, useState } from 'react'
import {
  approveArticle,
  checkAdminHealth,
  createCategory,
  deleteAdminArticle,
  deleteAdminComment,
  deleteCategory,
  disableCategory,
  disableUser,
  enableCategory,
  enableUser,
  getAdminDashboardSummary,
  offlineArticle,
  pageAdminArticles,
  pageAdminCategories,
  pageAdminComments,
  pageAdminReports,
  pageAdminUsers,
  pageAuditArticles,
  rejectReport,
  rejectArticle,
  resolveReport,
  updateCategory,
} from '../api/adminApi.js'

const adminTabs = [
  { key: 'dashboard', label: '数据看板' },
  { key: 'audit', label: '文章审核' },
  { key: 'articles', label: '文章管理' },
  { key: 'comments', label: '评论管理' },
  { key: 'reports', label: '举报处理' },
  { key: 'users', label: '用户管理' },
  { key: 'categories', label: '板块管理' },
]

function formatDate(dateText) {
  if (!dateText) {
    return '-'
  }
  return dateText.replace('T', ' ').slice(0, 16)
}

function formatNumber(value) {
  return Number(value || 0).toLocaleString('zh-CN')
}

const emptyCategoryForm = {
  categoryId: null,
  name: '',
  description: '',
  sortOrder: 0,
}

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

function AdminPanel({ currentUser }) {
  const [activeTab, setActiveTab] = useState('dashboard')
  const [records, setRecords] = useState([])
  const [dashboardSummary, setDashboardSummary] = useState(null)
  const [pageInfo, setPageInfo] = useState({ current: 1, pages: 1, total: 0 })
  const [pageNum, setPageNum] = useState(1)
  const [message, setMessage] = useState('')
  const [loading, setLoading] = useState(false)
  const [refreshKey, setRefreshKey] = useState(0)
  const [categoryForm, setCategoryForm] = useState(emptyCategoryForm)
  const [reportStatus, setReportStatus] = useState('PENDING')

  useEffect(() => {
    async function loadAdminData() {
      if (!currentUser) {
        setRecords([])
        setMessage('请先登录管理员账号。')
        return
      }

      setLoading(true)
      setMessage('')

      try {
        await checkAdminHealth()

        let result
        if (activeTab === 'dashboard') {
          result = await getAdminDashboardSummary()
          setDashboardSummary(result.data)
          setRecords([])
          setPageInfo({
            current: 1,
            pages: 1,
            total: result.data?.articleStats?.total || 0,
          })
          return
        } else if (activeTab === 'audit') {
          result = await pageAuditArticles({ status: 'PENDING', pageNum, pageSize: 5 })
        } else if (activeTab === 'articles') {
          result = await pageAdminArticles({ pageNum, pageSize: 5 })
        } else if (activeTab === 'comments') {
          result = await pageAdminComments({ pageNum, pageSize: 5 })
        } else if (activeTab === 'reports') {
          result = await pageAdminReports({ status: reportStatus, pageNum, pageSize: 5 })
        } else if (activeTab === 'users') {
          result = await pageAdminUsers({ pageNum, pageSize: 5 })
        } else {
          result = await pageAdminCategories({ pageNum, pageSize: 5 })
        }

        setDashboardSummary(null)
        setRecords(result.data.records || [])
        setPageInfo({
          current: result.data.current,
          pages: result.data.pages,
          total: result.data.total,
        })
      } catch (error) {
        setRecords([])
        setMessage(error.message)
      } finally {
        setLoading(false)
      }
    }

    loadAdminData()
  }, [activeTab, currentUser, pageNum, refreshKey, reportStatus])

  function switchTab(tabKey) {
    setActiveTab(tabKey)
    setPageNum(1)
    setMessage('')
  }

  async function runAdminAction(action, successMessage) {
    setLoading(true)
    setMessage('')

    try {
      await action()
      setMessage(successMessage)
      setRefreshKey((current) => current + 1)
    } catch (error) {
      setMessage(error.message)
    } finally {
      setLoading(false)
    }
  }

  async function rejectWithReason(articleId) {
    const reason = window.prompt('请输入驳回原因')
    if (!reason) {
      setMessage('驳回原因不能为空。')
      return
    }

    await runAdminAction(
      () => rejectArticle({ articleId, reason }),
      `文章 #${articleId} 已驳回。`,
    )
  }

  async function offlineWithReason(articleId) {
    const reason = window.prompt('请输入下架原因')
    if (!reason) {
      setMessage('下架原因不能为空。')
      return
    }

    await runAdminAction(
      () => offlineArticle({ articleId, reason }),
      `文章 #${articleId} 已下架。`,
    )
  }

  async function resolveReportWithResult(reportId) {
    const handleResult = window.prompt('请输入处理说明')
    if (!handleResult) {
      setMessage('处理说明不能为空。')
      return
    }

    await runAdminAction(
      () => resolveReport({ reportId, handleResult }),
      `举报 #${reportId} 已处理为成立。`,
    )
  }

  async function rejectReportWithResult(reportId) {
    const handleResult = window.prompt('请输入驳回说明')
    if (!handleResult) {
      setMessage('处理说明不能为空。')
      return
    }

    await runAdminAction(
      () => rejectReport({ reportId, handleResult }),
      `举报 #${reportId} 已驳回。`,
    )
  }

  function updateCategoryField(event) {
    const { name, value } = event.target
    setCategoryForm((current) => ({ ...current, [name]: value }))
  }

  function editCategory(category) {
    setCategoryForm({
      categoryId: category.id,
      name: category.name || '',
      description: category.description || '',
      sortOrder: category.sortOrder ?? 0,
    })
  }

  async function submitCategory(event) {
    event.preventDefault()

    const payload = {
      name: categoryForm.name,
      description: categoryForm.description,
      sortOrder: Number(categoryForm.sortOrder || 0),
    }

    if (categoryForm.categoryId) {
      await runAdminAction(
        () => updateCategory({ ...payload, categoryId: categoryForm.categoryId }),
        `板块 #${categoryForm.categoryId} 已更新。`,
      )
    } else {
      await runAdminAction(() => createCategory(payload), '板块已创建。')
    }

    setCategoryForm(emptyCategoryForm)
  }

  function renderStatCard(title, value, detail) {
    return (
      <article className="dashboard-stat-card" key={title}>
        <span>{title}</span>
        <strong>{formatNumber(value)}</strong>
        {detail && <small>{detail}</small>}
      </article>
    )
  }

  function renderMetricGroup(title, metrics) {
    return (
      <section className="dashboard-section" key={title}>
        <div className="sub-heading">
          <h3>{title}</h3>
          <span>{metrics.length} 项</span>
        </div>
        <div className="dashboard-mini-grid">
          {metrics.map((metric) => (
            <div className="dashboard-mini-stat" key={metric.label}>
              <span>{metric.label}</span>
              <strong>{formatNumber(metric.value)}</strong>
            </div>
          ))}
        </div>
      </section>
    )
  }

  function renderDashboard() {
    if (!dashboardSummary) {
      return <p className="form-message">暂无看板数据。</p>
    }

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
      renderStatCard('用户总数', userStats.total, `启用 ${formatNumber(userStats.enabled)} / 禁用 ${formatNumber(userStats.disabled)}`),
      renderStatCard('文章总数', articleStats.total, `今日新增 ${formatNumber(articleStats.todayCreated)}`),
      renderStatCard('评论总数', commentStats.total, `近 7 天 ${formatNumber(commentStats.last7DaysCreated)}`),
      renderStatCard('待处理举报', reportStats.pending, `举报总数 ${formatNumber(reportStats.total)}`),
    ]

    const groups = [
      renderMetricGroup('用户', [
        { label: '普通用户', value: userStats.normalUsers },
        { label: '管理员', value: userStats.admins },
        { label: '启用', value: userStats.enabled },
        { label: '禁用', value: userStats.disabled },
      ]),
      renderMetricGroup('文章状态', [
        { label: '草稿', value: articleStats.draft },
        { label: '待审核', value: articleStats.pending },
        { label: '已发布', value: articleStats.published },
        { label: '已驳回', value: articleStats.rejected },
        { label: '已下架', value: articleStats.offline },
        { label: '近 7 天新增', value: articleStats.last7DaysCreated },
      ]),
      renderMetricGroup('内容与互动', [
        { label: '今日评论', value: commentStats.todayCreated },
        { label: '近 7 天评论', value: commentStats.last7DaysCreated },
        { label: '收藏总数', value: favoriteStats.total },
        { label: '通知总数', value: notificationStats.total },
        { label: '未读通知', value: notificationStats.unread },
      ]),
      renderMetricGroup('板块与治理', [
        { label: '板块总数', value: categoryStats.total },
        { label: '启用板块', value: categoryStats.enabled },
        { label: '禁用板块', value: categoryStats.disabled },
        { label: '已处理举报', value: reportStats.resolved },
        { label: '已驳回举报', value: reportStats.rejected },
        { label: '近 7 天举报', value: reportStats.last7DaysCreated },
      ]),
    ]

    return (
      <div className="dashboard-board">
        <div className="dashboard-stat-grid">{headlineStats}</div>
        <div className="dashboard-section-grid">{groups}</div>
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
    )
  }

  function renderArticles() {
    return records.map((article) => (
      <tr key={article.id}>
        <td className="mono-cell">#{article.id}</td>
        <td className="strong-cell truncate-cell">{article.title}</td>
        <td className="truncate-cell">{article.categoryName || '-'}</td>
        <td>
          <span className={`status-badge status-${article.status?.toLowerCase()}`}>
            {statusLabels[article.status] || article.status}
          </span>
        </td>
        <td className="mono-cell">{article.userId}</td>
        <td className="mono-cell">{article.viewCount}</td>
        <td className="mono-cell">{article.likeCount}</td>
        <td className="action-cell multi-action-cell">
          {article.status === 'PUBLISHED' && (
            <button
              className="danger-button"
              disabled={loading}
              type="button"
              onClick={() => offlineWithReason(article.id)}
            >
              下架
            </button>
          )}
          <button
            className="danger-button"
            disabled={loading}
            type="button"
            onClick={() =>
              runAdminAction(
                () => deleteAdminArticle(article.id),
                `文章 #${article.id} 已删除。`,
              )
            }
          >
            删除
          </button>
        </td>
      </tr>
    ))
  }

  function renderAuditArticles() {
    return records.map((article) => (
      <tr key={article.id}>
        <td className="mono-cell">#{article.id}</td>
        <td className="strong-cell truncate-cell">{article.title}</td>
        <td className="truncate-cell">{article.categoryName || '-'}</td>
        <td className="mono-cell">{article.userId}</td>
        <td className="mono-cell">{formatDate(article.createTime)}</td>
        <td className="action-cell multi-action-cell">
          <button
            className="ghost-button compact-button"
            disabled={loading}
            type="button"
            onClick={() =>
              runAdminAction(
                () => approveArticle(article.id),
                `文章 #${article.id} 已审核通过。`,
              )
            }
          >
            通过
          </button>
          <button
            className="danger-button"
            disabled={loading}
            type="button"
            onClick={() => rejectWithReason(article.id)}
          >
            驳回
          </button>
        </td>
      </tr>
    ))
  }

  function renderComments() {
    return records.map((comment) => (
      <tr key={comment.id}>
        <td className="mono-cell">#{comment.id}</td>
        <td className="mono-cell">{comment.articleId}</td>
        <td className="mono-cell">{comment.userId}</td>
        <td className="truncate-cell">{comment.content}</td>
        <td className="action-cell">
          <button
            className="danger-button"
            disabled={loading}
            type="button"
            onClick={() =>
              runAdminAction(
                () => deleteAdminComment(comment.id),
                `评论 #${comment.id} 已删除。`,
              )
            }
          >
            删除
          </button>
        </td>
      </tr>
    ))
  }

  function renderReports() {
    return records.map((report) => (
      <tr key={report.id}>
        <td className="mono-cell">#{report.id}</td>
        <td>{targetTypeLabels[report.targetType] || report.targetType}</td>
        <td className="mono-cell">#{report.targetId}</td>
        <td className="mono-cell">{report.reporterId}</td>
        <td className="mono-cell">{report.targetOwnerId}</td>
        <td className="truncate-cell">{report.reason}</td>
        <td>
          <span className={`status-badge status-${report.status?.toLowerCase()}`}>
            {reportStatusLabels[report.status] || report.status}
          </span>
        </td>
        <td className="action-cell multi-action-cell">
          {report.status === 'PENDING' ? (
            <>
              <button
                className="ghost-button compact-button"
                disabled={loading}
                type="button"
                onClick={() => resolveReportWithResult(report.id)}
              >
                成立
              </button>
              <button
                className="danger-button"
                disabled={loading}
                type="button"
                onClick={() => rejectReportWithResult(report.id)}
              >
                驳回
              </button>
            </>
          ) : (
            <span className="muted-cell">{report.handleResult || '-'}</span>
          )}
        </td>
      </tr>
    ))
  }

  function renderUsers() {
    return records.map((user) => {
      const disabled = user.status === 0

      return (
        <tr key={user.userId}>
          <td className="mono-cell">#{user.userId}</td>
          <td className="strong-cell truncate-cell">{user.username}</td>
          <td>{user.role}</td>
          <td>{disabled ? '禁用' : '启用'}</td>
          <td className="mono-cell">{formatDate(user.createTime)}</td>
          <td className="action-cell">
            <button
              className={disabled ? 'ghost-button compact-button' : 'danger-button'}
              disabled={loading}
              type="button"
              onClick={() =>
                runAdminAction(
                  () => (disabled ? enableUser(user.userId) : disableUser(user.userId)),
                  disabled ? `用户 #${user.userId} 已启用。` : `用户 #${user.userId} 已禁用。`,
                )
              }
            >
              {disabled ? '启用' : '禁用'}
            </button>
          </td>
        </tr>
      )
    })
  }

  function renderCategories() {
    return records.map((category) => {
      const disabled = category.status === 0

      return (
        <tr key={category.id}>
          <td className="mono-cell">#{category.id}</td>
          <td className="strong-cell truncate-cell">{category.name}</td>
          <td className="truncate-cell">{category.description || '-'}</td>
          <td className="mono-cell">{category.sortOrder}</td>
          <td>{disabled ? '禁用' : '启用'}</td>
          <td className="action-cell multi-action-cell">
            <button
              className="ghost-button compact-button"
              disabled={loading}
              type="button"
              onClick={() => editCategory(category)}
            >
              编辑
            </button>
            <button
              className={disabled ? 'ghost-button compact-button' : 'danger-button'}
              disabled={loading}
              type="button"
              onClick={() =>
                runAdminAction(
                  () => (disabled ? enableCategory(category.id) : disableCategory(category.id)),
                  disabled ? `板块 #${category.id} 已启用。` : `板块 #${category.id} 已禁用。`,
                )
              }
            >
              {disabled ? '启用' : '禁用'}
            </button>
            <button
              className="danger-button"
              disabled={loading}
              type="button"
              onClick={() =>
                runAdminAction(
                  () => deleteCategory(category.id),
                  `板块 #${category.id} 已删除。`,
                )
              }
            >
              删除
            </button>
          </td>
        </tr>
      )
    })
  }

  return (
    <div className="content-view admin-view">
      <div className="section-heading">
        <div>
          <h2>管理员页面</h2>
          <p>所有后台接口都在 `/api/admin/**` 下，权限由后端 AdminInterceptor 校验。</p>
        </div>
        <span>{activeTab === 'dashboard' ? '实时聚合' : `${pageInfo.total} 条`}</span>
      </div>

      <div className="admin-tabs">
        {adminTabs.map((tab) => (
          <button
            className={activeTab === tab.key ? 'active' : ''}
            key={tab.key}
            type="button"
            onClick={() => switchTab(tab.key)}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {activeTab === 'categories' && (
        <form className="admin-inline-form" onSubmit={submitCategory}>
          <input
            name="name"
            onChange={updateCategoryField}
            placeholder="板块名称"
            value={categoryForm.name}
          />
          <input
            name="description"
            onChange={updateCategoryField}
            placeholder="板块描述"
            value={categoryForm.description}
          />
          <input
            min="0"
            name="sortOrder"
            onChange={updateCategoryField}
            placeholder="排序"
            type="number"
            value={categoryForm.sortOrder}
          />
          <button className="primary-button" disabled={loading} type="submit">
            {categoryForm.categoryId ? '保存板块' : '新建板块'}
          </button>
          {categoryForm.categoryId && (
            <button
              className="ghost-button"
              type="button"
              onClick={() => setCategoryForm(emptyCategoryForm)}
            >
              取消编辑
            </button>
          )}
        </form>
      )}

      {activeTab === 'reports' && (
        <label className="filter-row admin-filter-row">
          举报状态
          <select
            value={reportStatus}
            onChange={(event) => {
              setReportStatus(event.target.value)
              setPageNum(1)
            }}
          >
            <option value="PENDING">待处理</option>
            <option value="RESOLVED">已处理</option>
            <option value="REJECTED">已驳回</option>
          </select>
        </label>
      )}

      {loading && <p className="form-message">加载管理员数据中...</p>}
      {message && <p className="form-message">{message}</p>}

      {activeTab === 'dashboard' && renderDashboard()}

      {activeTab !== 'dashboard' && (
        <>
      <div className="admin-table-wrap">
        <table className={`admin-table admin-table-${activeTab}`}>
          {activeTab === 'articles' && (
            <colgroup>
              <col className="col-id" />
              <col className="col-title" />
              <col className="col-title" />
              <col className="col-role" />
              <col className="col-small" />
              <col className="col-small" />
              <col className="col-small" />
              <col className="col-wide-action" />
            </colgroup>
          )}
          {activeTab === 'audit' && (
            <colgroup>
              <col className="col-id" />
              <col className="col-title" />
              <col className="col-title" />
              <col className="col-small" />
              <col className="col-date" />
              <col className="col-wide-action" />
            </colgroup>
          )}
          {activeTab === 'comments' && (
            <colgroup>
              <col className="col-id" />
              <col className="col-small" />
              <col className="col-small" />
              <col className="col-content" />
              <col className="col-action" />
            </colgroup>
          )}
          {activeTab === 'reports' && (
            <colgroup>
              <col className="col-id" />
              <col className="col-role" />
              <col className="col-small" />
              <col className="col-small" />
              <col className="col-small" />
              <col className="col-content" />
              <col className="col-role" />
              <col className="col-wide-action" />
            </colgroup>
          )}
          {activeTab === 'users' && (
            <colgroup>
              <col className="col-id" />
              <col className="col-title" />
              <col className="col-role" />
              <col className="col-small" />
              <col className="col-date" />
              <col className="col-action" />
            </colgroup>
          )}
          {activeTab === 'categories' && (
            <colgroup>
              <col className="col-id" />
              <col className="col-title" />
              <col className="col-content" />
              <col className="col-small" />
              <col className="col-small" />
              <col className="col-wide-action" />
            </colgroup>
          )}
          <thead>
            {activeTab === 'audit' && (
              <tr>
                <th>ID</th>
                <th>标题</th>
                <th>板块</th>
                <th>作者</th>
                <th>提交时间</th>
                <th>操作</th>
              </tr>
            )}
            {activeTab === 'articles' && (
              <tr>
                <th>ID</th>
                <th>标题</th>
                <th>板块</th>
                <th>状态</th>
                <th>作者</th>
                <th>浏览</th>
                <th>点赞</th>
                <th>操作</th>
              </tr>
            )}
            {activeTab === 'comments' && (
              <tr>
                <th>ID</th>
                <th>文章</th>
                <th>用户</th>
                <th>内容</th>
                <th>操作</th>
              </tr>
            )}
            {activeTab === 'reports' && (
              <tr>
                <th>ID</th>
                <th>对象</th>
                <th>对象ID</th>
                <th>举报人</th>
                <th>作者</th>
                <th>原因</th>
                <th>状态</th>
                <th>操作</th>
              </tr>
            )}
            {activeTab === 'users' && (
              <tr>
                <th>ID</th>
                <th>用户名</th>
                <th>角色</th>
                <th>状态</th>
                <th>注册时间</th>
                <th>操作</th>
              </tr>
            )}
            {activeTab === 'categories' && (
              <tr>
                <th>ID</th>
                <th>名称</th>
                <th>描述</th>
                <th>排序</th>
                <th>状态</th>
                <th>操作</th>
              </tr>
            )}
          </thead>
          <tbody>
            {activeTab === 'audit' && renderAuditArticles()}
            {activeTab === 'articles' && renderArticles()}
            {activeTab === 'comments' && renderComments()}
            {activeTab === 'reports' && renderReports()}
            {activeTab === 'users' && renderUsers()}
            {activeTab === 'categories' && renderCategories()}
            {records.length === 0 && (
              <tr>
                <td className="empty-cell" colSpan="8">
                  暂无数据。
                </td>
              </tr>
            )}
          </tbody>
        </table>
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
        </>
      )}
    </div>
  )
}

export default AdminPanel
