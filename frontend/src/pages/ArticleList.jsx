import { useEffect, useState } from 'react'
import { getHotArticles, pageArticles, searchArticles } from '../api/articleApi.js'
import { listCategories } from '../api/categoryApi.js'
import PageHeader from '../components/PageHeader.jsx'
import ArticleCard from '../components/ArticleCard.jsx'
import Pagination from '../components/Pagination.jsx'
import EmptyState from '../components/EmptyState.jsx'
import LoadingSpinner from '../components/LoadingSpinner.jsx'
import MessageBanner from '../components/MessageBanner.jsx'

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
  const [hotCollapsed, setHotCollapsed] = useState(false)

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
        const params = { pageNum, pageSize: 6 }
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
      <PageHeader
        title="文章列表"
        description="浏览社区最新文章，按板块筛选或搜索感兴趣的内容。"
        badge={`${pageInfo.total} 篇`}
      />

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

      {loading && <LoadingSpinner text="加载文章中..." />}
      <MessageBanner message={message} type="error" />

      {hotArticles.length > 0 && (
        <section className="hot-section">
          <div className="sub-heading">
            <h3>热门文章</h3>
            <button
              className="ghost-button compact-button"
              type="button"
              onClick={() => setHotCollapsed((v) => !v)}
            >
              {hotCollapsed ? '展开' : '收起'}
            </button>
          </div>
          {!hotCollapsed && <div className="hot-list">
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
          }
        </section>
      )}

      <div className="article-list">
        {articles.map((article) => (
          <ArticleCard key={article.id} article={article} onOpen={onOpenDetail} />
        ))}
        {!loading && articles.length === 0 && <EmptyState message="暂无文章。" />}
      </div>

      {pageInfo.total > 0 && (
        <Pagination pageInfo={pageInfo} onPageChange={setPageNum} loading={loading} />
      )}
    </div>
  )
}

export default ArticleList
