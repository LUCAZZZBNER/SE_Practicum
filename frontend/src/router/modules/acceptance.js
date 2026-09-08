export default [
  {
    path: '/acceptance',
    name: 'Acceptance',
    component: () => import('../../views/AcceptanceView.vue'),
    meta: { title: '验收记录' },
  },
]
