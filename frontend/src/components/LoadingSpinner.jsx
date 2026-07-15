function LoadingSpinner({ text = '加载中...' }) {
  return <p className="form-message loading-state" role="status" aria-live="polite">{text}</p>
}

export default LoadingSpinner
