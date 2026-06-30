import { useEffect, useState } from 'react'
import { getHotArticles, pageArticles, searchArticles } from '../api/articleApi.js'
import { listCategories } from '../api/categoryApi.js'

function ArticleList({ onOpenDetail, refreshKey }) {
  const [articles, setArticles] = useState([])
  const [hotArticles, setHotArticles] = useState([])
  const [categories, setCategories] = useState([])
  const [selectedCategoryId, setSelectedCategoryId] = useState('')
  const [keyword, setKeyword] = useState('')
  const [submittedKeyword, setSubmittedKeyword] = useState('')
  const [pageInfo, setPageInfo] = useState({ current: 1, pages: 1, total: 0 })
  const [pageNum, setPageNum] = useState(1)
  const [message, setMessage] = useState('')
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    async function loadCategories() {
      try {
        const result = await listCategories()
        setCategories(result.data || [])
      } catch (error) {
        setMessage(error.message)
      }
    }

    loadCategories()
  }, [])

  useEffect(() => {
    async function loadArticles() {
      setLoading(true)
      setMessage('')

      try {
        const params = { pageNum, pageSize: 5 }
        if (selectedCategoryId) {
          params.categoryId = selectedCategoryId
        }
        if (submittedKeyword.trim()) {
          params.keyword = submittedKeyword.trim()
        }

        const [pageResult, hotResult] = await Promise.all([
          submittedKeyword.trim() ? searchArticles(params) : pageArticles(params),
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
  }, [pageNum, refreshKey, selectedCategoryId, submittedKeyword])

  function handleCategoryChange(event) {
    setSelectedCategoryId(event.target.value)
    setPageNum(1)
  }

  function handleSearch(event) {
    event.preventDefault()
    setSubmittedKeyword(keyword)
    setPageNum(1)
  }

  function clearSearch() {
    setKeyword('')
    setSubmittedKeyword('')
    setPageNum(1)
  }

  return (
    <div className="content-view">
      <div className="section-heading">
        <div>
          <h2>文章列表</h2>
          <p>公开列表只展示已发布文章，详情页会继续读取 Redis 浏览量和点赞数。</p>
        </div>
        <span>{pageInfo.total} 篇</span>
      </div>

      <form className="search-filter" onSubmit={handleSearch}>
        <label>
          关键词
          <input
            onChange={(event) => setKeyword(event.target.value)}
            placeholder="搜索标题或正文"
            value={keyword}
          />
        </label>
        <label>
          板块
          <select value={selectedCategoryId} onChange={handleCategoryChange}>
            <option value="">全部板块</option>
            {categories.map((category) => (
              <option key={category.id} value={category.id}>
                {category.name}
              </option>
            ))}
          </select>
        </label>
        <button className="primary-button" type="submit">
          搜索
        </button>
        <button className="ghost-button" type="button" onClick={clearSearch}>
          清空
        </button>
      </form>

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
              <span>
                #{article.id} {article.categoryName ? `· ${article.categoryName}` : ''}
              </span>
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
