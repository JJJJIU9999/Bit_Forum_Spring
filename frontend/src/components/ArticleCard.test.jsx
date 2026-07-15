import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import ArticleCard from './ArticleCard.jsx'

describe('ArticleCard', () => {
  it('renders supplied footer content exactly once', () => {
    render(
      <ArticleCard article={{ id: 1, title: 'Readable title', content: 'Summary', userId: 2 }}>
        <button type="button">footer action</button>
      </ArticleCard>,
    )

    expect(screen.getAllByRole('button', { name: 'footer action' })).toHaveLength(1)
  })
})
