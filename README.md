# API Balance Monitor

This repo publishes a minimal public JSON endpoint for KWGT via GitHub Pages.

## Public output
- `docs/balance.json`: only widget-safe display data
- `docs/index.html`: preview page

## Private data handling
- Store the real API key only in GitHub Actions secret `SUB2_API_KEY`
- Do not commit the key into repository files
- The public JSON does not contain the key or raw upstream response

## Required setup
1. Add Actions secret `SUB2_API_KEY`
2. Enable GitHub Pages from `main` branch and `/docs` folder
3. Run workflow `Publish balance JSON` once

## KWGT formulas
- Balance: `$wg("https://anhphuocchu2999-cloud.github.io/api-balance-monitor/balance.json", json, .display_balance)$`
- Detail: `$wg("https://anhphuocchu2999-cloud.github.io/api-balance-monitor/balance.json", json, .display_detail)$`
- Update: `Updated $wg("https://anhphuocchu2999-cloud.github.io/api-balance-monitor/balance.json", json, .updated_at)$`
- Progress: `$wg("https://anhphuocchu2999-cloud.github.io/api-balance-monitor/balance.json", json, .progress_pct)$`
