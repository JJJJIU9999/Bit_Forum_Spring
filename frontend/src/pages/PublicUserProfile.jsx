import { useEffect, useState } from 'react'
import {
  followUser,
  getPublicUserProfile,
  pagePublicUserArticles,
  pageUserFollowers,
  pageUserFollowing,
  unfollowUser,
} from '../api/userApi.js'
import PageHeader from '../components/PageHeader.jsx'
import ArticleCard from '../components/ArticleCard.jsx'
import Pagination from '../components/Pagination.jsx'
import EmptyState from '../components/EmptyState.jsx'
import LoadingSpinner from '../components/LoadingSpinner.jsx'
import MessageBanner from '../components/MessageBanner.jsx'

function PublicUserProfile({ userId, currentUser, onBack, onOpenDetail }) {
  const [profile, setProfile] = useState(null)
  const [articles, setArticles] = useState([])
  const [pageInfo, setPageInfo] = useState({ current: 1, pages: 1, total: 0 })
  const [pageNum, setPageNum] = useState(1)
  const [followTab, setFollowTab] = useState('followers')
  const [followUsers, setFollowUsers] = useState([])
  const [followPageInfo, setFollowPageInfo] = useState({ current: 1, pages: 1, total: 0 })
  const [followPageNum, setFollowPageNum] = useState(1)
  const [message, setMessage] = useState('')
  const [loading, setLoading] = useState(false)
  const [followLoading, setFollowLoading] = useState(false)
  const [followActionLoading, setFollowActionLoading] = useState(false)

  useEffect(() => {
    async function loadProfile() {
      if (!userId) return

      setLoading(true)
      setMessage('')

      try {
        const [profileResult, articlesResult] = await Promise.all([
          getPublicUserProfile(userId),
          pagePublicUserArticles(userId, { pageNum, pageSize: 5 }),
        ])
        setProfile(profileResult.data)
        setArticles(articlesResult.data.records || [])
        setPageInfo({
          current: articlesResult.data.current,
          pages: articlesResult.data.pages,
          total: articlesResult.data.total,
        })
      } catch (error) {
        setProfile(null)
        setArticles([])
        setMessage(error.message)
      } finally {
        setLoading(false)
      }
    }

    loadProfile()
  }, [pageNum, userId])

  useEffect(() => {
    setPageNum(1)
    setFollowPageNum(1)
  }, [userId])

  useEffect(() => {
    setFollowPageNum(1)
  }, [followTab])

  useEffect(() => {
    async function loadFollowUsers() {
      if (!userId) return

      setFollowLoading(true)

      try {
        const loader = followTab === 'followers' ? pageUserFollowers : pageUserFollowing
        const result = await loader(userId, { pageNum: followPageNum, pageSize: 5 })
        setFollowUsers(result.data.records || [])
        setFollowPageInfo({
          current: result.data.current,
          pages: result.data.pages,
          total: result.data.total,
        })
      } catch (error) {
        setFollowUsers([])
        setMessage(error.message)
      } finally {
        setFollowLoading(false)
      }
    }

    loadFollowUsers()
  }, [followPageNum, followTab, userId])

  async function handleFollowToggle() {
    if (!currentUser) {
      setMessage('请先登录后再关注用户')
      return
    }

    setFollowActionLoading(true)
    setMessage('')

    try {
      const result = profile.followedByCurrentUser
        ? await unfollowUser(profile.userId)
        : await followUser(profile.userId)
      setProfile((current) => ({
        ...current,
        followingCount: result.data.followingCount,
        followerCount: result.data.followerCount,
        followedByCurrentUser: result.data.followedByCurrentUser,
      }))
      setFollowPageNum(1)
    } catch (error) {
      setMessage(error.message)
    } finally {
      setFollowActionLoading(false)
    }
  }

  const isSelf = currentUser?.userId && profile?.userId && Number(currentUser.userId) === Number(profile.userId)

  return (
    <div className="content-view">
      <PageHeader
        title="用户主页"
        description="查看用户公开资料、关注关系和已发布文章。"
        badge={
          <button className="ghost-button" type="button" onClick={onBack}>
            返回
          </button>
        }
      />

      {loading && <LoadingSpinner text="加载用户主页中..." />}
      <MessageBanner message={message} type="error" />

      {profile && (
        <section className="profile-panel public-profile-panel">
          <div className="profile-summary">
            <div className="profile-avatar">
              {profile.avatar ? <img alt="头像" src={profile.avatar} /> : <span>{profile.username?.slice(0, 1)}</span>}
            </div>
            <div>
              <h3>{profile.nickname || profile.username}</h3>
              <p>{profile.bio || '这个用户还没有填写个人简介。'}</p>
              <div className="profile-stats">
                <span>用户 #{profile.userId}</span>
                <span>{profile.status === 1 ? '账号可用' : '账号不可用'}</span>
                <span>{profile.publishedArticleCount ?? 0} 篇已发布</span>
                <span>{profile.favoriteCount ?? 0} 个收藏</span>
                <span>{profile.followingCount ?? 0} 个关注</span>
                <span>{profile.followerCount ?? 0} 个粉丝</span>
              </div>
            </div>
            {!isSelf && (
              <button
                className={profile.followedByCurrentUser ? 'ghost-button profile-follow-button' : 'primary-button profile-follow-button'}
                disabled={followActionLoading}
                type="button"
                onClick={handleFollowToggle}
              >
                {profile.followedByCurrentUser ? '取消关注' : '关注'}
              </button>
            )}
          </div>
        </section>
      )}

      {profile && (
        <section className="profile-panel follow-panel">
          <div className="sub-heading">
            <h3>关注关系</h3>
            <span>{followPageInfo.total} 人</span>
          </div>
          <div className="follow-tabs">
            <button
              className={followTab === 'followers' ? 'active' : ''}
              type="button"
              onClick={() => setFollowTab('followers')}
            >
              粉丝
            </button>
            <button
              className={followTab === 'following' ? 'active' : ''}
              type="button"
              onClick={() => setFollowTab('following')}
            >
              关注
            </button>
          </div>
          <div className="follow-user-list">
            {followUsers.map((user) => (
              <div className="follow-user-row" key={user.userId}>
                <div className="profile-avatar mini-avatar">
                  {user.avatar ? <img alt="头像" src={user.avatar} /> : <span>{user.username?.slice(0, 1)}</span>}
                </div>
                <div>
                  <strong>{user.nickname || user.username}</strong>
                  <p>{user.bio || '暂未填写个人简介'}</p>
                </div>
              </div>
            ))}
            {!followLoading && followUsers.length === 0 && (
              <EmptyState message={followTab === 'followers' ? '暂无粉丝。' : '暂无关注用户。'} />
            )}
          </div>
          {followPageInfo.total > 0 && (
            <Pagination pageInfo={followPageInfo} onPageChange={setFollowPageNum} loading={followLoading} />
          )}
        </section>
      )}

      <div className="sub-heading">
        <h3>公开文章</h3>
        <span>{pageInfo.total} 篇</span>
      </div>

      <div className="article-list">
        {articles.map((article) => (
          <ArticleCard key={article.id} article={article} onOpen={onOpenDetail} />
        ))}
        {!loading && articles.length === 0 && <EmptyState message="暂无公开文章。" />}
      </div>

      {pageInfo.total > 0 && (
        <Pagination pageInfo={pageInfo} onPageChange={setPageNum} loading={loading} />
      )}
    </div>
  )
}

export default PublicUserProfile

