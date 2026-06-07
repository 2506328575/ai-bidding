import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import ChatMessage from '../../components/ChatMessage.vue'
import type { ChatMessage as Msg } from '../../types/qa'

function msg(overrides: Partial<Msg> = {}): Msg {
  return {
    id: '1', role: 'user', content: '测试', timestamp: Date.now(), ...overrides
  }
}

describe('ChatMessage', () => {
  it('T2.5: 用户消息靠右', () => {
    const wrapper = mount(ChatMessage, { props: { message: msg() } })
    expect(wrapper.find('.chat-message').classes()).toContain('user')
  })

  it('T2.6: AI 消息靠左 + 含来源 + 含意图', () => {
    const wrapper = mount(ChatMessage, {
      props: {
        message: msg({
          role: 'assistant', intent: 'CASE_RETRIEVAL',
          sources: [{ title: '案例A', relevance: '0.9' }]
        })
      }
    })
    expect(wrapper.find('.chat-message').classes()).toContain('assistant')
    expect(wrapper.text()).toContain('案例A')
    expect(wrapper.text()).toContain('案例检索')
  })

  it('T2.7: AI 消息无来源时不渲染 SourceCard', () => {
    const wrapper = mount(ChatMessage, {
      props: { message: msg({ role: 'assistant' }) }
    })
    expect(wrapper.findComponent({ name: 'SourceCard' }).exists()).toBe(false)
  })
})
