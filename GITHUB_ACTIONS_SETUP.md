# GitHub Actions Setup Guide

## Overview

Your repository has automated CI/CD workflows that build and push Docker images to Docker Hub whenever you merge a Pull Request.

---

## Required Secrets

You need to add these secrets to your GitHub repository for the workflows to work:

### 1. DOCKER_USERNAME
- **Value**: `igorkoishman`
- **Purpose**: Your Docker Hub username for authentication

### 2. DOCKER_PASSWORD
- **Value**: Your Docker Hub access token (NOT your password)
- **Purpose**: Authentication token for pushing images

---

## Step-by-Step Setup

### Step 1: Create Docker Hub Access Token

1. Go to https://hub.docker.com/
2. Log in with your account (`igorkoishman`)
3. Click your **username** (top right) → **Account Settings**
4. Click **Security** in the left sidebar
5. Click **New Access Token**
   - **Description**: `GitHub Actions - telegram project`
   - **Access permissions**: Select **Read, Write, Delete**
6. Click **Generate**
7. **Copy the token** immediately (you'll only see it once!)
   - Looks like: `dckr_pat_xxxxxxxxxxxxxxxxxxxxx`

### Step 2: Add Secrets to GitHub

1. Go to your repository: https://github.com/igorkoishman/telegram
2. Click **Settings** tab
3. In the left sidebar, click **Secrets and variables** → **Actions**
4. Click **New repository secret**

**Add first secret:**
- Name: `DOCKER_USERNAME`
- Secret: `igorkoishman`
- Click **Add secret**

**Add second secret:**
- Name: `DOCKER_PASSWORD`
- Secret: Paste your Docker Hub access token
- Click **Add secret**

### Step 3: Verify Secrets

Go to: https://github.com/igorkoishman/telegram/settings/secrets/actions

You should see:
- ✅ DOCKER_USERNAME
- ✅ DOCKER_PASSWORD

---

## How the Workflows Work

### Auto-Label Workflow (.github/workflows/auto-label.yml)

**Triggers**: When you create a new Pull Request

**What it does**: Automatically adds labels based on branch name:
- `feature/*` or `feat/*` → Adds **`minor`** label (1.0.0 → 1.1.0)
- `breaking/*` or `major/*` → Adds **`major`** label (1.0.0 → 2.0.0)
- Everything else → Adds **`patch`** label (1.0.0 → 1.0.1)

### CI/CD Workflow (.github/workflows/ci-cd.yml)

**Triggers**: When you merge a Pull Request to `main` branch

**What it does**:
1. ✅ Checks out code
2. ✅ Builds Java app with Maven
3. ✅ Runs tests
4. ✅ Determines version bump based on PR label
5. ✅ Creates Git tag (e.g., v1.2.3)
6. ✅ Logs in to Docker Hub
7. ✅ Builds GPU Docker image
8. ✅ Builds CPU Docker image
9. ✅ Pushes images to Docker Hub with tags:
   - `igorkoishman/telegram-translator:v1.2.3-gpu`
   - `igorkoishman/telegram-translator:v1.2.3-cpu`
   - `igorkoishman/telegram-translator:latest-gpu`
   - `igorkoishman/telegram-translator:latest-cpu`

---

## Workflow Examples

### Example 1: Bug Fix (Patch Version)

```bash
# Create branch
git checkout -b fix/translation-bug
git add .
git commit -m "Fix Russian translation bug"
git push origin fix/translation-bug

# Create PR on GitHub
# → Auto-label adds 'patch' label

# Merge PR
# → CI/CD builds and pushes: v1.0.1-gpu, v1.0.1-cpu
```

### Example 2: New Feature (Minor Version)

```bash
# Create branch
git checkout -b feature/dark-mode
git add .
git commit -m "Add dark mode support"
git push origin feature/dark-mode

# Create PR on GitHub
# → Auto-label adds 'minor' label

# Merge PR
# → CI/CD builds and pushes: v1.1.0-gpu, v1.1.0-cpu
```

### Example 3: Breaking Change (Major Version)

```bash
# Create branch
git checkout -b breaking/api-v2
git add .
git commit -m "Refactor API (breaking changes)"
git push origin breaking/api-v2

# Create PR on GitHub with 'major' label
# (manually add it if auto-label doesn't apply)

# Merge PR
# → CI/CD builds and pushes: v2.0.0-gpu, v2.0.0-cpu
```

---

## Checking Workflow Status

### View Workflow Runs

1. Go to: https://github.com/igorkoishman/telegram/actions
2. You'll see all workflow runs
3. Click on a run to see details
4. Green checkmark = Success ✅
5. Red X = Failed ❌

### View Build Logs

1. Click on a workflow run
2. Click on the job (e.g., "build-tag-push")
3. Expand steps to see logs
4. Look for errors if the workflow failed

### Check Docker Hub

After successful workflow:
1. Go to: https://hub.docker.com/r/igorkoishman/telegram-translator
2. Click **Tags** tab
3. You should see your new tags:
   - `v1.2.3-gpu`
   - `v1.2.3-cpu`
   - `latest-gpu`
   - `latest-cpu`

---

## Troubleshooting

### Workflow Fails: "Error: buildx failed"

**Cause**: Docker build failed

**Solution**: Check build logs for compilation errors

### Workflow Fails: "Error: denied: requested access to the resource is denied"

**Cause**: Docker Hub authentication failed

**Solution**:
1. Check that DOCKER_USERNAME and DOCKER_PASSWORD secrets are set correctly
2. Verify Docker Hub token is valid (not expired)
3. Create new token if needed

### Workflow Fails: "Tests failed"

**Cause**: Unit tests didn't pass

**Solution**:
1. Fix test failures locally first
2. Run `mvn test` locally to verify
3. Push fix and try again

### Workflow Doesn't Trigger

**Cause**: Workflow only runs on merged PRs to `main`

**Solution**:
1. Create a Pull Request
2. Merge it to `main` branch
3. Workflow will trigger automatically

---

## Manual Workflow Trigger (Advanced)

If you need to manually trigger a build without merging a PR:

1. Go to: https://github.com/igorkoishman/telegram/actions
2. Select the workflow
3. Click **Run workflow** button
4. Select branch
5. Click **Run workflow**

---

## Branch Naming Conventions

For automatic version bumping, use these branch name patterns:

| Pattern | Label | Version Bump | Example |
|---------|-------|--------------|---------|
| `feature/*` | minor | 1.0.0 → 1.1.0 | `feature/add-dark-mode` |
| `feat/*` | minor | 1.0.0 → 1.1.0 | `feat/new-ui` |
| `breaking/*` | major | 1.0.0 → 2.0.0 | `breaking/api-v2` |
| `major/*` | major | 1.0.0 → 2.0.0 | `major/refactor` |
| `fix/*` | patch | 1.0.0 → 1.0.1 | `fix/bug-123` |
| `bugfix/*` | patch | 1.0.0 → 1.0.1 | `bugfix/crash` |
| `docs/*` | patch | 1.0.0 → 1.0.1 | `docs/update-readme` |

---

## Security Best Practices

1. **Never commit secrets** to your repository
2. **Rotate tokens periodically** (every 6-12 months)
3. **Use least privilege** - Only grant necessary permissions
4. **Review workflow changes** - Check what workflows do before merging
5. **Monitor workflow runs** - Check for suspicious activity

---

## Summary

Once you add the Docker Hub secrets:

1. ✅ Create feature branch
2. ✅ Make changes
3. ✅ Push to GitHub
4. ✅ Create Pull Request
5. ✅ Auto-label applies based on branch name
6. ✅ Merge PR
7. ✅ Workflow automatically:
   - Builds Docker images
   - Tags with new version
   - Pushes to Docker Hub
8. ✅ Images are ready to deploy!

No manual Docker builds needed - everything is automated! 🎉

---

## Next Steps

1. Add `DOCKER_USERNAME` and `DOCKER_PASSWORD` secrets to GitHub
2. Create a test PR to verify the workflow works
3. Check Docker Hub for the published images
4. Deploy the images using `docker pull igorkoishman/telegram-translator:latest-cpu`

---

## Related Documentation

- [README.md](README.md) - Main documentation
- [.github/workflows/ci-cd.yml](.github/workflows/ci-cd.yml) - CI/CD workflow file
- [.github/workflows/auto-label.yml](.github/workflows/auto-label.yml) - Auto-labeling workflow
