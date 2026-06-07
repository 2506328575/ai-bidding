import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import App from '../../App.vue'

describe('App', () => {
  it('T0.1: 渲染 App 组件', () => {
    const wrapper = mount(App, {
      global: {
        stubs: { ChatView: { template: '<div>Chat</div>' } }
      }
    })
    expect(wrapper.text()).toContain('Chat')
  })
})
