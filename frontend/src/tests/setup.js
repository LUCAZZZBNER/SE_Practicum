import { config } from '@vue/test-utils'

config.global.stubs = {
  'router-link': true,
  'router-view': true,
}
