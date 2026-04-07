# Golden snapshots

- `home_envelope.golden.json` — normalized `GET /home` envelope (trace/time/ttl zeroed).

Regenerate after intentional composer changes:

```bash
# Windows PowerShell
$env:GENERATE_GOLDEN="1"; go test ./internal/contract -run TestGenerateHomeGolden -v
```

```bash
# Unix
GENERATE_GOLDEN=1 go test ./internal/contract -run TestGenerateHomeGolden -v
```
