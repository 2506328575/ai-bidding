import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import QaView from '../../views/QaView.vue'
import * as qaApi from '../../api/qa'

vi.mock('../../api/qa')

const mockResp = {
  answer: '根据案例库，我们曾为XX制造集团实施ERP项目...',
  intent: 'CASE_RETRIEVAL',
  sources: [{ title: 'XX制造案例', relevance: '0.9' }],
  tokensUsed: 150,
  latencyMs: 2300
}

function mountQaView() {
  return mount(QaView, {
    global: { plugins: [ElementPlus] }
  })
}

describe('QaView', () => {
  beforeEach(() => vi.clearAllMocks())

  it('T3.6: 发送后消息列表有用户消息 + AI 回复', async () => {
    vi.mocked(qaApi.askQuestion).mockResolvedValue(mockResp)
    const wrapper = mountQaView()
    const vm = wrapper.vm as any
    await vm.handleSend('有没有制造业的案例？')
    await flushPromises()
    expect(wrapper.text()).toContain('有没有制造业的案例？')
    expect(wrapper.text()).toContain('XX制造集团')
  })

  it('T3.8: API 错误时显示降级', async () => {
    vi.mocked(qaApi.askQuestion).mockRejectedValue(new Error('Network Error'))
    const wrapper = mountQaView()
    const vm = wrapper.vm as any
    await vm.handleSend('test')
    await flushPromises()
    expect(wrapper.text()).toContain('暂时不可用')
  })

  it('T4.1: 初始空状态显示欢迎', () => {
    const wrapper = mountQaView()
    expect(wrapper.text()).toContain('您好')
    expect(wrapper.text()).toContain('售前助手')
  })
})
