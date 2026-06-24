#!/usr/bin/env python3
"""并发压测脚本：模拟 50 并发下 200 次下单，验证分布式锁防超卖。"""
import concurrent.futures as cf
import requests, time, sys

URL = "http://localhost:8082/api/orders"
N_THREADS = 50
N_REQ = 200

def hit(_):
    try:
        r = requests.post(URL, params={"userId": 1, "goodsId": 1, "quantity": 1}, timeout=5)
        return r.status_code, r.json().get("id") if r.status_code == 200 else r.text
    except Exception as e:
        return 0, str(e)

if __name__ == "__main__":
    t0 = time.time()
    with cf.ThreadPoolExecutor(max_workers=N_THREADS) as pool:
        results = list(pool.map(hit, range(N_REQ)))
    dt = time.time() - t0
    ok = sum(1 for s, _ in results if s == 200)
    fail = N_REQ - ok
    print(f"并发 {N_THREADS} / 总请求 {N_REQ} / 成功 {ok} / 失败 {fail} / 耗时 {dt:.2f}s / QPS {N_REQ/dt:.0f}")
    print("失败明细样例:", [r for s, r in results if s != 200][:5])
