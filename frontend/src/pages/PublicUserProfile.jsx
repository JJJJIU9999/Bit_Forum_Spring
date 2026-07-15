import { useEffect, useState } from 'react'
import { ChevronLeft, UserMinus, UserPlus } from 'lucide-react'
import { useNavigate, useOutletContext, useParams, useSearchParams } from 'react-router'
import { followUser, getPublicUserProfile, pagePublicUserArticles, pageUserFollowers, pageUserFollowing, unfollowUser } from '../api/userApi.js'
import PageHeader from '../components/PageHeader.jsx'
import ArticleCard from '../components/ArticleCard.jsx'
import Pagination from '../components/Pagination.jsx'
import EmptyState from '../components/EmptyState.jsx'
import LoadingSpinner from '../components/LoadingSpinner.jsx'
import MessageBanner from '../components/MessageBanner.jsx'

function PublicUserProfile() {
  const { userId } = useParams()
  const { currentUser } = useOutletContext()
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  const [profile, setProfile] = useState(null)
  const [articles, setArticles] = useState([])
  const [articlePage, setArticlePage] = useState({ current: 1, pages: 1, total: 0 })
  const [articlePageNum, setArticlePageNum] = useState(1)
  const [followUsers, setFollowUsers] = useState([])
  const [followPage, setFollowPage] = useState({ current: 1, pages: 1, total: 0 })
  const [followPageNum, setFollowPageNum] = useState(1)
  const [message, setMessage] = useState('')
  const [loading, setLoading] = useState(false)
  const [followLoading, setFollowLoading] = useState(false)
  const [followActionLoading, setFollowActionLoading] = useState(false)
  const tab = searchParams.get('tab') === 'following' ? 'following' : 'followers'

  useEffect(() => {
    async function loadProfile() {
      setLoading(true); setMessage('')
      try {
        const [profileResult, articlesResult] = await Promise.all([getPublicUserProfile(userId), pagePublicUserArticles(userId, { pageNum: articlePageNum, pageSize: 6 })])
        setProfile(profileResult.data); setArticles(articlesResult.data.records || []); setArticlePage({ current: articlesResult.data.current, pages: articlesResult.data.pages, total: articlesResult.data.total })
      } catch (error) { setMessage(error.message); setProfile(null); setArticles([]) } finally { setLoading(false) }
    }
    loadProfile()
  }, [userId, articlePageNum])

  useEffect(() => {
    async function loadFollowUsers() {
      setFollowLoading(true)
      try {
        const result = await (tab === 'followers' ? pageUserFollowers : pageUserFollowing)(userId, { pageNum: followPageNum, pageSize: 6 })
        setFollowUsers(result.data.records || []); setFollowPage({ current: result.data.current, pages: result.data.pages, total: result.data.total })
      } catch (error) { setMessage(error.message); setFollowUsers([]) } finally { setFollowLoading(false) }
    }
    loadFollowUsers()
  }, [userId, tab, followPageNum])

  async function handleFollowToggle() {
    if (!currentUser) { navigate(`/login?redirectTo=${encodeURIComponent(`/users/${userId}`)}`); return }
    setFollowActionLoading(true)
    try {
      const result = profile.followedByCurrentUser ? await unfollowUser(profile.userId) : await followUser(profile.userId)
      setProfile((current) => ({ ...current, followingCount: result.data.followingCount, followerCount: result.data.followerCount, followedByCurrentUser: result.data.followedByCurrentUser }))
    } catch (error) { setMessage(error.message) } finally { setFollowActionLoading(false) }
  }

  const isSelf = Number(currentUser?.userId) === Number(profile?.userId)
  return <div className="content-view public-profile-view"><PageHeader title="用户主页" description="查看公开资料、关注关系和已发布文章。" actions={<button className="ghost-button icon-text-button" type="button" onClick={() => navigate(-1)}><ChevronLeft size={17} aria-hidden="true" />返回</button>} />{loading && <LoadingSpinner text="正在打开用户主页…" />}<MessageBanner message={message} type="error" />{profile && <><section className="profile-panel public-profile-panel"><div className="profile-summary"><div className="profile-avatar">{profile.avatar ? <img alt={`${profile.nickname || profile.username} 的头像`} src={profile.avatar} /> : <span>{profile.username?.slice(0, 1)}</span>}</div><div><p className="eyebrow">用户 #{profile.userId}</p><h2>{profile.nickname || profile.username}</h2><p>{profile.bio || '这个用户还没有填写个人简介。'}</p><div className="profile-stats"><span>{profile.publishedArticleCount ?? 0} 篇文章</span><span>{profile.favoriteCount ?? 0} 个收藏</span><span>{profile.followingCount ?? 0} 个关注</span><span>{profile.followerCount ?? 0} 个粉丝</span></div></div>{!isSelf && <button className={profile.followedByCurrentUser ? 'ghost-button profile-follow-button' : 'primary-button profile-follow-button'} disabled={followActionLoading} type="button" onClick={handleFollowToggle}>{profile.followedByCurrentUser ? <><UserMinus size={17} aria-hidden="true" />取消关注</> : <><UserPlus size={17} aria-hidden="true" />关注</>}</button>}</div></section><section className="follow-panel profile-panel"><div className="sub-heading"><div><p className="eyebrow">关注关系</p><h2>{tab === 'followers' ? '粉丝' : '正在关注'}</h2></div><span>{followPage.total} 人</span></div><div className="follow-tabs"><button className={tab === 'followers' ? 'active' : ''} type="button" onClick={() => { setSearchParams({ tab: 'followers' }); setFollowPageNum(1) }}>粉丝</button><button className={tab === 'following' ? 'active' : ''} type="button" onClick={() => { setSearchParams({ tab: 'following' }); setFollowPageNum(1) }}>关注</button></div><div className="follow-user-list">{followUsers.map((user) => <button className="follow-user-row" key={user.userId} type="button" onClick={() => navigate(`/users/${user.userId}`)}><span className="mini-avatar">{user.avatar ? <img alt="" src={user.avatar} /> : user.username?.slice(0, 1)}</span><span><strong>{user.nickname || user.username}</strong><small>{user.bio || '暂未填写个人简介'}</small></span></button>)}{!followLoading && followUsers.length === 0 && <EmptyState message={tab === 'followers' ? '暂无粉丝。' : '暂无关注用户。'} />}</div>{followPage.total > 0 && <Pagination pageInfo={followPage} onPageChange={setFollowPageNum} loading={followLoading} />}</section><section className="public-article-section"><div className="sub-heading"><div><p className="eyebrow">公开内容</p><h2>已发布文章</h2></div><span>{articlePage.total} 篇</span></div><div className="article-list">{articles.map((article) => <ArticleCard key={article.id} article={article} onOpen={(id) => navigate(`/articles/${id}`)} />)}{!loading && articles.length === 0 && <EmptyState message="暂无公开文章。" />}</div>{articlePage.total > 0 && <Pagination pageInfo={articlePage} onPageChange={setArticlePageNum} loading={loading} />}</section></>}</div>
}

export default PublicUserProfile
