# 운영 명령어 별칭 가이드 (Ops Aliases)

반복 입력되는 긴 명령어를 alias로 축약해 운영 속도와 정확도를 높입니다.

## 문서 메타정보
| 항목 | 값 |
|---|---|
| 문서 버전 | v1.0.0 |
| 최종 수정일 | 2026-03-05 |
| 수정자 | ops-admin |

## 빠른 검색 키워드
`alias`, `kubectl-shortcuts`, `docker-shortcuts`, `ops-productivity`, `command-shortcuts`, `shell-profile`, `bashrc`, `zshrc`

---

## 1) 추천 alias 목록
```bash
# 공통
alias k='kubectl'
alias dc='docker compose'
alias dps='docker ps --format "table {{.Names}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}"'

# Kubernetes (thock-prod 고정)
alias kns='kubectl -n thock-prod'
alias kpods='kubectl -n thock-prod get pods -o wide'
alias kevents='kubectl -n thock-prod get events --sort-by=.lastTimestamp | tail -n 50'
alias ktop='kubectl -n thock-prod top pod'
alias kroll='kubectl -n thock-prod rollout status'
alias klogs-gw='kubectl -n thock-prod logs deploy/api-gateway --tail=200'
alias king='kubectl -n thock-prod get ingress'

# 배포/이미지 확인
alias kimg='kubectl -n thock-prod get deploy api-gateway member-service product-service market-service payment-service settlement-service -o=jsonpath='"'"'{range .items[*]}{.metadata.name}{" => "}{.spec.template.spec.containers[0].image}{"\n"}{end}'"'"''

# Compose 점검
alias dcps='docker compose ps'
alias dclogs='docker compose logs --tail=200'
```

---

## 2) 적용 방법

### 2-1. Bash
```bash
vi ~/.bashrc
# 위 alias 블록 추가
source ~/.bashrc
```

### 2-2. Zsh
```bash
vi ~/.zshrc
# 위 alias 블록 추가
source ~/.zshrc
```

### 2-3. 적용 확인
```bash
alias | grep -E '^alias (k=|kns=|kpods=|kevents=|ktop=|kimg=|dc=|dcps=)'
```

---

## 3) 사용 예시
```bash
# Pod 상태 확인
kpods

# 최근 이벤트 확인
kevents

# API Gateway 로그 확인
klogs-gw

# 배포 이미지 태그 확인
kimg

# Compose 상태 확인
dcps
```

---

## 4) 운영 주의사항
- `kns` 계열은 `thock-prod` 네임스페이스로 고정됩니다.
- 멀티 네임스페이스 운영 시 alias 오용을 방지하기 위해 `kubectl config current-context`를 먼저 확인합니다.
- 파괴적 명령(`delete`, `scale`, `rollout undo`)은 alias로 축약하지 않는 것을 권장합니다.
- 공용 계정 서버에서는 alias 변경 이력을 `OPS_CHANGELOG.md`에 기록합니다.

---

## 5) 관련 문서 (공통 링크)
- `AWS EC2/README.md`
- `AWS EC2/OPERATIONS_RUNBOOK.md`
- `AWS EC2/OPS_CHECK_TEMPLATE.md`
- `AWS EC2/OPS_CHANGELOG.md`
- `AWS EC2/OPS_ONBOARDING.md`
- `AWS EC2/OPS_ALIASES.md`
- `AWS EC2/OPS_MONITORING_ALERTS.md`
- `AWS EC2/OPS_DB_MIGRATION_GUIDE.md`
- `AWS EC2/OPS_SECURITY_SECRETS_POLICY.md`
- `AWS EC2/OPS_DOCUMENTATION_GUIDE.md`

---

## 개정 이력
| 버전 | 일자 | 수정자 | 변경 요약 |
|---|---|---|---|
| v1.0.0 | 2026-03-05 | ops-admin | 운영 alias 표준 목록 및 적용 가이드 작성 |
| v1.0.1 | 2026-03-05 | ops-admin | 공통 링크에 신규 정책 문서(보안) 반영 |
