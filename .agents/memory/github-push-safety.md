---
name: GitHub push safety
description: Safe credential handling for GitHub pushes when source-control OAuth is unavailable.
---

Use the secure secrets flow for a replacement GitHub token and pass it only through a temporary, deleted askpass process when the runtime actually exposes the value to that flow. Never embed tokens in remote URLs, shell history, project files, logs, or chat.

**Why:** A token pasted into a chat or remote URL is exposed and must be revoked, even if the push succeeds.

**How to apply:** Prefer the connected GitHub integration. Replit's secret request may confirm existence without exposing the value to shell/Git; in that case do not retry direct Git auth. Ask the user to connect GitHub or push manually, remove any temporary askpass file, and verify the remote commit without printing credentials.