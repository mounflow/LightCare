# 轻养 LightCare · API 速查表

> 全部接口前缀 `/v1`。鉴权头：`X-LightCare-User-Id: <userId>`（P3 占位；P3 收尾切 JWT）。

## Auth

| Method | Path | Body | 200 data |
|---|---|---|---|
| POST | `/auth/sms-code` | `{phone}` | `{sent: true}` |
| POST | `/auth/login` | `{phone, code}` | `{userId, defaultProfileId, nickname, token}` |

## Profile

| Method | Path | 说明 |
|---|---|---|
| GET | `/profiles` | 当前用户所有档案 |
| POST | `/profiles` | 新建档案（≤ 4 份），`{displayName, relation, birthDate, gender}` |
| PATCH | `/profiles/{id}/goals` | 修改目标值（蛋白/蔬菜/水分/步数/热量） |

## Meal

| Method | Path | 说明 |
|---|---|---|
| GET | `/meals?profileId=&date=YYYY-MM-DD` | 当日所有记录 |
| POST | `/meals` | 写一餐；可传 `foodKeys` 让服务端用 AI 估算营养 |
| DELETE | `/meals/{id}` | 删一餐 |

## Exercise

| Method | Path | 说明 |
|---|---|---|
| POST | `/exercises` | 写一次运动 |
| POST | `/exercises/water` | 喝水打卡（每杯 200ml） |
| GET | `/exercises/today?profileId=` | 当日汇总（占位，P5 收尾填齐） |

## Recommend

| Method | Path | 说明 |
|---|---|---|
| GET | `/recommend/today?profileId=` | 基于过去 3 天缺口生成饮食卡 |
| POST | `/recommend/exercise` | `{profileId, fatigue, stepsToday}` 运动卡 |
| POST | `/recommend/{id}/accept` | 用户接受 |
| POST | `/recommend/{id}/skip` | 用户跳过 |

## Report

| Method | Path | 说明 |
|---|---|---|
| GET | `/reports/weekly?profileId=` | 周报（7 天营养达成率 + 鼓励 + 亮点） |

## 错误码

`code: 0` = ok；其他见 `com/lightcare/server/common/ApiError.java`。
