import '@mdi/font/css/materialdesignicons.css'
import 'vuetify/styles'
import { createApp } from 'vue'
import { createVuetify } from 'vuetify'
import App from './App.vue'
import { restore } from './auth'
import { router } from './router'

const vuetify = createVuetify({
  theme: {
    defaultTheme: window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light',
    themes: {
      light: { colors: { primary: '#6d3fe0' } },
      dark: { colors: { primary: '#a98bff' } },
    },
  },
  defaults: {
    VBtn: { rounded: 'lg' },
    VCard: { rounded: 'lg' },
    VTextField: { variant: 'outlined', density: 'comfortable' },
  },
})

// la pagina di ritorno dal login gestisce da se' la sessione
if (window.location.pathname !== '/callback') void restore()

createApp(App).use(router).use(vuetify).mount('#app')
