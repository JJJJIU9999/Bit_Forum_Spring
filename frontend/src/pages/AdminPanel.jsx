import { useEffect, useState } from 'react'
import {
  checkAdminHealth,
  deleteAdminArticle,
  deleteAdminComment,
  disableUser,
  enableUser,
  pageAdminArticles,
  pageAdminComments,
  pageAdminUsers,
} from '../api/adminApi.js'

const adminTabs = [
  { key: 'articles', label: '文章管理' },
  { key: 'comments', label: '评论管理' },
  { key: 'users', label: '用户管理' },
]

function formatDate(dateText) {
  if (!dateText) {
    return '-'
  }

  // 后端 LocalDateTime 默认会返回 2026-06-03T14:00:00 这种格式，页面只展示到分钟。
  return dateText.replace('T', ' ').slice(0, 16)
}

function AdminPanel({ currentUser }) {
  const [activeTab, setActiveTab] = useState('articles')
  const [records, setRecords] = useState([])
  const [pageInfo, setPageInfo] = useState({ current: 1, pages: 1, total: 0 })
  const [pageNum, setPageNum] = useState(1)
  const [message, setMessage] = useState('')
  const [loading, setLoading] = useState(false)
  const [refreshKey, setRefreshKey] = useState(0)

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
        // health 接口用来验证当前 JWT 是否真的具备 ADMIN 权限。
        await checkAdminHealth()

        let result
        if (activeTab === 'articles') {
          result = await pageAdminArticles({ pageNum, pageSize: 5 })
        } else if (activeTab === 'comments') {
          result = await pageAdminComments({ pageNum, pageSize: 5 })
        } else {
          result = await pageAdminUsers({ pageNum, pageSize: 5 })
        }

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
  }, [activeTab, currentUser, pageNum, refreshKey])

  function switchTab(tabKey) {
    setActiveTab(tabKey)
    setPageNum(1)
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

  function renderArticles() {
    return records.map((article) => (
      <tr key={article.id}>
        <td className="mono-cell">#{article.id}</td>
        <td className="strong-cell truncate-cell">{article.title}</td>
        <td className="mono-cell">{article.userId}</td>
        <td className="mono-cell">{article.viewCount}</td>
        <td className="mono-cell">{article.likeCount}</td>
        <td className="action-cell">
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

  return (
    <div className="content-view admin-view">
      <div className="section-heading">
        <div>
          <h2>管理员页面</h2>
          <p>这些接口都在 `/api/admin/**` 下，权限由后端 AdminInterceptor 校验。</p>
        </div>
        <span>{pageInfo.total} 条</span>
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

      {loading && <p className="form-message">加载管理员数据中...</p>}
      {message && <p className="form-message">{message}</p>}

      <div className="admin-table-wrap">
        <table className={`admin-table admin-table-${activeTab}`}>
          {activeTab === 'articles' && (
            <colgroup>
              <col className="col-id" />
              <col className="col-title" />
              <col className="col-small" />
              <col className="col-small" />
              <col className="col-small" />
              <col className="col-action" />
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
          <thead>
            {activeTab === 'articles' && (
              <tr>
                <th>ID</th>
                <th>标题</th>
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
          </thead>
          <tbody>
            {activeTab === 'articles' && renderArticles()}
            {activeTab === 'comments' && renderComments()}
            {activeTab === 'users' && renderUsers()}
            {records.length === 0 && (
              <tr>
                <td className="empty-cell" colSpan="6">
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
    </div>
  )
}

export default AdminPanel
