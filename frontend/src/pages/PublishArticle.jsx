import { useEffect, useState } from 'react'
import { publishArticle, saveDraft } from '../api/articleApi.js'
import { listCategories } from '../api/categoryApi.js'
import { uploadArticleCover } from '../api/uploadApi.js'
import PageHeader from '../components/PageHeader.jsx'
import MessageBanner from '../components/MessageBanner.jsx'

function PublishArticle({ currentUser, onPublished }) {
  const [form, setForm] = useState({ title: '', content: '', categoryId: '', coverUrl: '' })
  const [categories, setCategories] = useState([])
  const [coverFile, setCoverFile] = useState(null)
  const [coverInputKey, setCoverInputKey] = useState(0)
  const [message, setMessage] = useState('')
  const [messageType, setMessageType] = useState('info')
  const [loading, setLoading] = useState(false)
  const [draftLoading, setDraftLoading] = useState(false)
  const [coverUploading, setCoverUploading] = useState(false)

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

  function payloadFromForm() {
    return {
      title: form.title,
      content: form.content,
      categoryId: Number(form.categoryId),
      coverUrl: form.coverUrl.trim() || null,
    }
  }

  function resetForm() {
    setForm((current) => ({ title: '', content: '', categoryId: current.categoryId, coverUrl: '' }))
    setCoverFile(null)
    setCoverInputKey((key) => key + 1)
  }

  async function handleCoverUpload() {
    if (!currentUser) {
      setMessage('请先登录，再上传封面。')
      setMessageType('error')
      return
    }
    if (!coverFile) {
      setMessage('请选择要上传的封面图片。')
      setMessageType('error')
      return
    }

    setCoverUploading(true)
    setMessage('')

    try {
      const result = await uploadArticleCover(coverFile)
      setForm((current) => ({ ...current, coverUrl: result.data.url }))
      setMessage('封面上传成功。')
      setMessageType('info')
    } catch (error) {
      setMessage(error.message)
      setMessageType('error')
    } finally {
      setCoverUploading(false)
    }
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
      await publishArticle(payloadFromForm())
      resetForm()
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
      await saveDraft(payloadFromForm())
      resetForm()
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

      <div className="upload-field">
        <label>
          封面 URL
          <input
            name="coverUrl"
            onChange={updateField}
            placeholder="/uploads/article-cover/example.jpg"
            type="text"
            value={form.coverUrl}
          />
        </label>
        <div className="file-upload-row">
          <input
            key={coverInputKey}
            accept="image/jpeg,image/png,image/webp"
            type="file"
            onChange={(event) => setCoverFile(event.target.files?.[0] || null)}
          />
          <button
            className="ghost-button"
            disabled={loading || draftLoading || coverUploading}
            type="button"
            onClick={handleCoverUpload}
          >
            {coverUploading ? '上传中...' : '上传封面'}
          </button>
        </div>
        {form.coverUrl && (
          <img className="cover-preview" alt="文章封面预览" src={form.coverUrl} />
        )}
      </div>

      <div className="form-action-row">
        <button className="primary-button" disabled={loading || draftLoading || coverUploading} type="submit">
          {loading ? '提交中...' : '提交审核'}
        </button>
        <button
          className="ghost-button"
          disabled={loading || draftLoading || coverUploading}
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
