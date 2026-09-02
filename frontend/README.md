# Frontend

Vue 3 + Vue Router 4 + Element Plus 前端项目。

## 开始运行

```bash
npm install
npm run dev
```

浏览器打开终端输出的地址，默认是 `http://localhost:5173`。

## 当前脚手架

- `src/main.js`：创建 Vue 应用，注册 Pinia、Router、Element Plus
- `src/App.vue`：应用外壳、导航栏和页面出口 `router-view`
- `src/router/`：路由配置
- `src/api/http.js`：Axios 统一请求实例，暂不绑定具体业务接口
- `src/views/`：业务页面
- `src/components/`：公共组件
- `src/stores/`：全局状态
- `src/tests/`：前端测试

- `src/api/`：Axios 请求封装
- `src/components/`：公共组件
- `src/router/`：路由配置
- `src/stores/`：状态管理
- `src/utils/`：工具函数
- `src/views/`：业务页面
- `src/tests/`：Vitest 单元和集成测试

前端工程初始化后，在此目录执行 `npm run test`。
