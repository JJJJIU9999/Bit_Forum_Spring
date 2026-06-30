import { useEffect, useState } from 'react'
import { publishArticle, saveDraft } from '../api/articleApi.js'
import { listCategories } from '../api/categoryApi.js'
import PageHeader from '../components/PageHeader.jsx'
import MessageBanner from '../components/MessageBanner.jsx'

function PublishArticle({ currentUser, onPublished }) {
  const [form, setForm] = useState({ title: '', content: '', categoryId: '' })
  const [categories, setCategories] = useState([])
  const [message, setMessage] = useState('')
  const [messageType, setMessageType] = useState('info')
  const [loading, setLoading] = useState(false)
  const [draftLoading, setDraftLoading] = useState(false)

  useEffect(() => {
    async function loadCategories() {
      try {
        const result = await listCategories()
        const list = result.data || []
        setCategories(list)
        if (list.length > 0) {
          setForm((current) => ({ ...current, categoryId: String(list[0].id) }))
        }
      } catch (error) {
        setMessage(error.message)
        setMessageType('error')
      }
    }

    loadCategories()
  }, [])

  function updateField(event) {
    const { name, value } = event.target
    setForm((current) => ({ ...current, [name]: value }))
  }

  async function handleSubmit(event) {
    event.preventDefault()

    if (!currentUser) {
      setMessage('请先登录，再提交审核。')
      setMessageType('error')
      return
    }

    if (!form.categoryId) {
      setMessage('请选择文章板块。')
      setMessageType('error')
      return
    }

    setLoading(true)
    setMessage('')

    try {
      await publishArticle({
        title: form.title,
        content: form.content,
        categoryId: Number(form.categoryId),
      })
      setForm((current) => ({ title: '', content: '', categoryId: current.categoryId }))
      setMessage('提交审核成功，可在"我的文章"查看状态。')
      setMessageType('info')
      onPublished()
    } catch (error) {
      setMessage(error.message)
      setMessageType('error')
    } finally {
      setLoading(false)
    }
  }

  async function handleSaveDraft() {
    if (!currentUser) {
      setMessage('请先登录，再保存草稿。')
      setMessageType('error')
      return
    }

    if (!form.categoryId) {
      setMessage('请选择文章板块。')
      setMessageType('error')
      return
    }

    setDraftLoading(true)
    setMessage('')

    try {
      await saveDraft({
        title: form.title,
        content: form.content,
        categoryId: Number(form.categoryId),
      })
      setForm((current) => ({ title: '', content: '', categoryId: current.categoryId }))
      setMessage('草稿保存成功。')
      setMessageType('info')
      onPublished()
    } catch (error) {
      setMessage(error.message)
      setMessageType('error')
    } finally {
      setDraftLoading(false)
    }
  }

  return (
    <form className="auth-form" onSubmit={handleSubmit}>
      <PageHeader
        title="发布文章"
        description="选择板块、填写标题和内容，提交后等待管理员审核。"
      />

      <label>
        板块
        <select name="categoryId" onChange={updateField} value={form.categoryId}>
          {categories.map((category) => (
            <option key={category.id} value={category.id}>
              {category.name}
            </option>
          ))}
        </select>
      </label>

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

      <div className="form-action-row">
        <button className="primary-button" disabled={loading || draftLoading} type="submit">
          {loading ? '提交中...' : '提交审核'}
        </button>
        <button
          className="ghost-button"
          disabled={loading || draftLoading}
          type="button"
          onClick={handleSaveDraft}
        >
          {draftLoading ? '保存中...' : '保存草稿'}
        </button>
      </div>

      <MessageBanner message={message} type={messageType} />
    </form>
  )
}

export default PublishArticle
