import { config } from '@vue/test-utils'
import { createPinia } from 'pinia'

config.global.stubs = {
  'router-link': true,
  'router-view': true,
}

config.global.plugins = [createPinia()]
