import { useEffect, useState } from 'react'
import {
  markAllNotificationsRead,
  markNotificationRead,
  pageNotifications,
} from '../api/notificationApi.js'

const typeLabels = {
  COMMENT: '评论',
  LIKE: '点赞',
  FAVORITE: '收藏',
  AUDIT_APPROVED: '审核通过',
  AUDIT_REJECTED: '审核驳回',
  ARTICLE_OFFLINE: '文章下架',
}

function NotificationCenter({ currentUser, refreshKey, onOpenArticle, onUnreadChanged }) {
  const [notifications, setNotifications] = useState([])
  const [pageInfo, setPageInfo] = useState({ current: 1, pages: 1, total: 0 })
  const [pageNum, setPageNum] = useState(1)
  const [message, setMessage] = useState('')
  const [loading, setLoading] = useState(false)
  const [localRefreshKey, setLocalRefreshKey] = useState(0)

  async function loadNotifications() {
    if (!currentUser) {
      setNotifications([])
      setMessage('请先登录。')
      return
    }

    setLoading(true)
    setMessage('')

    try {
      const result = await pageNotifications({ pageNum, pageSize: 8 })
      setNotifications(result.data.records || [])
      setPageInfo({
        current: result.data.current,
        pages: result.data.pages,
        total: result.data.total,
      })
    } catch (error) {
      setNotifications([])
      setMessage(error.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadNotifications()
  }, [currentUser, pageNum, refreshKey, localRefreshKey])

  async function handleRead(notificationId) {
    setLoading(true)
    setMessage('')

    try {
      await markNotificationRead(notificationId)
      setMessage('通知已标记为已读。')
      setLocalRefreshKey((current) => current + 1)
      onUnreadChanged?.()
    } catch (error) {
      setMessage(error.message)
    } finally {
      setLoading(false)
    }
  }

  async function handleReadAll() {
    setLoading(true)
    setMessage('')

    try {
      await markAllNotificationsRead()
      setMessage('全部通知已标记为已读。')
      setLocalRefreshKey((current) => current + 1)
      onUnreadChanged?.()
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
          <h2>通知中心</h2>
          <p>评论、点赞、收藏和文章审核结果会汇总在这里。</p>
        </div>
        <button
          className="ghost-button"
          disabled={!currentUser || loading || notifications.length === 0}
          type="button"
          onClick={handleReadAll}
        >
          全部已读
        </button>
      </div>

      {loading && <p className="form-message">加载通知中...</p>}
      {message && <p className="form-message">{message}</p>}

      <div className="notification-list">
        {notifications.map((notification) => (
          <article
            className={`notification-item ${notification.readStatus === 0 ? 'unread' : ''}`}
            key={notification.id}
          >
            <div className="notification-main">
              <span className="status-badge">
                {typeLabels[notification.type] || notification.type}
              </span>
              <div>
                <h3>{notification.title}</h3>
                <p>{notification.content}</p>
                <small>{notification.createTime}</small>
              </div>
            </div>
            <div className="notification-actions">
              {notification.articleId && (
                <button
                  className="ghost-button compact-button"
                  type="button"
                  onClick={() => onOpenArticle?.(notification.articleId)}
                >
                  查看文章
                </button>
              )}
              {notification.readStatus === 0 ? (
                <button
                  className="primary-button compact-button"
                  disabled={loading}
                  type="button"
                  onClick={() => handleRead(notification.id)}
                >
                  标记已读
                </button>
              ) : (
                <span className="status-badge status-published">已读</span>
              )}
            </div>
          </article>
        ))}
        {notifications.length === 0 && <p className="form-message">暂无通知。</p>}
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
          第 {pageInfo.current} / {pageInfo.pages || 1} 页，共 {pageInfo.total} 条
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

export default NotificationCenter

