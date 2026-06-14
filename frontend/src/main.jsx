import React from 'react'
import { createRoot } from 'react-dom/client'
import App from './App.jsx'
import './style.css'

// React 从这里接管 index.html 里的 #root 节点。
// StrictMode 会在开发环境做额外检查，能帮助发现 useEffect 副作用问题。
createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
)
