function ConfirmDialog({ title, message, inputLabel, onConfirm, onCancel }) {
  function handleSubmit(event) {
    event.preventDefault()
    const input = event.target.elements['dialog-input']
    onConfirm(input ? input.value : undefined)
  }

  return (
    <div className="dialog-overlay" onClick={onCancel}>
      <div className="dialog-box" onClick={(e) => e.stopPropagation()}>
        <h3>{title}</h3>
        <p>{message}</p>
        {inputLabel ? (
          <form onSubmit={handleSubmit}>
            <label className="auth-form" style={{ gap: 10 }}>
              {inputLabel}
              <input
                autoFocus
                name="dialog-input"
                placeholder={inputLabel}
                style={{ width: '100%' }}
              />
            </label>
            <div className="dialog-actions" style={{ marginTop: 16 }}>
              <button className="ghost-button compact-button" type="button" onClick={onCancel}>
                取消
              </button>
              <button className="primary-button compact-button" type="submit">
                确认
              </button>
            </div>
          </form>
        ) : (
          <div className="dialog-actions">
            <button className="ghost-button compact-button" type="button" onClick={onCancel}>
              取消
            </button>
            <button className="primary-button compact-button" type="button" onClick={() => onConfirm()}>
              确认
            </button>
          </div>
        )}
      </div>
    </div>
  )
}

export default ConfirmDialog
