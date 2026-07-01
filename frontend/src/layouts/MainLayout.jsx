function MainLayout({ currentUser, activePage, onNavigate, onLogout, unreadCount, children }) {
  const isLoggedIn = !!currentUser

  return (
    <div className="site-layout">
      <header className="site-header">
        <div className="header-inner">
          <a className="header-brand" onClick={() => onNavigate('articles')}>
            Bit Forum
          </a>

          <nav className="header-nav">
            <button
              className={
                activePage === 'articles' || activePage === 'detail' || activePage === 'publicProfile'
                  ? 'nav-active'
                  : ''
              }
              type="button"
              onClick={() => onNavigate('articles')}
            >
              首页
            </button>
            {isLoggedIn && (
              <>
                <button
                  className={activePage === 'notifications' ? 'nav-active' : ''}
                  type="button"
                  onClick={() => onNavigate('notifications')}
                >
                  通知
                  {unreadCount > 0 && <span className="nav-badge">{unreadCount}</span>}
                </button>
                <button
                  className={activePage === 'publish' ? 'nav-active' : ''}
                  type="button"
                  onClick={() => onNavigate('publish')}
                >
                  投稿
                </button>
                <button
                  className={activePage === 'myArticles' ? 'nav-active' : ''}
                  type="button"
                  onClick={() => onNavigate('myArticles')}
                >
                  我的
                </button>
              </>
            )}
          </nav>

          <div className="header-actions">
            {isLoggedIn ? (
              <>
                <span className="header-username">{currentUser.username}</span>
                <button
                  className="ghost-button compact-button"
                  type="button"
                  onClick={() => onNavigate('admin')}
                >
                  后台
                </button>
                <button
                  className="ghost-button compact-button"
                  type="button"
                  onClick={onLogout}
                >
                  退出
                </button>
              </>
            ) : (
              <button
                className="primary-button compact-button"
                type="button"
                onClick={() => onNavigate('login')}
              >
                登录
              </button>
            )}
          </div>
        </div>
      </header>

      <main className="site-main">
        <div className="content-container">
          {children}
        </div>
      </main>

      <footer className="site-footer">
        <p>Bit Forum · 基于 Spring Boot 与 React 的社区交流平台</p>
      </footer>
    </div>
  )
}

export default MainLayout
