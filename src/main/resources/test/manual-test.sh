#!/bin/bash
# =============================================================================
# Manual SSE Notification Test Script
# =============================================================================
# Prerequisites:
#   - App running on localhost:8080  (./mvnw spring-boot:run)
#   - MySQL running with rtns_db database created
#   - Two terminal windows open
#
# User IDs used:
#   - User 1 (task creator) : userId = 1
#   - User 2 (task assignee): userId = 2
# =============================================================================


# -----------------------------------------------------------------------------
# SCENARIO 1: User 2 is ONLINE
# Run these commands in order, each in the terminal indicated.
# -----------------------------------------------------------------------------

# [Terminal 1] Open User 2's SSE stream — keep this running and watch for events
curl -N "http://localhost:8080/notifications/stream?userId=2"

# [Terminal 2] User 1 creates a task for User 2
curl -X POST http://localhost:8080/tasks \
  -H "Content-Type: application/json" \
  -d '{"title":"Fix login bug","description":"Users cant login on mobile","assignedToUserId":2,"createdByUserId":1}'

# Expected result in Terminal 1:
#   event:notification
#   data:{"taskId":1,"title":"Fix login bug",...}


# -----------------------------------------------------------------------------
# SCENARIO 2: User 2 is OFFLINE
# Do NOT run the curl stream command. User 2 has no open connection.
# -----------------------------------------------------------------------------

# [Terminal 2] User 1 creates a task while User 2 is offline
curl -X POST http://localhost:8080/tasks \
  -H "Content-Type: application/json" \
  -d '{"title":"Deploy hotfix","description":"Must go out today","assignedToUserId":2,"createdByUserId":1}'

# [Terminal 2] Verify notification is saved as PENDING in MySQL
mysql -u root -p rtns_db -e "SELECT id, recipient_user_id, status, payload FROM notifications;"

# [Terminal 1] User 2 comes back online — notification should arrive immediately on connect
curl -N "http://localhost:8080/notifications/stream?userId=2"

# [Terminal 2] Verify notification is now marked DELIVERED
mysql -u root -p rtns_db -e "SELECT id, recipient_user_id, status, delivered_at FROM notifications;"


# -----------------------------------------------------------------------------
# TIPS
# -----------------------------------------------------------------------------
# - curl -N disables output buffering so SSE events print in real time
# - Press Ctrl+C in Terminal 1 to simulate User 2 going offline
# - To reset between test runs, clear the notifications table:
#     mysql -u root -p rtns_db -e "TRUNCATE TABLE notifications; TRUNCATE TABLE tasks;"
