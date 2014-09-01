# Send short sync.  2 minute jitter
# Arguments: server admin-secret
curl -H "Authorization: key=$2" -H "Content-Type: application/json" -d '{"sync_jitter":120000}' $1/send/global/sync_schedule