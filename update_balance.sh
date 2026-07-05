#!/bin/bash
# API Balance Monitor - 自动更新脚本
# 用法: bash update_balance.sh

cd /tmp/api-balance-monitor

# 查询DeepSeek余额
DS_BALANCE=$(curl -s 'https://api.deepseek.com/user/balance' \
  -H 'Accept: application/json' \
  -H 'Authorization: Bearer sk-c638d552d4f641b7b55f8a656c8ae28a' | \
  python3 -c "import sys,json; d=json.load(sys.stdin); print(d['balance_infos'][0]['total_balance'])" 2>/dev/null)

# 查询Sub2余额
S2_BALANCE=$(curl -s 'https://raw.githubusercontent.com/anhphuocchu2999-cloud/api-balance-monitor/main/docs/balance.json' | \
  python3 -c "import sys,json; d=json.load(sys.stdin); print(d['display_balance'].replace('$',''))" 2>/dev/null)

# 计算使用百分比
PROGRESS=$(python3 -c "b=float('${S2_BALANCE:-0}'); print(int((10-b)/10*100))")
NOW=$(date -u +'%Y-%m-%dT%H:%M:%SZ')

# 更新balance.json
cat > docs/balance.json << EOF
{"display_balance":"$${S2_BALANCE}","display_detail":"Used ${PROGRESS}%","progress_pct":${PROGRESS},"updated_at":"${NOW}","deepseek_display":"$${DS_BALANCE}","mimo_display":"--","providers_display":"DeepSeek $${DS_BALANCE}    MiMo --"}
EOF

echo "Updated: DeepSeek=$${DS_BALANCE} Sub2=$${S2_BALANCE}"

# 提交并推送
git add docs/balance.json
git commit -m "Auto update: DS=$${DS_BALANCE} S2=$${S2_BALANCE}" 2>/dev/null
git push origin main 2>/dev/null && echo "Pushed to GitHub!"
