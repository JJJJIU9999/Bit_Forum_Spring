import request from './request'

// 注册：body 里只传 username/password，role/status 由后端默认处理。
export function registerUser(payload) {
  return request.post('/user/register', payload)
}

// 登录：成功后后端会在 data.token 中返回 JWT。
export function loginUser(payload) {
  return request.post('/user/login', payload)
}
