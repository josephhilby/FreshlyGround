import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
    plugins: [react()],
    server: {
        proxy: {
            '/compile': 'http://localhost:7070',
            '/health': 'http://localhost:7070',
        },
    },
})
