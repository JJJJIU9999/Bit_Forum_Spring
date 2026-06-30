const NAV_ITEMS = [
  { key: 'dashboard', label: '数据看板' },
  { key: 'audit', label: '文章审核' },
  { key: 'articles', label: '文章管理' },
  { key: 'comments', label: '评论管理' },
  { key: 'reports', label: '举报处理' },
  { key: 'users', label: '用户管理' },
  { key: 'categories', label: '板块管理' },
]

function AdminLayout({ currentUser, onLogout, onBackToSite, activeTab, onTabChange, children }) {
  return (
    <div className="admin-layout">
      <aside className="admin-sidebar">
        <div className="admin-sidebar-brand">
          <h2>Bit Forum</h2>
          <span>管理后台</span>
        </div>
        <nav className="admin-sidebar-nav">
          {NAV_ITEMS.map((item) => (
            <button
              key={item.key}
              className={activeTab === item.key ? 'active' : ''}
              type="button"
              onClick={() => onTabChange(item.key)}
            >
              {item.label}
            </button>
          ))}
        </nav>
      </aside>
      <div className="admin-main">
        <div className="admin-topbar">
          <span>{currentUser?.username}</span>
          <button className="ghost-button compact-button" type="button" onClick={onBackToSite}>
            返回前台
          </button>
          <button className="ghost-button compact-button" type="button" onClick={onLogout}>
            退出
          </button>
        </div>
        {children}
      </div>
    </div>
  )
}

export default AdminLayout
