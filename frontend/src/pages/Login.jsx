import { useState } from 'react'
import { loginUser } from '../api/userApi.js'
import { getCurrentUser, saveAuth } from '../api/request.js'
import MessageBanner from '../components/MessageBanner.jsx'

function Login({ onLogin }) {
  const [form, setForm] = useState({ username: '', password: '' })
  const [message, setMessage] = useState('')
  const [messageType, setMessageType] = useState('info')
  const [loading, setLoading] = useState(false)

  function updateField(event) {
    const { name, value } = event.target
    setForm((current) => ({ ...current, [name]: value }))
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setMessage('')
    setLoading(true)

    try {
      const result = await loginUser(form)
      saveAuth(result.data)
      onLogin(getCurrentUser())
      setMessage('登录成功，欢迎回来。')
      setMessageType('info')
    } catch (error) {
      setMessage(error.message)
      setMessageType('error')
    } finally {
      setLoading(false)
    }
  }

  return (
    <form className="auth-form" onSubmit={handleSubmit}>
      <div className="form-heading">
        <h2>用户登录</h2>
        <p>登录后即可发布文章、点赞、收藏和评论。</p>
      </div>

      <label>
        用户名
        <input
          name="username"
          onChange={updateField}
          placeholder="请输入用户名"
          type="text"
          value={form.username}
        />
      </label>

      <label>
        密码
        <input
          name="password"
          onChange={updateField}
          placeholder="请输入密码"
          type="password"
          value={form.password}
        />
      </label>

      <button className="primary-button" disabled={loading} type="submit">
        {loading ? '登录中...' : '登录'}
      </button>

      <MessageBanner message={message} type={messageType} />
    </form>
  )
}

export default Login
