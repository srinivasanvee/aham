You are helping cut a production release for the Aham Android app.
The CI/CD pipeline is triggered exclusively by pushing a `v*` tag to `main`.
`versionCode` is set automatically from `GITHUB_RUN_NUMBER`; `versionName` is derived from the tag (strip the leading `v`).

## Steps to follow — in order

### 1. Check for uncommitted changes
- Run `git status --porcelain` to detect any modified, staged, or untracked files
- If there are changes:
  - Show the user a `git status` summary and `git diff --stat`
  - Ask for a commit message (or suggest one based on the changed files)
  - Stage all changes: `git add -A`
  - Commit: `git commit -m "<message>"`
- If there are no changes, skip this step

### 2. Verify preconditions
Run these checks and **stop with a clear error** if any fail:
- `git branch --show-current` → must be `main`
- `git log origin/main..HEAD --oneline` → show any local commits not yet pushed (informational, not a blocker)

### 3. Determine the new version tag
- Run `git tag --list 'v*' --sort=-version:refname | head -5` to show recent tags
- If `$ARGUMENTS` is provided (e.g. `/prod-push v1.2.0` or `/prod-push patch`):
  - If it starts with `v` and matches `vMAJOR.MINOR.PATCH`, use it directly
  - If it is `patch`, `minor`, or `major`, auto-increment the latest tag accordingly
- If no argument is given, show the 5 most recent tags and ask the user what the new version should be before continuing

### 4. Confirm before acting
Show a full summary of everything that will happen:

```
Ready to release:
  Branch  : main
  Commits : <number of unpushed commits including any just made>
  New tag : v<VERSION>
  Actions :
    git push origin main
    git tag v<VERSION>
    git push origin v<VERSION>
```

Ask the user to confirm ("Proceed? y/n") before running any push commands.

### 5. Push, tag, and push tag
After confirmation:
```bash
git push origin main
git tag v<VERSION>
git push origin v<VERSION>
```

### 6. Report outcome
- Print the tag that was created and pushed
- Remind the user to watch the deploy workflow:
  `GitHub → repo → Actions → "Deploy to Play Store"`
  Expected completion: ~5–10 minutes
- If the push fails, show the full error and suggest fixes (e.g. branch not up to date with remote)
