import { useState } from 'react'
import { registerUser } from '../api/userApi.js'

function Register({ onRegistered }) {
  const [form, setForm] = useState({ username: '', password: '' })
  const [message, setMessage] = useState('')
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
      await registerUser(form)
      setMessage('注册成功，请使用新账号登录。')
      setForm({ username: '', password: '' })
      // 注册成功后回到登录页，让用户明确走一次登录拿 token 的流程。
      onRegistered()
    } catch (error) {
      setMessage(error.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <form className="auth-form" onSubmit={handleSubmit}>
      <div className="form-heading">
        <h2>用户注册</h2>
        <p>注册接口只返回安全的用户信息，不会把密码哈希返回给前端。</p>
      </div>

      <label>
        用户名
        <input
          name="username"
          onChange={updateField}
          placeholder="3 到 30 个字符"
          type="text"
          value={form.username}
        />
      </label>

      <label>
        密码
        <input
          name="password"
          onChange={updateField}
          placeholder="6 到 30 个字符"
          type="password"
          value={form.password}
        />
      </label>

      <button className="primary-button" disabled={loading} type="submit">
        {loading ? '注册中...' : '注册'}
      </button>

      {message && <p className="form-message">{message}</p>}
    </form>
  )
}

export default Register
