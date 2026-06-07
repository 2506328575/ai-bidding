import { describe, it, expect, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import ChatInput from '../../components/ChatInput.vue'

function mountInput(props = { loading: false }) {
  return mount(ChatInput, {
    props,
    global: { plugins: [ElementPlus] }
  })
}

describe('ChatInput', () => {
  it('T3.1: 空输入时发送按钮 disabled', () => {
    const wrapper = mountInput()
    const btn = wrapper.find('[data-testid="send-button"]')
    expect(btn.attributes('disabled')).toBeDefined()
  })

  it('T3.2: 输入文字后按钮 enabled', async () => {
    const wrapper = mountInput()
    const textarea = wrapper.find('textarea')
    await textarea.setValue('测试')
    const btn = wrapper.find('[data-testid="send-button"]')
    expect(btn.attributes('disabled')).toBeUndefined()
  })

  it('T3.3: 点击发送触发 @send', async () => {
    const wrapper = mountInput()
    const textarea = wrapper.find('textarea')
    await textarea.setValue('测试问题')
    await wrapper.find('[data-testid="send-button"]').trigger('click')
    expect(wrapper.emitted('send')).toBeTruthy()
    expect(wrapper.emitted('send')![0]).toEqual(['测试问题'])
  })

  it('T3.4: 发送后清空输入框', async () => {
    const wrapper = mountInput()
    const textarea = wrapper.find('textarea')
    await textarea.setValue('测试')
    await wrapper.find('[data-testid="send-button"]').trigger('click')
    expect((textarea.element as HTMLTextAreaElement).value).toBe('')
  })

  it('T3.5: 加载中时按钮 disabled', () => {
    const wrapper = mountInput({ loading: true })
    const btn = wrapper.find('[data-testid="send-button"]')
    expect(btn.attributes('disabled')).toBeDefined()
  })
})
