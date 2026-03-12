# Aham — Play Store Release Setup

One-time checklist to get the CI/CD pipeline fully operational.
Work through these steps in order — each step depends on the previous one.

---

## Step 1 — Create a Release Keystore

### What it is
A keystore is a password-protected file that holds your private signing key.
Google Play requires every AAB to be signed with the **same key for the lifetime
of the app**. If you lose this file you can never publish an update — treat it
like a master password.

### Where to store it
**Outside the repo.** The `.gitignore` already blocks `*.jks` but keeping it
in a separate folder (e.g. `~/keys/`) eliminates any risk of accidental commits.
Back it up to a secure cloud drive (iCloud / Google Drive with encryption).

### Commands

```bash
# 1. Create a dedicated folder outside the project
mkdir -p ~/keys

# 2. Generate the keystore
keytool -genkey -v \
  -keystore ~/keys/aham-release.jks \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias aham-key
```

### Prompts and what to enter

```
Enter keystore password:          ← strong password, e.g. from a password manager
Re-enter new password:            ← same
What is your first and last name? ← your name or organisation name
What is your organisational unit? ← can leave blank, press Enter
What is your organisation name?   ← e.g. "Sri Dev"
What is your city or locality?    ← your city
What is your state or province?   ← your state / province
What is your two-letter country code? ← IN, US, GB, etc.
```

### Values to note down (needed in Step 3)

| Value | GitHub Secret name |
|---|---|
| Keystore password you chose | `KEYSTORE_PASSWORD` |
| Alias you used (`aham-key`) | `KEY_ALIAS` |
| Key password (same as above unless you set it separately) | `KEY_PASSWORD` |

### Encode the keystore file for GitHub

```bash
# macOS — output is copied straight to clipboard
base64 -i ~/keys/aham-release.jks | pbcopy

# Linux
base64 ~/keys/aham-release.jks | xclip -selection clipboard
```

Keep the clipboard value — you will paste it as `KEYSTORE_BASE64` in Step 3.

### Configure local signing (optional, for building release locally)

```bash
cp keystore.properties.template keystore.properties
```

Edit `keystore.properties`:
```properties
storeFile=../../keys/aham-release.jks   # path relative to the app/ folder
storePassword=<your password>
keyAlias=aham-key
keyPassword=<your password>
```

`keystore.properties` is gitignored — it will never be committed.

- [ ] Keystore file created at `~/keys/aham-release.jks`
- [ ] Keystore backed up to secure cloud storage
- [ ] Passwords saved in a password manager
- [ ] `keystore.properties` filled in locally (optional)

---

## Step 2 — Create a Play Store Service Account

### What it is
A service account is a bot Google identity that the GitHub Actions pipeline uses
to authenticate with the Play Store API. It lets Fastlane upload AABs
programmatically without you being logged in interactively.
The account has narrowly scoped permissions (Release Manager only) so it cannot
accidentally publish or delete anything critical.

### Part A — Link Play Console to Google Cloud

1. Open [Play Console](https://play.google.com/console)
2. Select your app → **Setup** → **API access**
3. Click **Link to a Google Cloud project**
   - Use the automatically suggested project (same org as your Play account)
4. Click **Learn how to create service accounts** — this opens Google Cloud Console

### Part B — Create the service account in Google Cloud Console

1. Navigate to **IAM & Admin** → **Service Accounts**
2. Click **Create Service Account**
3. Fill in:
   - **Name:** `aham-fastlane`
   - **ID:** auto-filled, e.g. `aham-fastlane@your-project.iam.gserviceaccount.com`
   - **Description:** `Fastlane CI upload account for Aham`
4. Click **Done** (skip the optional role step — permissions are set in Play Console)
5. Click the newly created service account row
6. Go to the **Keys** tab → **Add Key** → **Create new key** → choose **JSON**
7. A `.json` file downloads automatically — save it as `~/keys/play-store-key.json`

### Part C — Grant permissions in Play Console

1. Return to Play Console → **Setup** → **API access**
2. Find `aham-fastlane` in the service accounts list → click **Grant access**
3. Set the role to **Release Manager**
   - Can upload and manage releases
   - Cannot publish directly to production (you keep that control)
4. Click **Apply**

### Encode the JSON file for GitHub

```bash
# macOS
base64 -i ~/keys/play-store-key.json | pbcopy

# Linux
base64 ~/keys/play-store-key.json | xclip -selection clipboard
```

Keep the clipboard value — you will paste it as `PLAY_STORE_JSON_KEY_BASE64` in Step 3.

- [ ] Play Console linked to Google Cloud project
- [ ] Service account `aham-fastlane` created in Cloud Console
- [ ] JSON key downloaded and saved to `~/keys/play-store-key.json`
- [ ] Service account granted **Release Manager** role in Play Console

---

## Step 3 — Add GitHub Secrets

### What they are
GitHub Secrets are encrypted variables stored in the repository settings.
The deploy workflow reads them at runtime and writes them to temporary files
on the CI runner. They are:
- Never visible in workflow logs (masked as `***`)
- Never accessible to anyone who forks the repo
- Deleted from the runner filesystem when the job finishes

### Where to add them

```
GitHub → your repo → Settings → Secrets and variables → Actions → New repository secret
```

### The 5 secrets

| Secret name | Value | Source |
|---|---|---|
| `KEYSTORE_BASE64` | Base64 output from `base64 -i ~/keys/aham-release.jks` | Step 1 |
| `KEYSTORE_PASSWORD` | Keystore store password | Step 1 |
| `KEY_ALIAS` | `aham-key` (or the alias you chose) | Step 1 |
| `KEY_PASSWORD` | Key password (often same as store password) | Step 1 |
| `PLAY_STORE_JSON_KEY_BASE64` | Base64 output from `base64 -i ~/keys/play-store-key.json` | Step 2 |

### How secrets flow through the pipeline

```
GitHub Secret (AES-256 encrypted at rest)
        │
        │  decoded by the deploy workflow at runtime
        ▼
  Temp file on CI runner  (e.g. /tmp/runner-xyz/keystore.jks)
        │
        │  path passed as environment variable to Gradle / Fastlane
        ▼
  Gradle signs the AAB  ──►  Fastlane uploads to Play Store
        │
        │  runner VM is destroyed after job completes
        ▼
  No sensitive data persists anywhere
```

- [ ] `KEYSTORE_BASE64` added
- [ ] `KEYSTORE_PASSWORD` added
- [ ] `KEY_ALIAS` added
- [ ] `KEY_PASSWORD` added
- [ ] `PLAY_STORE_JSON_KEY_BASE64` added

---

## Step 4 — First Manual Upload (one-time Play Console requirement)

### Why this is needed
Google Play requires the **very first build** to be uploaded manually through
the Play Console UI. After that, all subsequent uploads can be automated via
the API (i.e. Fastlane).

### Steps

1. Build a release AAB locally:
   ```bash
   ./gradlew bundleRelease
   # Output: app/build/outputs/bundle/release/app-release.aab
   ```
2. Open Play Console → your app → **Testing** → **Internal testing** → **Create new release**
3. Upload `app-release.aab`
4. Fill in release notes, click **Save** and **Review release**
5. Submit for review

After this first manual upload, the pipeline takes over for all future releases.

- [ ] First AAB built locally with `./gradlew bundleRelease`
- [ ] First release uploaded manually via Play Console internal testing
- [ ] App reviewed and available to internal testers

---

## Step 5 — Verify the Pipeline End-to-End

Once Steps 1–4 are done, validate that everything works:

```bash
# 1. Make sure you are on main and up to date
git checkout main && git pull

# 2. Push your first automated release tag
git tag v1.0.0
git push origin v1.0.0

# 3. Watch the workflow
#    GitHub → your repo → Actions → "Deploy to Play Store"
#    It should complete in ~5–10 minutes
```

Expected outcome:
- ✅ Workflow completes without errors
- ✅ AAB artifact appears under the Actions run (`release-aab-1.0.0`)
- ✅ New release visible in Play Console → Internal testing

- [ ] `v1.0.0` tag pushed
- [ ] Deploy workflow completed successfully in GitHub Actions
- [ ] Release visible in Play Console internal track

---

## Daily Release Workflow (after setup is complete)

```bash
# Develop normally on the develop branch
git checkout develop
# ... write code, commit, push ...
git push origin develop

# Open a PR from develop → main on GitHub
# CI runs automatically (tests + debug build must pass)
# Merge the PR when ready

# When you want to cut a release — tag main and push
git checkout main && git pull
git tag v1.1.0                  # bump version as appropriate
git push origin v1.1.0          # ← this is the only deploy trigger

# To promote from internal testing to production
bundle exec fastlane promote    # internal → production
# or use the Play Console UI for a gradual rollout (recommended)
```

---

## Reference — Key Files

| File | Purpose |
|---|---|
| `~/keys/aham-release.jks` | Release keystore — **never commit** |
| `~/keys/play-store-key.json` | Service account JSON — **never commit** |
| `keystore.properties` | Local signing config — gitignored |
| `keystore.properties.template` | Template to copy from — committed |
| `fastlane/Fastfile` | Lane definitions (deploy, promote, test) |
| `fastlane/Appfile` | Package name + credentials reference |
| `.github/workflows/ci.yml` | PR check (tests + debug build) |
| `.github/workflows/deploy.yml` | Tag-triggered release deploy |
