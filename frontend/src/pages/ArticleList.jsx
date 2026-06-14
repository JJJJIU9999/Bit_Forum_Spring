import { useEffect, useState } from 'react'
import { getHotArticles, pageArticles } from '../api/articleApi.js'

function ArticleList({ onOpenDetail, refreshKey }) {
  const [articles, setArticles] = useState([])
  const [hotArticles, setHotArticles] = useState([])
  const [pageInfo, setPageInfo] = useState({ current: 1, pages: 1, total: 0 })
  const [pageNum, setPageNum] = useState(1)
  const [message, setMessage] = useState('')
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    async function loadArticles() {
      setLoading(true)
      setMessage('')

      try {
        // 列表数据来自 MySQL，热门数据来自 Redis ZSet；两个请求可以一起发出。
        const [pageResult, hotResult] = await Promise.all([
          pageArticles({ pageNum, pageSize: 5 }),
          getHotArticles(),
        ])

        setArticles(pageResult.data.records || [])
        setPageInfo({
          current: pageResult.data.current,
          pages: pageResult.data.pages,
          total: pageResult.data.total,
        })
        setHotArticles(hotResult.data || [])
      } catch (error) {
        setMessage(error.message)
      } finally {
        setLoading(false)
      }
    }

    loadArticles()
    // pageNum 变化时换页；refreshKey 变化时强制刷新列表。
  }, [pageNum, refreshKey])

  return (
    <div className="content-view">
      <div className="section-heading">
        <div>
          <h2>文章列表</h2>
          <p>分页接口来自 MySQL，点击详情时会再读取 Redis 浏览量和点赞数。</p>
        </div>
        <span>{pageInfo.total} 篇</span>
      </div>

      {loading && <p className="form-message">加载文章中...</p>}
      {message && <p className="form-message">{message}</p>}

      <section className="hot-section" aria-label="热门文章">
        <div className="sub-heading">
          <h3>热门文章</h3>
          <span>Redis ZSet</span>
        </div>

        <div className="hot-list">
          {hotArticles.length === 0 && <p>暂无热门文章数据。</p>}
          {hotArticles.map((item) => (
            <button
              className="hot-item"
              key={item.article.id}
              type="button"
              onClick={() => onOpenDetail(item.article.id)}
            >
              <span>热度 {item.hotScore}</span>
              <strong>{item.article.title}</strong>
            </button>
          ))}
        </div>
      </section>

      <div className="article-list">
        {articles.map((article) => (
          <article className="article-item" key={article.id}>
            <div>
              <span>#{article.id}</span>
              <h3>{article.title}</h3>
              <p>{article.content}</p>
            </div>
            <button
              className="ghost-button"
              type="button"
              onClick={() => onOpenDetail(article.id)}
            >
              查看详情
            </button>
          </article>
        ))}
      </div>

      <div className="pager">
        <button
          className="ghost-button"
          disabled={pageInfo.current <= 1}
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
          disabled={pageInfo.current >= pageInfo.pages}
          type="button"
          onClick={() => setPageNum((current) => current + 1)}
        >
          下一页
        </button>
      </div>
    </div>
  )
}

export default ArticleList
