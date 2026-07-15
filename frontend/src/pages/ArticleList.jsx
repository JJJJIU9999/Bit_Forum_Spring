import { useEffect, useState } from 'react'
import { Search, TrendingUp, X } from 'lucide-react'
import { useNavigate, useSearchParams } from 'react-router'
import { getHotArticles, pageArticles, searchArticles } from '../api/articleApi.js'
import { listCategories } from '../api/categoryApi.js'
import PageHeader from '../components/PageHeader.jsx'
import ArticleCard from '../components/ArticleCard.jsx'
import Pagination from '../components/Pagination.jsx'
import EmptyState from '../components/EmptyState.jsx'
import LoadingSpinner from '../components/LoadingSpinner.jsx'
import MessageBanner from '../components/MessageBanner.jsx'

function ArticleList() {
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  const [articles, setArticles] = useState([])
  const [hotArticles, setHotArticles] = useState([])
  const [categories, setCategories] = useState([])
  const [keyword, setKeyword] = useState(searchParams.get('q') || '')
  const [pageInfo, setPageInfo] = useState({ current: 1, pages: 1, total: 0 })
  const [message, setMessage] = useState('')
  const [loading, setLoading] = useState(false)

  const selectedCategoryId = searchParams.get('categoryId') || ''
  const submittedKeyword = searchParams.get('q') || ''
  const pageNum = Math.max(1, Number(searchParams.get('page')) || 1)

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
    setKeyword(submittedKeyword)
  }, [submittedKeyword])

  useEffect(() => {
    async function loadArticles() {
      setLoading(true)
      setMessage('')
      try {
        const params = { pageNum, pageSize: 8 }
        if (selectedCategoryId) params.categoryId = selectedCategoryId
        if (submittedKeyword) params.keyword = submittedKeyword

        const [pageResult, hotResult] = await Promise.all([
          submittedKeyword ? searchArticles(params) : pageArticles(params),
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
  }, [pageNum, selectedCategoryId, submittedKeyword])

  function updateQuery(changes) {
    const next = new URLSearchParams(searchParams)
    Object.entries(changes).forEach(([key, value]) => {
      if (value) next.set(key, String(value))
      else next.delete(key)
    })
    setSearchParams(next)
  }

  function handleSearch(event) {
    event.preventDefault()
    updateQuery({ q: keyword.trim(), page: 1 })
  }

  function clearSearch() {
    setKeyword('')
    updateQuery({ q: '', categoryId: '', page: '' })
  }

  return (
    <div className="community-home">
      <section className="community-masthead">
        <p className="eyebrow">社区精选</p>
        <h1>让每一次讨论，都留下可回看的观点。</h1>
        <p>从技术实践到日常思考，在 Bit Forum 找到值得继续聊下去的内容。</p>
      </section>

      <div className="community-grid">
        <section className="article-feed" aria-label="文章列表">
          <PageHeader level={2} title="最新文章" description="按板块筛选，或直接搜索标题和正文。" badge={`${pageInfo.total} 篇`} />
          <form className="search-filter editorial-toolbar" onSubmit={handleSearch}>
            <label>
              <span>搜索内容</span>
              <div className="input-with-icon"><Search size={17} aria-hidden="true" /><input onChange={(event) => setKeyword(event.target.value)} placeholder="搜索标题或正文" value={keyword} /></div>
            </label>
            <label>
              <span>浏览板块</span>
              <select value={selectedCategoryId} onChange={(event) => updateQuery({ categoryId: event.target.value, page: 1 })}>
                <option value="">全部板块</option>
                {categories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}
              </select>
            </label>
            <button className="primary-button" type="submit">搜索</button>
            {(submittedKeyword || selectedCategoryId) && <button className="ghost-button icon-text-button" type="button" onClick={clearSearch}><X size={16} aria-hidden="true" />清空</button>}
          </form>

          {loading && <LoadingSpinner text="正在整理最新文章..." />}
          <MessageBanner message={message} type="error" />
          <div className="article-list">
            {articles.map((article) => <ArticleCard key={article.id} article={article} onOpen={(id) => navigate(`/articles/${id}`)} />)}
            {!loading && articles.length === 0 && <EmptyState message="还没有符合条件的文章，换个关键词试试。" />}
          </div>
          {pageInfo.total > 0 && <Pagination pageInfo={pageInfo} onPageChange={(page) => updateQuery({ page })} loading={loading} />}
        </section>

        <aside className="community-sidebar">
          <section className="hot-section">
            <div className="sub-heading"><div><p className="eyebrow">正在发生</p><h2><TrendingUp size={18} aria-hidden="true" />热门讨论</h2></div></div>
            <div className="hot-list">
              {hotArticles.length === 0 ? <p>暂时还没有热榜数据。</p> : hotArticles.map((item, index) => (
                <button className="hot-item" key={item.article.id} type="button" onClick={() => navigate(`/articles/${item.article.id}`)}>
                  <span>{String(index + 1).padStart(2, '0')}</span><strong>{item.article.title}</strong><small>热度 {item.hotScore}</small>
                </button>
              ))}
            </div>
          </section>
          <section className="category-rail">
            <p className="eyebrow">按兴趣阅读</p><h2>热门板块</h2>
            <div>{categories.map((category) => <button key={category.id} className={String(category.id) === selectedCategoryId ? 'active' : ''} type="button" onClick={() => updateQuery({ categoryId: category.id, page: 1 })}>{category.name}</button>)}</div>
          </section>
        </aside>
      </div>
    </div>
  )
}

export default ArticleList
