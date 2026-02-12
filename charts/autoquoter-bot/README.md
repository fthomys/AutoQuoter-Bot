# AutoQuoter-Bot Helm Chart

This Helm chart deploys the AutoQuoter-Bot on a Kubernetes cluster.

## Prerequisites

- Kubernetes 1.19+
- Helm 3.2.0+
- A PostgreSQL database

## Installation

### 1. Configure the bot

Edit `values.yaml` or provide a custom values file with the necessary configuration:

```yaml
config:
  token: "YOUR_DISCORD_BOT_TOKEN"
  ownerIds: [123456789]
  databaseConfig:
    serverName: "postgres-service"
    port: 5432
    name: "autoquoter"
    user: "postgres"
    password: "securepassword"
  supportGuildInvite: "https://discord.gg/yourserver"
  joinLeaveLogWebhook: "https://discord.com/api/webhooks/..."
```

### 2. Install the chart

```bash
helm install autoquoter-bot ./charts/autoquoter-bot
```

## Configuration

The following table lists the configurable parameters of the AutoQuoter-Bot chart and their default values.

| Parameter | Description | Default |
|-----------|-------------|---------|
| `replicaCount` | Number of replicas | `1` |
| `image.repository` | Image repository | `ghcr.io/fabichan/autoquoter-bot` |
| `image.tag` | Image tag | `.Chart.AppVersion` |
| `config.token` | Discord Bot Token | `""` |
| `config.databaseConfig.serverName` | PostgreSQL Server Name | `""` |
| `config.databaseConfig.password` | PostgreSQL Password | `""` |
| `resources` | Pod resources | `requests: {cpu: 100m, memory: 256Mi}, limits: {cpu: 500m, memory: 512Mi}` |

## ArgoCD Integration

To use this chart with ArgoCD, you can point your ArgoCD Application to the repository and specify the path to the chart.

Example ArgoCD Application:

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: autoquoter-bot
  namespace: argocd
spec:
  project: default
  source:
    repoURL: https://github.com/FabianThomys/AutoQuoter-Bot.git
    targetRevision: HEAD
    path: charts/autoquoter-bot
    helm:
      values: |
        config:
          token: "secret-token"
          # ... other config
  destination:
    server: https://kubernetes.default.svc
    namespace: autoquoter
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
```
