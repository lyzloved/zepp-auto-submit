# Zepp 网页自动化脚本（Windows / Python）

本项目使用 Playwright 控制 Chromium，操作 `https://zepp.net.cn/` 的公开网页表单：填写个人账户信息、目标步数，并点击「立即提交」。

**这是网页操作自动化，不是 Zepp 官方 API 客户端。** 不绕过验证码、账号权限或卡密支付。当前未提供真实 Zepp 账号进行网站提交测试，故不能保证一定更新微信运动。网页可能改版，免费线路也可能有提交限制。请仅在你有权使用、且网站允许自动化的前提下运行。

## 最快使用方式（Windows 10/11）

电脑先安装 Python 3.10+，安装时勾选 Add Python to PATH，且保证网络可用。

1. 双击 `install.cmd`：安装 Playwright、keyring、Chromium。
2. 双击 `setup.cmd`：输入 **自己的** Zepp 邮箱/手机号、目标步数（例如 15570）、密码。密码通过 Windows 凭据管理器保存，不写入代码或 config.json。
3. 双击 `test_fill_only.cmd`：检查页面填写是否正确，该步骤不会点击提交。
4. 双击 `run_once.cmd`：执行一次提交。查看 `logs/zepp_auto.log` 中的结果，并在微信运动核对是否真正同步。
5. 经验证可用后，双击 `schedule_daily_2100.cmd`：Windows 每天 **21:00** 自动执行（电脑本地时区）。

### 命令行运行

在项目目录使用 PowerShell 或 CMD：

```powershell
py -3 -m pip install -r requirements.txt
py -3 -m playwright install chromium
py -3 zepp_auto.py setup
py -3 zepp_auto.py run --visible --dry-run
py -3 zepp_auto.py run --visible
py -3 zepp_auto.py schedule --time 21:00
```

其他实用命令：

```powershell
py -3 zepp_auto.py set-steps --steps 18888
py -3 zepp_auto.py run
py -3 zepp_auto.py unschedule
```

## 执行结果含义

- `ACCEPTED`：站点网页**明确显示**成功提示；不代表已在微信运动到账。
- `FAILED`：站点明确提示失败。
- `MANUAL`：遇到需要人为介入的提示（登录、验证码、会员等），不绕过。
- `UNKNOWN`：没有识别出明确结果，可能是网页结构变化或等待超时；**不会自动重试**，以免重复提交。
- `ERROR`：运行异常（网络/浏览器依赖/网站改版）。
- `DRY_RUN`：仅填表，未提交。

站点提示“目标步数低于现有步数可能不更新”。程序不会从微信读取步数。某些账户可能要求已在官方 Zepp Life 中正确绑定第三方平台。

## Windows 定时任务限制

计划任务使用系统 `schtasks.exe`，仅在**本机开机、当前 Windows 用户已登录、未休眠**时自动运行。不同用户或 Windows 服务账号不能保证取到当前用户保存的密码。执行日志位于 `logs/`。

## 隐私与安全

- **不要发给别人 Zepp 密码、Token、Cookie、完整抓包内容**。网站是第三方服务，向其输入账号密码有泄漏风险，不建议复用密码。
- 不采集原始 HTTP 请求体，不自动保存网页截图或浏览器 Cookie。
- 不做自动刷新验证码或购买会员，不代替账户本人绕过验证。
- 账户参与运动竞赛、积分、保险或奖励时，虚构运动步数可能造成违规，请勿用于欺诈。

## 兼容和测试范围

网页表单的定位依据是已公开索引中的 `请输入ZEPP账号`、`请输入ZEPP密码`、`请输入同步步数`、`立即提交`。目前只做了离线浏览器表单测试与语法检查，**未对 zepp.net.cn 提交真实账号请求**。

## Android APP

仓库的 [`android-app/`](android-app/) 目录包含原生 Android 版本。当前版本为 1.5.2，最低支持 Android 6.0，提供输入框与滑块联动选择步数、单次提交、卡密兑换及状态检查、会员到期提醒、密码加密保存、提交动画和每 2 小时更新的每日一言。

可安装包位于 [`android-app/releases/StepSubmit-1.5.2.apk`](android-app/releases/StepSubmit-1.5.2.apk)。APK 使用项目本地开发签名构建，不代表 Zepp、微信或第三方网站官方发布。
