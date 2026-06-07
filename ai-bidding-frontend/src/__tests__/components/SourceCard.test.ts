import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import SourceCard from '../../components/SourceCard.vue'

describe('SourceCard', () => {
  it('T2.3: 渲染 title + relevance', () => {
    const wrapper = mount(SourceCard, {
      props: { source: { title: 'XX制造案例', relevance: '0.95' } }
    })
    expect(wrapper.text()).toContain('XX制造案例')
    expect(wrapper.text()).toContain('0.95')
  })
})
