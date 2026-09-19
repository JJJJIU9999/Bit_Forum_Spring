import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router'
import { Bot, MessageSquarePlus, Send, Sparkles, X } from 'lucide-react'
import {
  createConversation,
  listConversations,
  listMessages,
  sendMessage,
} from '../api/aiApi.js'
import MarkdownText from './MarkdownText.jsx'

// AI 助手浮动面板（M13）。
// 仅在用户登录后渲染；对话历史由后端持久化，刷新页面后仍然保留。
function AiAssistantPanel() {
  const [open, setOpen] = useState(false)
  const [conversations, setConversations] = useState([])
  const [activeId, setActiveId] = useState(null)
  const [messages, setMessages] = useState([])
  const [input, setInput] = useState('')
  const [sending, setSending] = useState(false)
  const [error, setError] = useState('')
  const bottomRef = useRef(null)

  // 每次打开时重新拉取会话列表，保证与他人/其他标签页操作后状态一致
  useEffect(() => {
    if (open) {
      refreshConversations()
    }
  }, [open])

  // 新消息到达后滚动到底部
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
  }, [messages, sending])

  async function refreshConversations() {
    try {
      const result = await listConversations()
      setConversations(result.data || [])
    } catch (e) {
      setError(e.message)
    }
  }

  async function openConversation(id) {
    setActiveId(id)
    setError('')
    try {
      const result = await listMessages(id)
      setMessages(result.data || [])
    } catch (e) {
      setError(e.message)
      setMessages([])
    }
  }

  async function handleNewConversation() {
    setError('')
    try {
      const result = await createConversation()
      const created = result.data
      setConversations((prev) => [created, ...prev])
      setActiveId(created.id)
      setMessages([])
    } catch (e) {
      setError(e.message)
    }
  }

  async function handleSend(event) {
    event.preventDefault()
    const content = input.trim()
    if (!content || sending) return

    let conversationId = activeId
    setSending(true)
    setError('')

    try {
      // 没有选中会话时自动新建一个，避免用户先点"新建对话"
      if (!conversationId) {
        const created = await createConversation()
        conversationId = created.data.id
        setActiveId(conversationId)
        setConversations((prev) => [created.data, ...prev])
      }

      // 先把用户消息显示出来，避免等待期间界面没有反馈
      setMessages((prev) => [...prev, { id: `local-${Date.now()}`, role: 'user', content }])
      setInput('')

      const result = await sendMessage(conversationId, content)
      setMessages((prev) => [...prev, result.data])
      // 会话标题可能因首轮提问而变化，重新拉取列表同步
      refreshConversations()
    } catch (e) {
      setError(e.message)
    } finally {
      setSending(false)
    }
  }

  if (!open) {
    return (
      <button
        className="ai-fab"
        type="button"
        onClick={() => setOpen(true)}
        aria-label="打开 AI 助手"
        title="AI 助手"
      >
        <Sparkles size={20} aria-hidden="true" />
        <span>AI 助手</span>
      </button>
    )
  }

  return (
    <section className="ai-panel" aria-label="AI 助手">
      <header className="ai-panel-header">
        <strong>
          <Bot size={16} aria-hidden="true" />
          AI 助手
        </strong>
        <div className="ai-panel-header-actions">
          <button type="button" onClick={handleNewConversation} title="新建对话" aria-label="新建对话">
            <MessageSquarePlus size={16} aria-hidden="true" />
          </button>
          <button type="button" onClick={() => setOpen(false)} title="收起" aria-label="收起 AI 助手">
            <X size={16} aria-hidden="true" />
          </button>
        </div>
      </header>

      {conversations.length > 0 && (
        <div className="ai-conversation-list">
          {conversations.map((conversation) => (
            <button
              key={conversation.id}
              type="button"
              className={conversation.id === activeId ? 'is-active' : ''}
              onClick={() => openConversation(conversation.id)}
              title={conversation.title}
            >
              {conversation.title}
            </button>
          ))}
        </div>
      )}

      <div className="ai-panel-body">
        {messages.length === 0 && !sending && (
          <div className="ai-empty">
            <p>你好，我是 BitForum 的 AI 助手。</p>
            <p className="ai-empty-hint">
              可以多轮对话、查询与操作站内数据，并基于站内文章作答（回答下方会列出参考来源）。
            </p>
          </div>
        )}

        {messages.map((message) => (
          <div key={message.id} className={`ai-message ai-message-${message.role}`}>
            {/* 助手回答是 Markdown，需要渲染；用户输入按纯文本显示，避免被当作语法解析 */}
            {message.role === 'assistant' ? (
              <>
                <MarkdownText content={message.content} />
                {/* M15：检索命中的站内文章，点击跳到原帖 */}
                {message.citations?.length > 0 && (
                  <div className="ai-citations">
                    <span className="ai-citations-label">参考来源</span>
                    <ul>
                      {message.citations.map((citation) => (
                        <li key={citation.articleId}>
                          <Link to={`/articles/${citation.articleId}`} className="ai-citation-link">
                            {citation.title}
                          </Link>
                        </li>
                      ))}
                    </ul>
                  </div>
                )}
              </>
            ) : (
              message.content
            )}
          </div>
        ))}

        {sending && <div className="ai-message ai-message-assistant ai-typing">正在生成回答…</div>}

        {error && <p className="ai-error">{error}</p>}
        <div ref={bottomRef} />
      </div>

      <form className="ai-panel-footer" onSubmit={handleSend}>
        <input
          type="text"
          value={input}
          maxLength={2000}
          onChange={(event) => setInput(event.target.value)}
          placeholder="输入你的问题…"
          aria-label="消息内容"
          disabled={sending}
        />
        <button type="submit" disabled={sending || !input.trim()} aria-label="发送">
          <Send size={16} aria-hidden="true" />
        </button>
      </form>
    </section>
  )
}

export default AiAssistantPanel
