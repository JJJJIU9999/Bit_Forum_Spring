import { useState } from 'react'
import { publishArticle } from '../api/articleApi.js'

function PublishArticle({ currentUser, onPublished }) {
  const [form, setForm] = useState({ title: '', content: '' })
  const [message, setMessage] = useState('')
  const [loading, setLoading] = useState(false)

  function updateField(event) {
    const { name, value } = event.target
    setForm((current) => ({ ...current, [name]: value }))
  }

  async function handleSubmit(event) {
    event.preventDefault()

    if (!currentUser) {
      setMessage('请先登录，再发布文章。')
      return
    }

    setLoading(true)
    setMessage('')

    try {
      // 这里只提交 title/content。后端会从 JWT 中读取真正的 userId。
      await publishArticle(form)
      setForm({ title: '', content: '' })
      setMessage('发布成功，正在返回文章列表。')
      onPublished()
    } catch (error) {
      setMessage(error.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <form className="auth-form" onSubmit={handleSubmit}>
      <div className="form-heading">
        <h2>发布文章</h2>
        <p>发布接口需要登录。作者 ID 不由前端传，后端从 JWT 中读取。</p>
      </div>

      <label>
        标题
        <input
          maxLength="50"
          name="title"
          onChange={updateField}
          placeholder="不超过 50 个字符"
          type="text"
          value={form.title}
        />
      </label>

      <label>
        内容
        <textarea
          name="content"
          onChange={updateField}
          placeholder="写一段用于演示的文章内容"
          value={form.content}
        />
      </label>

      <button className="primary-button" disabled={loading} type="submit">
        {loading ? '发布中...' : '发布文章'}
      </button>

      {message && <p className="form-message">{message}</p>}
    </form>
  )
}

export default PublishArticle
