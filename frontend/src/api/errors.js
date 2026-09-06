export class ApiError extends Error {
  constructor(message, { status = 0, code = null, data = null } = {}) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
    this.data = data
  }
}
