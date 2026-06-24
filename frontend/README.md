# Frontend

Vue 3 + TypeScript + Vite 前端工程。

## 技术

- Vue 3
- TypeScript
- Vant 4：手机用户端
- Element Plus：后台管理
- Pinia
- Vue Router
- Axios

## 启动

```powershell
pnpm install
pnpm dev --host 0.0.0.0
```

默认端口：

```text
http://localhost:5173
```

手机访问时使用电脑局域网 IP：

```text
http://电脑IPv4:5173
```

Vite 已将 `/api` 代理到：

```text
http://localhost:8080
```

## 目录

```text
src/api          接口请求
src/types        前后端共享字段类型
src/router       路由
src/stores       Pinia 状态
src/components   通用组件
src/views/mobile 手机页面
src/views/admin  后台页面
src/utils        摇一摇等工具函数
```

