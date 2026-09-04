import { beforeEach, describe, expect, it, vi } from 'vitest'

const mocks = vi.hoisted(() => {
  const state = {
    requestFulfilled: null,
    responseFulfilled: null,
    responseRejected: null,
    errorSpy: vi.fn(),
  }

  const createSpy = vi.fn(() => ({
    interceptors: {
      request: {
        use: vi.fn((fulfilled) => {
          state.requestFulfilled = fulfilled
        }),
      },
      response: {
        use: vi.fn((fulfilled, rejected) => {
          state.responseFulfilled = fulfilled
          state.responseRejected = rejected
        }),
      },
    },
  }))

  return { state, createSpy }
})

vi.mock('axios', () => ({
  default: {
    create: mocks.createSpy,
  },
}))

vi.mock('element-plus', () => ({
  ElMessage: {
    error: mocks.state.errorSpy,
  },
}))

import http from '../../api/http'

beforeEach(() => {
  localStorage.clear()
  mocks.state.errorSpy.mockClear()
})

describe('http', () => {
  it('creates axios instance with expected base config', () => {
    expect(mocks.createSpy).toHaveBeenCalledWith({
      baseURL: '/api/v1',
      timeout: 10000,
    })
  })

  it('adds bearer token to request headers when token exists', () => {
    localStorage.setItem('access_token', 'token-123')

    const result = mocks.state.requestFulfilled({ headers: {} })

    expect(result.headers.Authorization).toBe('Bearer token-123')
  })

  it('keeps request config unchanged when token does not exist', () => {
    const result = mocks.state.requestFulfilled({ headers: {} })

    expect(result.headers.Authorization).toBeUndefined()
  })

  it('returns response data when business code indicates success', () => {
    const result = mocks.state.responseFulfilled({
      data: {
        code: 0,
        msg: '操作成功',
        data: { id: 1 },
      },
    })

    expect(result).toEqual({ id: 1 })
  })

  it('supports string success codes', () => {
    const result = mocks.state.responseFulfilled({
      data: {
        code: 'success',
        msg: '操作成功',
        data: { ok: true },
      },
    })

    expect(result).toEqual({ ok: true })
  })

  it('returns raw body when response has no business code', () => {
    const body = { list: [1, 2, 3] }

    const result = mocks.state.responseFulfilled({ data: body })

    expect(result).toBe(body)
  })

  it('rejects with backend message when business code is not success', async () => {
    await expect(
      mocks.state.responseFulfilled({
        data: {
          code: 1101,
          msg: '用户账号已存在',
          data: null,
        },
      }),
    ).rejects.toThrow('用户账号已存在')
  })

  it('shows backend error message for failed http responses', async () => {
    const error = {
      response: {
        data: {
          msg: '请求参数不合法',
        },
      },
      message: 'Request failed',
    }

    await expect(mocks.state.responseRejected(error)).rejects.toBe(error)
    expect(mocks.state.errorSpy).toHaveBeenCalledWith('请求参数不合法')
  })

  it('falls back to generic message when failed response has no msg', async () => {
    const error = {
      message: 'Network Error',
    }

    await expect(mocks.state.responseRejected(error)).rejects.toBe(error)
    expect(mocks.state.errorSpy).toHaveBeenCalledWith('Network Error')
  })
})
