import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import IntentBadge from '../../components/IntentBadge.vue'

describe('IntentBadge', () => {
  it('T2.1: CASE_RETRIEVAL 显示案例检索', () => {
    const wrapper = mount(IntentBadge, { props: { intent: 'CASE_RETRIEVAL' } })
    expect(wrapper.text()).toContain('案例检索')
    expect(wrapper.classes()).toContain('case_retrieval')
  })

  it('T2.2: 未知 intent 显示原文', () => {
    const wrapper = mount(IntentBadge, { props: { intent: 'UNKNOWN_TYPE' } })
    expect(wrapper.text()).toContain('UNKNOWN_TYPE')
  })
})
