# 我在太原馋哭了

手机优先的太原/中北大学周边觅食推荐 PWA。核心推荐来自本地 SQLite 餐厅库；当前数据库由高德地图批量拉取真实餐厅并经 DeepSeek 自动补全标签生成，CSV 保留为备用导入能力。DeepSeek 可用于自然语言觅食意图解析、文案润色和标签增强，但不决定推荐排序。

## 目录

```text
frontend/   Vue 3 + TypeScript + Vant 用户端和 Element Plus 后台
backend/    Spring Boot 3 + Java 21 + SQLite 后端
data/       CSV 模板和备用种子餐厅数据
.env.example  本地环境变量模板，不包含真实 Key
start.bat     Windows 一键启动脚本
```

## 运行环境

建议在 Windows 10/11、macOS 或 Linux 上运行。当前项目在 Windows + PowerShell 下验证通过。

必需环境：

```text
Java JDK 21
Maven 3.9+
Node.js 20+
pnpm 9+
Git
```

说明：

- 后端基于 Spring Boot 3.3 和 SQLite，SQLite JDBC 会随 Maven 依赖安装，不需要单独安装 SQLite 服务。
- 前端基于 Vue 3、Vite、TypeScript、Vant、Element Plus。
- 默认后端端口为 `8080`，默认前端端口为 `5173`。
- 高德和 DeepSeek Key 都不是必需项；未配置时，本地餐厅库、条件推荐、摇一摇、收藏、浏览记录等核心功能仍可运行。
- 需要高德实时搜索、逆地理编码或 DeepSeek 增强时，再通过 `.env` 或系统环境变量配置 Key。

## 环境变量

复制 `.env.example` 为 `.env` 后按需填写：

```text
APP_ENV=dev
DB_PATH=./data/food-seeker.db
JWT_SECRET=replace-with-long-random-string
AMAP_WEB_SERVICE_KEY=
DEEPSEEK_ENABLED=false
DEEPSEEK_API_KEY=
DEEPSEEK_BASE_URL=https://api.deepseek.com
DEEPSEEK_MODEL=deepseek-chat
DEEPSEEK_TIMEOUT_SECONDS=15
```

安全约束：

- `.env`、`backend/.env`、`frontend/.env` 已在 `.gitignore` 中忽略；
- 不要把 `AMAP_WEB_SERVICE_KEY`、`DEEPSEEK_API_KEY`、`JWT_SECRET` 的真实值提交到 Git；
- `backend/src/main/resources/application.yml` 只读取环境变量，不保存真实 Key。

## 启动后端

```powershell
cd backend
mvn package
java -jar target/food-seeker-0.0.1-SNAPSHOT.jar
```

健康检查：

```text
http://localhost:8080/api/health
```

Swagger：

```text
http://localhost:8080/swagger-ui.html
```

## 一键启动

```powershell
start.bat
```

`start.bat` 不内置任何真实 Key。需要高德或 DeepSeek 时，可以复制 `.env.example` 为 `.env` 后填写本机 Key，也可以先在系统环境变量或当前命令行中配置：

```powershell
$env:AMAP_WEB_SERVICE_KEY="你的高德Web服务Key"
$env:DEEPSEEK_ENABLED="true"
$env:DEEPSEEK_API_KEY="你的DeepSeek Key"
```

未配置 Key 时，后端仍可启动，本地推荐、AI 推荐本地解析、餐厅列表和摇一摇可用；高德附近搜索和 AI 润色会走降级。

## 启动前端

```powershell
cd frontend
pnpm install
pnpm dev --host 0.0.0.0
```

电脑访问：

```text
http://localhost:5173
```

手机访问：

1. 手机和电脑连接同一个 Wi-Fi；
2. 电脑运行 `ipconfig` 查 IPv4 地址；
3. 手机打开 `http://电脑IPv4:5173`。

前端开发服务器会把 `/api` 代理到 `http://localhost:8080`，所以手机访问前端时也能请求电脑上的后端。

## 手机真机摇一摇测试

为了方便课程演示，前端已将普通局域网 HTTP 作为演示来源放行。手机可以直接打开：

```text
http://电脑IPv4:5173
```

只要当前手机浏览器暴露了 `DeviceMotionEvent`，页面就会允许点击“开启摇一摇”并监听真实晃动。

注意：这只是取消项目前端自己的拦截。浏览器底层如果仍然因为系统策略隐藏 `DeviceMotionEvent`，前端代码无法强制打开传感器，页面会提示当前浏览器没有开放运动传感器。

备选方式一：Android USB + ADB 转发。

```powershell
adb devices
adb reverse tcp:5173 tcp:5173
adb reverse tcp:8080 tcp:8080
```

然后在手机 Chrome/Edge 打开：

```text
http://localhost:5173/shake
```

备选方式二：Android Chrome 开发临时开关。

1. 手机 Chrome 打开 `chrome://flags/#unsafely-treat-insecure-origin-as-secure`；
2. 启用该 flag；
3. 输入当前前端地址，例如 `http://192.168.1.3:5173`；
4. 重启 Chrome；
5. 再打开 `/shake` 页面测试。

该方式只用于本地开发和答辩测试，不用于正式环境。

备选方式三：使用 HTTPS 隧道。

可用 ngrok、cloudflared 等工具把 `http://localhost:5173` 暴露成 HTTPS 地址，手机访问 HTTPS 地址后再测试摇一摇。

注意：

- 优先使用 Android Chrome/Edge 或 iOS Safari；
- 微信内置浏览器可能不开放真实摇动传感器；
- 如果页面显示“已开启监听，但暂未收到传感器数据”，检查浏览器的传感器/动作权限，或换 Chrome/Edge 测试。

## PWA 和手机页面

前端已接入 PWA：

```text
manifest.webmanifest
sw.js
app-icon.svg
maskable-icon.svg
```

Android Chrome 打开 `http://电脑IPv4:5173` 后，可在浏览器菜单中尝试“添加到主屏幕”。当前 PWA 可缓存页面壳和静态资源；餐厅数据、推荐和地图仍需连接本机后端。

主要手机路由：

```text
/                 首页
/search           AI 觅食 / 本地算法
/recommendations  推荐结果
/restaurants/:id  餐厅详情
/shake            摇一摇
/nearby           附近地图
/discover         分类 / 榜单
/favorites        收藏
/history          浏览记录
/profile          我的
/taste-mirror     口味魔镜
```

底部导航：

```text
首页 / 附近 / 发现 / 收藏 / 我的
```

## 数据

默认数据库路径：

```text
backend/data/food-seeker.db
```

首次启动时如果该文件不存在，后端会自动创建 SQLite 表结构。仓库不提交本机 `.db` 文件，避免把本地运行状态、测试数据或潜在敏感内容发布到 Git。

CSV 模板仍保留用于备用导入：

```text
data/restaurants_template.csv
```

种子数据：

```text
data/restaurants_seed.csv
```

需要完整真实餐厅库时，可以在后台管理页使用 CSV 导入，或配置 `AMAP_WEB_SERVICE_KEY` 后通过高德拉取入库。

## 硬性约束

- 真机摇动触发推荐是 P0；
- 点击按钮只是兜底；
- 高德批量入库必须走后端管理接口，不得把 Key 写入前端；
- 高德实时附近结果仅供位置参考，未导入前不参与本地推荐排序；
- DeepSeek 可解析自然语言为白名单筛选条件，但不决定排序、不生成餐厅事实；
- API Key 不写入前端或 Git；
- 每次完成任务必须写入 `对接文档/YYYY-MM-DD_任务名.md`。
