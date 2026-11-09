#!/usr/bin/env bash
#
# Deploy yt-proxy to Cloud Run.
#
#   ./yt-proxy/deploy.sh                          # defaults below
#   REGION=europe-west1 ./yt-proxy/deploy.sh      # pick a region
#   MAX_INSTANCES=1 ./yt-proxy/deploy.sh          # tighter cost cap
#   ALLOW_UNAUTH=false ./yt-proxy/deploy.sh       # private service
#
# Requires an authenticated gcloud CLI and a project with billing enabled.
# Nothing here touches the Android app; it prints the one line you paste into
# local.properties afterwards.

set -euo pipefail

SERVICE="${SERVICE:-minefiy-yt}"
REGION="${REGION:-us-central1}"
MEMORY="${MEMORY:-512Mi}"
CPU="${CPU:-1}"
MIN_INSTANCES="${MIN_INSTANCES:-0}"
MAX_INSTANCES="${MAX_INSTANCES:-3}"
CONCURRENCY="${CONCURRENCY:-80}"
TIMEOUT="${TIMEOUT:-60}"
CACHE_TTL_SECONDS="${CACHE_TTL_SECONDS:-600}"
ALLOW_UNAUTH="${ALLOW_UNAUTH:-true}"
SKIP_SMOKE_TEST="${SKIP_SMOKE_TEST:-false}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

info() { printf '\033[1m==>\033[0m %s\n' "$*"; }
warn() { printf '\033[1;33mwarn:\033[0m %s\n' "$*" >&2; }
die()  { printf '\033[1;31merror:\033[0m %s\n' "$*" >&2; exit 1; }

command -v gcloud >/dev/null 2>&1 ||
  die "gcloud not found. Install: https://cloud.google.com/sdk/docs/install"
command -v curl >/dev/null 2>&1 || die "curl not found"

PROJECT_ID="${PROJECT_ID:-$(gcloud config get-value project 2>/dev/null || true)}"
if [ -z "$PROJECT_ID" ] || [ "$PROJECT_ID" = "(unset)" ]; then
  die "no active project. Run 'gcloud config set project <PROJECT_ID>' or export PROJECT_ID=..."
fi

if [ -z "$(gcloud auth list --filter=status:ACTIVE --format='value(account)' 2>/dev/null)" ]; then
  die "no active gcloud account. Run 'gcloud auth login' (or 'gcloud auth activate-service-account ...')."
fi

info "Project: $PROJECT_ID | region: $REGION | service: $SERVICE"

info "Enabling run/cloudbuild APIs (idempotent)"
gcloud services enable \
  run.googleapis.com \
  cloudbuild.googleapis.com \
  --project "$PROJECT_ID" >/dev/null

AUTH_FLAG="--allow-unauthenticated"
if [ "$ALLOW_UNAUTH" != "true" ]; then
  AUTH_FLAG="--no-allow-unauthenticated"
  warn "deploying without public access; callers will need an identity token"
fi

info "Building and deploying from $SCRIPT_DIR"
gcloud run deploy "$SERVICE" \
  --source "$SCRIPT_DIR" \
  --project "$PROJECT_ID" \
  --region "$REGION" \
  --platform managed \
  "$AUTH_FLAG" \
  --memory "$MEMORY" \
  --cpu "$CPU" \
  --min-instances "$MIN_INSTANCES" \
  --max-instances "$MAX_INSTANCES" \
  --concurrency "$CONCURRENCY" \
  --timeout "$TIMEOUT" \
  --set-env-vars "CACHE_TTL_SECONDS=$CACHE_TTL_SECONDS" \
  --quiet

URL="$(gcloud run services describe "$SERVICE" \
  --project "$PROJECT_ID" \
  --region "$REGION" \
  --format='value(status.url)')"
[ -n "$URL" ] || die "deploy finished but the service URL was empty"

if [ "$SKIP_SMOKE_TEST" != "true" ]; then
  info "Smoke testing $URL/health"
  if curl -fsS --max-time 30 "$URL/health" >/dev/null; then
    info "/health OK"
  else
    warn "/health did not return 2xx — check logs (see below)"
  fi

  if [ "$ALLOW_UNAUTH" = "true" ]; then
    info "Smoke testing /search"
    if curl -fsS --max-time 45 "$URL/search?q=coldplay&filter=songs&limit=1" >/dev/null; then
      info "/search OK"
    else
      # A cold start or upstream throttling is common here and is not fatal.
      warn "/search did not return 2xx (upstream throttle or cold start?)"
    fi
  fi
fi

# Wire the URL into the Android build. Only the yt.proxy.base.url line is
# touched, so existing B2 keys / sdk.dir survive.
LOCAL_PROPERTIES="${LOCAL_PROPERTIES:-$SCRIPT_DIR/../local.properties}"
if [ "${WRITE_LOCAL_PROPERTIES:-true}" = "true" ]; then
  touch "$LOCAL_PROPERTIES"
  if grep -qE '^yt\.proxy\.base\.url=' "$LOCAL_PROPERTIES"; then
    sed -i.bak -E "s|^yt\.proxy\.base\.url=.*|yt.proxy.base.url=$URL|" "$LOCAL_PROPERTIES"
    rm -f "$LOCAL_PROPERTIES.bak"
  else
    printf 'yt.proxy.base.url=%s\n' "$URL" >> "$LOCAL_PROPERTIES"
  fi
  info "Wired $LOCAL_PROPERTIES -> yt.proxy.base.url=$URL"
else
  warn "WRITE_LOCAL_PROPERTIES=false: not touching $LOCAL_PROPERTIES"
fi

cat <<EOF

Deployed: $URL

The app build now reads this from local.properties (rebuild to pick it up):

  yt.proxy.base.url=$URL

To build without editing files instead:

  YT_PROXY_BASE_URL=$URL ./gradlew :app:assembleDebug

Logs:
  gcloud logging read 'resource.type=cloud_run_revision AND resource.labels.service_name=$SERVICE' \\
    --project $PROJECT_ID --limit 50

Rollback / cost control:
  gcloud run services update $SERVICE --project $PROJECT_ID --region $REGION --max-instances 1
  gcloud run services delete $SERVICE --project $PROJECT_ID --region $REGION

EOF
