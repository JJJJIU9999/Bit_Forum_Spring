import { useEffect, useState } from 'react'
import MainLayout from './layouts/MainLayout.jsx'
import AdminLayout from './layouts/AdminLayout.jsx'
import Login from './pages/Login.jsx'
import Register from './pages/Register.jsx'
import ArticleList from './pages/ArticleList.jsx'
import ArticleDetail from './pages/ArticleDetail.jsx'
import PublishArticle from './pages/PublishArticle.jsx'
import MyArticles from './pages/MyArticles.jsx'
import PublicUserProfile from './pages/PublicUserProfile.jsx'
import NotificationCenter from './pages/NotificationCenter.jsx'
import Dashboard from './pages/admin/Dashboard.jsx'
import AuditArticles from './pages/admin/AuditArticles.jsx'
import ManageArticles from './pages/admin/ManageArticles.jsx'
import ManageComments from './pages/admin/ManageComments.jsx'
import ManageReports from './pages/admin/ManageReports.jsx'
import ManageUsers from './pages/admin/ManageUsers.jsx'
import ManageCategories from './pages/admin/ManageCategories.jsx'
import { clearAuth, getCurrentUser } from './api/request.js'
import { getUnreadNotificationCount } from './api/notificationApi.js'

function App() {
  const [currentUser, setCurrentUser] = useState(getCurrentUser())
  const [activePage, setActivePage] = useState(currentUser ? 'articles' : 'login')
  const [selectedArticleId, setSelectedArticleId] = useState(null)
  const [selectedUserId, setSelectedUserId] = useState(null)
  const [listRefreshKey, setListRefreshKey] = useState(0)
  const [myArticleRefreshKey, setMyArticleRefreshKey] = useState(0)
  const [notificationRefreshKey, setNotificationRefreshKey] = useState(0)
  const [unreadCount, setUnreadCount] = useState(0)
  const [adminTab, setAdminTab] = useState('dashboard')

  async function refreshUnreadCount() {
    if (!currentUser) {
      setUnreadCount(0)
      return
    }

    try {
      const result = await getUnreadNotificationCount()
      setUnreadCount(result.data || 0)
    } catch {
      setUnreadCount(0)
    }
  }

  useEffect(() => {
    refreshUnreadCount()
  }, [currentUser, notificationRefreshKey])

  function openDetail(articleId) {
    setSelectedArticleId(articleId)
    setActivePage('detail')
  }

  function openUserProfile(userId) {
    setSelectedUserId(userId)
    setActivePage('publicProfile')
  }

  function handleLogin(user) {
    setCurrentUser(user)
    setActivePage('articles')
  }

  function handleLogout() {
    clearAuth()
    setCurrentUser(null)
    setSelectedArticleId(null)
    setSelectedUserId(null)
    setUnreadCount(0)
    setActivePage('login')
  }

  function handlePublished() {
    setListRefreshKey((k) => k + 1)
    setMyArticleRefreshKey((k) => k + 1)
    setActivePage('myArticles')
  }

  function renderPage() {
    if (activePage === 'login') {
      return (
        <>
          <div className="tabs">
            <button className="active" type="button">登录</button>
            <button type="button" onClick={() => setActivePage('register')}>注册</button>
          </div>
          <Login onLogin={handleLogin} />
        </>
      )
    }

    if (activePage === 'register') {
      return (
        <>
          <div className="tabs">
            <button type="button" onClick={() => setActivePage('login')}>登录</button>
            <button className="active" type="button">注册</button>
          </div>
          <Register onRegistered={() => setActivePage('login')} />
        </>
      )
    }

    if (activePage === 'articles') {
      return <ArticleList refreshKey={listRefreshKey} onOpenDetail={openDetail} />
    }

    if (activePage === 'detail') {
      return (
        <ArticleDetail
          articleId={selectedArticleId}
          currentUser={currentUser}
          onBack={() => setActivePage('articles')}
          onOpenUserProfile={openUserProfile}
        />
      )
    }

    if (activePage === 'publicProfile') {
      return (
        <PublicUserProfile
          userId={selectedUserId}
          currentUser={currentUser}
          onBack={() => setActivePage(selectedArticleId ? 'detail' : 'articles')}
          onOpenDetail={openDetail}
        />
      )
    }

    if (activePage === 'publish') {
      return <PublishArticle currentUser={currentUser} onPublished={handlePublished} />
    }

    if (activePage === 'myArticles') {
      return <MyArticles currentUser={currentUser} refreshKey={myArticleRefreshKey} />
    }

    if (activePage === 'notifications') {
      return (
        <NotificationCenter
          currentUser={currentUser}
          refreshKey={notificationRefreshKey}
          onOpenArticle={openDetail}
          onUnreadChanged={() => setNotificationRefreshKey((k) => k + 1)}
        />
      )
    }

    return null
  }

  function renderAdminPage() {
    const props = { currentUser }

    switch (adminTab) {
      case 'dashboard': return <Dashboard {...props} />
      case 'audit': return <AuditArticles {...props} />
      case 'articles': return <ManageArticles {...props} />
      case 'comments': return <ManageComments {...props} />
      case 'reports': return <ManageReports {...props} />
      case 'users': return <ManageUsers {...props} />
      case 'categories': return <ManageCategories {...props} />
      default: return <Dashboard {...props} />
    }
  }

  if (activePage === 'admin') {
    return (
      <AdminLayout
        currentUser={currentUser}
        onLogout={handleLogout}
        onBackToSite={() => setActivePage('articles')}
        activeTab={adminTab}
        onTabChange={setAdminTab}
      >
        {renderAdminPage()}
      </AdminLayout>
    )
  }

  return (
    <MainLayout
      currentUser={currentUser}
      activePage={activePage}
      onNavigate={setActivePage}
      onLogout={handleLogout}
      unreadCount={unreadCount}
    >
      {renderPage()}
    </MainLayout>
  )
}

export default App
