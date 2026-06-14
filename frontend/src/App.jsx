import { useState } from 'react'
import Login from './pages/Login.jsx'
import Register from './pages/Register.jsx'
import ArticleList from './pages/ArticleList.jsx'
import ArticleDetail from './pages/ArticleDetail.jsx'
import PublishArticle from './pages/PublishArticle.jsx'
import AdminPanel from './pages/AdminPanel.jsx'
import { clearAuth, getCurrentUser } from './api/request.js'

const learningSteps = [
  '搭建 React + Vite 项目结构',
  '理解前端目录结构',
  '封装 Axios 请求',
  '完成登录和注册',
  '完成文章列表、详情和发布',
  '完成点赞、评论和热门文章',
  '完成管理员页面',
]

function App() {
  // currentUser 从 localStorage 恢复，所以刷新页面后仍能显示登录用户。
  const [currentUser, setCurrentUser] = useState(getCurrentUser())

  // 这里暂时不用 React Router，用一个字符串控制当前显示哪个页面。
  const [activePage, setActivePage] = useState(currentUser ? 'articles' : 'login')
  const [selectedArticleId, setSelectedArticleId] = useState(null)

  // 发布文章后递增这个值，ArticleList 的 useEffect 会重新拉取列表。
  const [listRefreshKey, setListRefreshKey] = useState(0)

  function openDetail(articleId) {
    setSelectedArticleId(articleId)
    setActivePage('detail')
  }

  function handleLogin(user) {
    setCurrentUser(user)
    setActivePage('articles')
  }

  function handleLogout() {
    clearAuth()
    setCurrentUser(null)
    setSelectedArticleId(null)
    setActivePage('login')
  }

  function handlePublished() {
    setListRefreshKey((current) => current + 1)
    setActivePage('articles')
  }

  return (
    <main className={`app-shell ${activePage === 'admin' ? 'admin-mode' : ''}`}>
      <section className="hero">
        <p className="eyebrow">Bit Forum Frontend</p>
        <h1>论坛前端学习演示页</h1>
        <p className="summary">
          当前进入第七步：管理员页面。后台接口统一走 `/api/admin/**`，
          前端负责发起请求，真正权限仍然由后端拦截器判断。
        </p>

        <div className="auth-status">
          {currentUser ? (
            <>
              <div>
                <span>当前用户</span>
                <strong>{currentUser.username}</strong>
              </div>
              <button className="ghost-button" type="button" onClick={handleLogout}>
                退出登录
              </button>
            </>
          ) : (
            <span>当前未登录</span>
          )}
        </div>
      </section>

      <section className="work-panel" aria-label="前端功能区">
        <div className="top-nav">
          <button
            className={activePage === 'login' || activePage === 'register' ? 'active' : ''}
            type="button"
            onClick={() => setActivePage(currentUser ? 'articles' : 'login')}
          >
            账号
          </button>
          <button
            className={activePage === 'articles' || activePage === 'detail' ? 'active' : ''}
            type="button"
            onClick={() => setActivePage('articles')}
          >
            文章
          </button>
          <button
            className={activePage === 'publish' ? 'active' : ''}
            type="button"
            onClick={() => setActivePage('publish')}
          >
            发布
          </button>
          <button
            className={activePage === 'admin' ? 'active' : ''}
            type="button"
            onClick={() => setActivePage('admin')}
          >
            管理
          </button>
        </div>

        {/* 登录页和注册页共用顶部 tabs，只是当前激活项不同。 */}
        {activePage === 'login' && (
          <>
            <div className="tabs">
              <button className="active" type="button">
                登录
              </button>
              <button type="button" onClick={() => setActivePage('register')}>
                注册
              </button>
            </div>
            <Login onLogin={handleLogin} />
          </>
        )}

        {activePage === 'register' && (
          <>
            <div className="tabs">
              <button type="button" onClick={() => setActivePage('login')}>
                登录
              </button>
              <button className="active" type="button">
                注册
              </button>
            </div>
            <Register onRegistered={() => setActivePage('login')} />
          </>
        )}

        {activePage === 'articles' && (
          <ArticleList refreshKey={listRefreshKey} onOpenDetail={openDetail} />
        )}

        {activePage === 'detail' && (
          <ArticleDetail
            articleId={selectedArticleId}
            currentUser={currentUser}
            onBack={() => setActivePage('articles')}
          />
        )}

        {activePage === 'publish' && (
          <PublishArticle currentUser={currentUser} onPublished={handlePublished} />
        )}

        {activePage === 'admin' && <AdminPanel currentUser={currentUser} />}
      </section>

      <section className="learning-panel" aria-label="前端学习路线">
        <div className="panel-header">
          <h2>学习路线</h2>
          <span>Step 7 / 7</span>
        </div>

        <ol className="step-list">
          {learningSteps.map((step, index) => (
            <li className={index === 6 ? 'active' : ''} key={step}>
              <span>{index + 1}</span>
              <p>{step}</p>
            </li>
          ))}
        </ol>
      </section>
    </main>
  )
}

export default App
