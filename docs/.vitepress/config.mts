import { defineConfig } from 'vitepress'
import { withMermaid } from "vitepress-plugin-mermaid"
import type MarkdownIt from 'markdown-it'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { createHighlighter } from 'shiki'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

const ebnfGrammar = JSON.parse(
    fs.readFileSync(
        path.resolve(__dirname, './syntax/ebnf.tmLanguage.json'),
        'utf8'
    )
)

const ebnfLang = {
  name: 'ebnf',
  aliases: ['ebnf'],
  scopeName: ebnfGrammar.scopeName ?? 'source.bnf',
  ...ebnfGrammar
}

const ebnfHighlighter = await createHighlighter({
  themes: ['github-light', 'github-dark'],
  langs: [ebnfLang]
})

export default withMermaid(defineConfig({
  title: 'FreshlyGround',
  description: 'A coffee-fueled compiler built from first principles',

  markdown: {
    lineNumbers: true,
    config(md: MarkdownIt) {
      const defaultFence =
          md.renderer.rules.fence ??
          ((tokens, idx, options, env, self) =>
              self.renderToken(tokens, idx, options))

      md.renderer.rules.fence = (tokens, idx, options, env, self) => {
        const token = tokens[idx]
        const info = (token.info || '').trim()
        const lang = info.split(/\s+/)[0]

        if (lang !== 'ebnf') {
          return defaultFence(tokens, idx, options, env, self)
        }

        return ebnfHighlighter.codeToHtml(token.content, {
          lang: 'ebnf',
          themes: {
            light: 'github-light',
            dark: 'github-dark'
          }
        })
      }
    }
  },

  themeConfig: {
    nav: [
      { text: 'Docs', link: '/' },
      { text: 'Live Compiler', link: 'https://freshlyground.onrender.com' },
      { text: 'GitHub', link: 'https://github.com/josephhilby/FreshlyGround' }
    ],

    sidebar: [
      {
        text: 'Compiler Architecture',
        items: [
          { text: 'Index', link: '/' },
          { text: 'About', link: '/00_about.md' },
          { text: 'Compiler Pipeline', link: '/01_pipeline' },
          { text: 'Language Syntax', link: '/02_syntax' },
          { text: 'Program Model', link: '/03_program_model' },
          { text: 'Semantics', link: '/04_semantics' },
          { text: 'Backend', link: '/05_backend' }
        ]
      }
    ]
  }
}))
