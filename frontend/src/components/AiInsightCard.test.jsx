import { cleanup, render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import AiInsightCard from './AiInsightCard.jsx'
import { generateInsight, getInsightStatus, getLatestInsight } from '../api/adminApi.js'

vi.mock('../api/adminApi.js', () => ({
  generateInsight: vi.fn(),
  getInsightStatus: vi.fn(),
  getLatestInsight: vi.fn(),
}))

describe('AiInsightCard', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    getInsightStatus.mockResolvedValue({ data: null })
  })

  afterEach(() => {
    cleanup()
  })

  it('prompts the admin when no report exists yet', async () => {
    getLatestInsight.mockResolvedValue({ data: null })

    render(<AiInsightCard />)

    expect(await screen.findByText(/暂无运营洞察报告/)).toBeTruthy()
  })

  it('renders the latest report with generation metadata', async () => {
    getLatestInsight.mockResolvedValue({
      data: {
        id: 9,
        content: '社区目前共有 43 位用户。',
        model: 'deepseek-flash',
        latencyMs: 5004,
        createTime: '2026-09-19T15:29:47',
      },
    })

    render(<AiInsightCard />)

    expect(await screen.findByText(/社区目前共有 43 位用户/)).toBeTruthy()
    expect(screen.getByText(/生成于 2026-09-19 15:29/)).toBeTruthy()
    expect(screen.getByText(/deepseek-flash/)).toBeTruthy()
  })

  it('enters the generating state after the admin triggers a run', async () => {
    getLatestInsight.mockResolvedValue({ data: null })
    generateInsight.mockResolvedValue({ code: 200 })

    render(<AiInsightCard />)
    const button = await screen.findByRole('button', { name: /生成洞察/ })
    button.click()

    // 生成是异步的：点完必须立刻切到"生成中"，否则管理员会以为没反应而反复点
    await waitFor(() => expect(generateInsight).toHaveBeenCalled())
    expect(await screen.findByRole('button', { name: /生成中/ })).toBeTruthy()
    expect(screen.getByText(/已开始生成/)).toBeTruthy()
  })

  it('resumes the generating state when a run is already in progress', async () => {
    // 刷新页面时上一次生成可能还没跑完：应直接进入轮询，而不是显示"暂无报告"
    getInsightStatus.mockResolvedValue({ data: { id: 9, status: 'PENDING' } })

    render(<AiInsightCard />)

    expect(await screen.findByRole('button', { name: /生成中/ })).toBeTruthy()
    expect(getLatestInsight).not.toHaveBeenCalled()
  })

  it('surfaces an error when triggering fails', async () => {
    getLatestInsight.mockResolvedValue({ data: null })
    generateInsight.mockRejectedValue(new Error('已有一份运营洞察正在生成，请稍候'))

    render(<AiInsightCard />)
    const button = await screen.findByRole('button', { name: /生成洞察/ })
    button.click()

    expect(await screen.findByText(/已有一份运营洞察正在生成/)).toBeTruthy()
  })
})
