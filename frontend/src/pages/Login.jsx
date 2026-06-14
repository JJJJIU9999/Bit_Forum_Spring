import { useState } from 'react'
import { loginUser } from '../api/userApi.js'
import { getCurrentUser, saveAuth } from '../api/request.js'

function Login({ onLogin }) {
  // form 用来保存两个输入框的值；输入框改变时同步更新。
  const [form, setForm] = useState({ username: '', password: '' })
  const [message, setMessage] = useState('')
  const [loading, setLoading] = useState(false)

  function updateField(event) {
    const { name, value } = event.target
    setForm((current) => ({ ...current, [name]: value }))
  }

  async function handleSubmit(event) {
    // 阻止浏览器表单默认刷新页面，改为由 React 接管提交逻辑。
    event.preventDefault()
    setMessage('')
    setLoading(true)

    try {
      const result = await loginUser(form)
      saveAuth(result.data)
      onLogin(getCurrentUser())
      setMessage('登录成功，token 已保存。')
    } catch (error) {
      setMessage(error.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <form className="auth-form" onSubmit={handleSubmit}>
      <div className="form-heading">
        <h2>用户登录</h2>
        <p>登录成功后，后续发布文章、点赞和评论请求会自动带上 JWT。</p>
      </div>

      <label>
        用户名
        <input
          name="username"
          onChange={updateField}
          placeholder="例如 writer_liu"
          type="text"
          value={form.username}
        />
      </label>

      <label>
        密码
        <input
          name="password"
          onChange={updateField}
          placeholder="至少 6 位"
          type="password"
          value={form.password}
        />
      </label>

      <button className="primary-button" disabled={loading} type="submit">
        {loading ? '登录中...' : '登录'}
      </button>

      {message && <p className="form-message">{message}</p>}
    </form>
  )
}

export default Login
