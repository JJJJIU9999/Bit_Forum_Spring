import StatusBadge from './StatusBadge.jsx'

function ArticleCard({ article, onOpen, statusLabel, children }) {
  return (
    <article className={children ? 'article-item article-item-with-footer' : 'article-item'}>
      <div className="article-main">
        {article.coverUrl && (
          <img className="article-cover-thumb" alt={`${article.title} 封面`} src={article.coverUrl} />
        )}
        <span>
          #{article.id}
          {article.categoryName ? ` · ${article.categoryName}` : ''}
        </span>
        <h3>{article.title}</h3>
        <p>{article.content}</p>
      </div>
      <div className="article-actions">
        {article.status && (
          <StatusBadge status={article.status} label={statusLabel} />
        )}
        {children}
        {onOpen && (
          <button
            className="ghost-button"
            type="button"
            onClick={() => onOpen(article.id)}
          >
            查看详情
          </button>
        )}
      </div>
      {children && <div className="article-card-footer">{children}</div>}
    </article>
  )
}

export default ArticleCard
