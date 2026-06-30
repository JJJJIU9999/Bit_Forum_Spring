function MessageBanner({ message, type = 'info' }) {
  if (!message) return null

  const cls = type === 'error' ? 'form-message message-error' : 'form-message'

  return <p className={cls}>{message}</p>
}

export default MessageBanner
