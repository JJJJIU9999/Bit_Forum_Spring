import { useEffect, useState } from 'react'
import { pageMyArticles, pageMyFavorites, submitArticle, updateArticle } from '../api/articleApi.js'
import { pageMyReports } from '../api/reportApi.js'
import { getCurrentUserProfile, updateCurrentUserProfile } from '../api/userApi.js'
import { uploadArticleCover, uploadAvatar } from '../api/uploadApi.js'
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
  const [profile, setProfile] = useState(null)
  const [profileForm, setProfileForm] = useState({ avatar: '', nickname: '', bio: '' })
  const [avatarFile, setAvatarFile] = useState(null)
  const [avatarInputKey, setAvatarInputKey] = useState(0)
  const [avatarUploading, setAvatarUploading] = useState(false)
  const [coverEdits, setCoverEdits] = useState({})
  const [coverFiles, setCoverFiles] = useState({})
  const [coverUploadingId, setCoverUploadingId] = useState(null)
  const [coverSavingId, setCoverSavingId] = useState(null)

  useEffect(() => {
    async function loadContent() {
      if (!currentUser) {
        setArticles([])
        setProfile(null)
        setMessage('请先登录。')
        setMessageType('error')
        return
      }

      setLoading(true)
      setMessage('')

      try {
        if (activeTab === 'profile') {
          const result = await getCurrentUserProfile()
          const nextProfile = result.data
          setProfile(nextProfile)
          setProfileForm({
            avatar: nextProfile.avatar || '',
            nickname: nextProfile.nickname || '',
            bio: nextProfile.bio || '',
          })
          setPageInfo({ current: 1, pages: 1, total: 0 })
          return
        }

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
        const records = result.data.records || []
        setArticles(records)
        if (activeTab === 'articles') {
          setCoverEdits(Object.fromEntries(records.map((article) => [article.id, article.coverUrl || ''])))
        }
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

    loadContent()
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

  function handleProfileChange(event) {
    const { name, value } = event.target
    setProfileForm((form) => ({ ...form, [name]: value }))
  }

  async function handleAvatarUpload() {
    if (!avatarFile) {
      setMessage('请选择要上传的头像图片。')
      setMessageType('error')
      return
    }

    setAvatarUploading(true)
    setMessage('')

    try {
      const result = await uploadAvatar(avatarFile)
      setProfileForm((form) => ({ ...form, avatar: result.data.url }))
      setMessage('头像上传成功，请保存资料使头像生效。')
      setMessageType('info')
      setAvatarFile(null)
      setAvatarInputKey((key) => key + 1)
    } catch (error) {
      setMessage(error.message)
      setMessageType('error')
    } finally {
      setAvatarUploading(false)
    }
  }

  async function handleProfileSubmit(event) {
    event.preventDefault()
    setLoading(true)
    setMessage('')

    try {
      const result = await updateCurrentUserProfile(profileForm)
      setProfile(result.data)
      setProfileForm({
        avatar: result.data.avatar || '',
        nickname: result.data.nickname || '',
        bio: result.data.bio || '',
      })
      setMessage('个人资料已更新。')
      setMessageType('info')
    } catch (error) {
      setMessage(error.message)
      setMessageType('error')
    } finally {
      setLoading(false)
    }
  }

  function handleCoverUrlChange(articleId, value) {
    setCoverEdits((current) => ({ ...current, [articleId]: value }))
  }

  function handleCoverFileChange(articleId, file) {
    setCoverFiles((current) => ({ ...current, [articleId]: file }))
  }

  async function handleCoverUpload(articleId) {
    const file = coverFiles[articleId]
    if (!file) {
      setMessage('请选择要上传的封面图片。')
      setMessageType('error')
      return
    }

    setCoverUploadingId(articleId)
    setMessage('')

    try {
      const result = await uploadArticleCover(file)
      handleCoverUrlChange(articleId, result.data.url)
      setCoverFiles((current) => ({ ...current, [articleId]: null }))
      setMessage(`文章 #${articleId} 封面上传成功，请保存封面。`)
      setMessageType('info')
    } catch (error) {
      setMessage(error.message)
      setMessageType('error')
    } finally {
      setCoverUploadingId(null)
    }
  }

  async function handleCoverSave(article) {
    setCoverSavingId(article.id)
    setMessage('')

    try {
      await updateArticle({
        articleId: article.id,
        title: article.title,
        content: article.content,
        coverUrl: (coverEdits[article.id] || '').trim() || null,
      })
      setMessage(`文章 #${article.id} 封面已更新。`)
      setMessageType('info')
      setLocalRefreshKey((key) => key + 1)
    } catch (error) {
      setMessage(error.message)
      setMessageType('error')
    } finally {
      setCoverSavingId(null)
    }
  }

  const tabTitles = {
    profile: '个人资料',
    articles: '我的文章',
    favorites: '我的收藏',
    reports: '我的举报',
  }

  const tabDescriptions = {
    profile: '维护头像 URL、昵称和个人简介。',
    articles: '草稿、待审核、已发布、已驳回和已下架的文章都会显示在这里。',
    favorites: '这里展示你收藏且仍处于已发布状态的文章。',
    reports: '你提交过的文章和评论举报，以及管理员处理状态。',
  }

  return (
    <div className="content-view">
      <PageHeader
        title={tabTitles[activeTab]}
        description={tabDescriptions[activeTab]}
        badge={activeTab === 'profile' ? currentUser?.username : `${pageInfo.total} 条`}
      />

      <div className="tabs">
        <button
          className={activeTab === 'profile' ? 'active' : ''}
          type="button"
          onClick={() => switchTab('profile')}
        >
          个人资料
        </button>
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

      {activeTab === 'profile' && (
        <section className="profile-panel">
          <div className="profile-summary">
            <div className="profile-avatar">
              {profileForm.avatar ? <img alt="头像" src={profileForm.avatar} /> : <span>{currentUser?.username?.slice(0, 1)}</span>}
            </div>
            <div>
              <h3>{profile?.nickname || profile?.username || currentUser?.username}</h3>
              <p>{profile?.bio || '还没有填写个人简介。'}</p>
              <div className="profile-stats">
                <span>{profile?.publishedArticleCount ?? 0} 篇已发布</span>
                <span>{profile?.favoriteCount ?? 0} 个收藏</span>
              </div>
            </div>
          </div>

          <form className="profile-form" onSubmit={handleProfileSubmit}>
            <label>
              头像 URL
              <input
                name="avatar"
                onChange={handleProfileChange}
                placeholder="https://example.com/avatar.png"
                value={profileForm.avatar}
              />
            </label>
            <div className="file-upload-row">
              <input
                key={avatarInputKey}
                accept="image/jpeg,image/png,image/webp"
                type="file"
                onChange={(event) => setAvatarFile(event.target.files?.[0] || null)}
              />
              <button
                className="ghost-button"
                disabled={loading || avatarUploading}
                type="button"
                onClick={handleAvatarUpload}
              >
                {avatarUploading ? '上传中...' : '上传头像'}
              </button>
            </div>
            <label>
              昵称
              <input
                maxLength={50}
                name="nickname"
                onChange={handleProfileChange}
                placeholder="输入昵称"
                value={profileForm.nickname}
              />
            </label>
            <label>
              个人简介
              <textarea
                maxLength={255}
                name="bio"
                onChange={handleProfileChange}
                placeholder="介绍一下自己"
                value={profileForm.bio}
              />
            </label>
            <button className="primary-button" disabled={loading || avatarUploading} type="submit">
              保存资料
            </button>
          </form>
        </section>
      )}

      {activeTab !== 'profile' && <div className="article-list">
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
                  <div className="article-cover-editor">
                    <label>
                      封面 URL
                      <input
                        value={coverEdits[article.id] ?? article.coverUrl ?? ''}
                        onChange={(event) => handleCoverUrlChange(article.id, event.target.value)}
                        placeholder="/uploads/article-cover/example.jpg"
                      />
                    </label>
                    <div className="file-upload-row">
                      <input
                        accept="image/jpeg,image/png,image/webp"
                        type="file"
                        onChange={(event) => handleCoverFileChange(article.id, event.target.files?.[0] || null)}
                      />
                      <button
                        className="ghost-button compact-button"
                        disabled={coverUploadingId === article.id || coverSavingId === article.id}
                        type="button"
                        onClick={() => handleCoverUpload(article.id)}
                      >
                        {coverUploadingId === article.id ? '上传中...' : '上传封面'}
                      </button>
                    </div>
                    {(coverEdits[article.id] || article.coverUrl) && (
                      <img
                        className="cover-preview compact-cover-preview"
                        alt={`${article.title} 封面预览`}
                        src={coverEdits[article.id] || article.coverUrl}
                      />
                    )}
                    <div className="form-action-row">
                      <button
                        className="ghost-button"
                        disabled={loading || coverUploadingId === article.id || coverSavingId === article.id}
                        type="button"
                        onClick={() => handleCoverSave(article)}
                      >
                        {coverSavingId === article.id ? '保存中...' : '保存封面'}
                      </button>
                      <button
                        className="primary-button"
                        disabled={loading || coverUploadingId === article.id || coverSavingId === article.id}
                        type="button"
                        onClick={() => handleSubmit(article.id)}
                      >
                        提交审核
                      </button>
                    </div>
                  </div>
                )}
            </ArticleCard>
          ))}

        {!loading && articles.length === 0 && <EmptyState />}
      </div>
      }

      {activeTab !== 'profile' && pageInfo.total > 0 && (
        <Pagination pageInfo={pageInfo} onPageChange={setPageNum} loading={loading} />
      )}
    </div>
  )
}

export default MyArticles
