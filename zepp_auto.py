#!/usr/bin/env python3
"""Personal Zepp webpage automation (unofficial, subject to site terms)."""
import argparse
import getpass
import json
import logging
import os
from pathlib import Path
import platform
import re
import subprocess
import sys
import time
from urllib.parse import urlparse

ROOT = Path(__file__).resolve().parent
CONFIG = ROOT / "config.json"
LOGS = ROOT / "logs"
SERVICE = "zepp-net-cn-personal-automation"
TASK = "ZeppPersonalDailySubmit"
SITE = "https://zepp.net.cn/"
SUCCESS = [r"提交成功", r"同步成功", r"更新成功", r"修改成功", r"步数.{0,12}成功"]
FAILURE = [r"提交失败", r"同步失败", r"密码.{0,12}错误", r"账号.{0,12}错误", r"余额.{0,12}不足"]
MANUAL = [r"验证码", r"人机验证", r"滑块验证", r"请先购买", r"请联系客服", r"需要会员"]

def validate_steps(steps):
    if type(steps) is not int or not 1 <= steps <= 99999:
        raise ValueError("步数必须为 1~99999 的整数")
    return steps

def valid_target(url):
    parsed = urlparse(url)
    return parsed.scheme == "https" and parsed.hostname == "zepp.net.cn" and parsed.port in (None, 443)

def classify(before, after):
    def appeared(pattern):
        return any(len(re.findall(p, after, re.I)) > len(re.findall(p, before, re.I)) for p in pattern)
    if appeared(FAILURE):
        return "FAILED"
    if appeared(MANUAL):
        return "MANUAL"
    if appeared(SUCCESS):
        return "ACCEPTED"
    return "UNKNOWN"

def keyring():
    import keyring as module
    return module

def load_config():
    if not CONFIG.is_file():
        raise RuntimeError("请先运行 setup")
    cfg = json.loads(CONFIG.read_text(encoding="utf-8"))
    if not isinstance(cfg.get("account"), str) or not cfg["account"].strip():
        raise ValueError("缺少账号")
    validate_steps(cfg.get("steps"))
    return cfg

def save_config(cfg):
    temp = CONFIG.with_suffix(".tmp")
    temp.write_text(json.dumps(cfg, ensure_ascii=False, indent=2), encoding="utf-8")
    os.replace(temp, CONFIG)
    try:
        os.chmod(CONFIG, 0o600)
    except OSError:
        pass

def setup(args):
    account = (args.account or input("Zepp 账号：")).strip()
    if not account or any(c.isspace() for c in account):
        raise ValueError("账号不能为空或含空格")
    steps = validate_steps(args.steps if args.steps is not None else int(input("目标步数：")))
    password = getpass.getpass("Zepp 密码：")
    if not password:
        raise ValueError("密码不能为空")
    keyring().set_password(SERVICE, account, password)
    if keyring().get_password(SERVICE, account) != password:
        raise RuntimeError("Windows 凭据管理器写入失败")
    save_config({"account": account, "steps": steps})
    print("已保存配置。请先运行 run --visible --dry-run")
    return 0

def set_steps(args):
    cfg = load_config()
    cfg["steps"] = validate_steps(args.steps)
    save_config(cfg)
    print("步数已更新")
    return 0

def fill_form(page, account, password, steps):
    for text, fallback, value in (
        (r"ZEPP账号", 'input[type="email"], input[autocomplete="username"]', account),
        (r"ZEPP密码", 'input[type="password"]', password),
        (r"同步步数", 'input[type="number"]', str(steps)),
    ):
        el = page.get_by_placeholder(re.compile(text, re.I)).first
        if el.count() == 0:
            el = page.locator(fallback).first
        el.wait_for(state="visible", timeout=12000)
        el.fill(value)

def click_submit(page):
    button = page.get_by_role("button", name=re.compile("立即提交")).first
    if button.count() == 0:
        button = page.get_by_text("立即提交", exact=True).first
    button.click(timeout=12000)

def run(args):
    from playwright.sync_api import sync_playwright
    cfg = load_config()
    account, steps = cfg["account"], cfg["steps"]
    password = keyring().get_password(SERVICE, account)
    if not password:
        raise RuntimeError("凭据管理器中未找到密码；请重新运行 setup")
    LOGS.mkdir(exist_ok=True)
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s",
                        handlers=[logging.FileHandler(LOGS / "zepp_auto.log", encoding="utf-8"),
                                  logging.StreamHandler(sys.stdout)])
    status = "ERROR"
    try:
        with sync_playwright() as playwright:
            browser = playwright.chromium.launch(headless=not args.visible)
            context = browser.new_context(locale="zh-CN")
            page = context.new_page()
            dialogs = []
            def on_dialog(dialog):
                dialogs.append(dialog.message)
                dialog.accept()
            page.on("dialog", on_dialog)
            page.goto(SITE, wait_until="domcontentloaded", timeout=45000)
            if not valid_target(page.url):
                raise RuntimeError("网页跳转到了意外域名，不会填写密码")
            fill_form(page, account, password, steps)
            password = None
            if args.dry_run:
                status = "DRY_RUN"
                logging.info("DRY_RUN: 已填表但未提交")
                if args.visible:
                    page.wait_for_timeout(1500)
            else:
                before = page.locator("body").inner_text()
                click_submit(page)
                deadline = time.monotonic() + args.timeout
                status = "UNKNOWN"
                while time.monotonic() < deadline:
                    for dialog_text in dialogs:
                        status = classify("", dialog_text)
                        if status != "UNKNOWN":
                            break
                    if status != "UNKNOWN":
                        break
                    try:
                        after = page.locator("body").inner_text(timeout=4000)
                        status = classify(before, after)
                    except Exception:
                        status = "UNKNOWN"
                    if status != "UNKNOWN":
                        break
                    page.wait_for_timeout(650)
            context.close()
            browser.close()
    except Exception as exc:
        # Avoid logging Playwright traces: they can include filled credentials.
        logging.error("执行异常：%s", type(exc).__name__)
        status = "ERROR"
    finally:
        password = None
    logging.info("结果：%s。网站成功提示不等于微信运动到账。", status)
    return 0 if status in ("ACCEPTED", "DRY_RUN") else 1

def schedule(args):
    if platform.system() != "Windows":
        raise RuntimeError("仅支持 Windows 计划任务")
    load_config()
    if not re.fullmatch(r"(?:[01]\d|2[0-3]):[0-5]\d", args.time):
        raise ValueError("时间格式应为 HH:MM")
    LOGS.mkdir(exist_ok=True)
    runner = ROOT / "run_daily.cmd"
    runner.write_text('@echo off\ncd /d "' + str(ROOT) + '"\n"' + sys.executable +
                      '" "' + str(Path(__file__).resolve()) + '" run\n', encoding="utf-8")
    result = subprocess.run(["schtasks.exe", "/Create", "/F", "/SC", "DAILY",
                             "/ST", args.time, "/TN", TASK, "/TR", str(runner), "/IT"],
                            capture_output=True, text=True)
    if result.returncode:
        raise RuntimeError("创建计划任务失败，请检查 Windows 任务计划程序权限")
    print("已创建每日任务。电脑需要开机、未休眠且当前用户已登录。")
    return 0

def unschedule(args):
    if platform.system() != "Windows":
        raise RuntimeError("仅支持 Windows")
    result = subprocess.run(["schtasks.exe", "/Delete", "/TN", TASK, "/F"],
                            capture_output=True)
    if result.returncode:
        raise RuntimeError("删除计划任务失败")
    return 0

def main():
    parser = argparse.ArgumentParser(description="Zepp 网页自动化工具")
    subs = parser.add_subparsers(dest="action", required=True)
    p = subs.add_parser("setup"); p.add_argument("--account"); p.add_argument("--steps", type=int); p.set_defaults(func=setup)
    p = subs.add_parser("set-steps"); p.add_argument("--steps", type=int, required=True); p.set_defaults(func=set_steps)
    p = subs.add_parser("run"); p.add_argument("--visible", action="store_true"); p.add_argument("--dry-run", action="store_true"); p.add_argument("--timeout", type=int, default=35); p.set_defaults(func=run)
    p = subs.add_parser("schedule"); p.add_argument("--time", required=True); p.set_defaults(func=schedule)
    p = subs.add_parser("unschedule"); p.set_defaults(func=unschedule)
    args = parser.parse_args()
    try:
        if hasattr(args, "timeout") and args.timeout <= 0:
            raise ValueError("timeout 必须大于 0")
        return args.func(args)
    except (ValueError, RuntimeError, ImportError) as exc:
        print("错误：" + str(exc), file=sys.stderr)
        return 2

if __name__ == "__main__":
    sys.exit(main())
