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
- `src/App.vue`：根页面出口，具体布局由路由挂载
- `src/layouts/CustomerLayout.vue`：普通用户消费端布局
- `src/layouts/MerchantLayout.vue`：商家管理端布局
- `src/components/layout/`：应用导航栏、侧边栏等布局组件
- `src/components/common/`：认证表单外壳、空状态、确认操作等公共组件
- `src/router/`：路由入口
- `src/router/modules/`：按认证、普通用户、商家、验收拆分路由
- `src/api/http.js`：Axios 统一请求实例，集中处理 baseURL、token 和响应错误
- `src/api/*.js`：按业务模块封装接口调用，页面不直接写 axios
- `src/views/`：业务页面
- `src/components/`：公共组件
- `src/stores/`：全局状态
- `src/tests/`：前端测试

## 架构约定

1. 页面跳转统一写在 `src/router/modules/`，不要把所有路由长期堆在 `router/index.js`。
2. 导航栏、侧边栏、空状态、确认弹窗等通用 UI 放在 `src/components/` 复用。
3. 业务页面只调用 `src/api/*.js` 暴露的方法，不直接 `import axios`。
4. 接口路径统一由 `src/api/http.js` 的 `baseURL` 接管，业务 API 文件只写资源路径。
5. 后端响应结构变化时，优先调整 `src/api/http.js` 或对应业务 API 文件，减少页面改动。
6. `/` 作为登录/注册入口，不承载登录后的业务功能。
7. 普通用户功能统一放在 `/customer/*`，商家功能统一放在 `/merchant/*`。
8. 个人信息在普通用户端和商家端都保留入口。
9. 普通用户与商家的登录/注册页面保持独立，但复用通用认证表单容器。
