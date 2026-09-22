---
name: GitHub remote authentication
description: Durable guidance for pushing this project from the Replit workspace.
---

The GitHub remote for this project is named `github`, not `origin`. When a normal noninteractive push is rejected because the HTTPS remote has no usable credential helper, use the managed GitHub personal access token through a temporary askpass process; never print or persist the token.

**Why:** The remote name and credential behavior are workspace-specific and are not obvious from the application code. A normal `git push origin` is misleading here, while a token in a command URL would risk exposing the secret.

**How to apply:** Inspect `git remote -v` before pushing, use the secret-backed askpass flow only for the push command, and verify the branch is no longer ahead afterward.