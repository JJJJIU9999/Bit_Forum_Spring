import { useState } from 'react'
import { registerUser } from '../api/userApi.js'
import MessageBanner from '../components/MessageBanner.jsx'

function Register({ onRegistered }) {
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
      await registerUser(form)
      setMessage('注册成功，请登录。')
      setMessageType('info')
      setForm({ username: '', password: '' })
      onRegistered()
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
        <h2>用户注册</h2>
        <p>注册后即可加入社区，参与文章发布与互动。</p>
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

      <MessageBanner message={message} type={messageType} />
    </form>
  )
}

export default Register
