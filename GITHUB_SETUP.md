# GitHub Repository Setup Guide

This guide will help you set up the repository and CI/CD pipeline for automatic Docker image builds.

## Repository Information

- **GitHub Repository**: `https://github.com/igorkoishman/telegram`
- **Docker Hub Username**: `igorkoishman`
- **Docker Images will be**:
  - `igorkoishman/telegram-translator:latest-gpu`
  - `igorkoishman/telegram-translator:latest-cpu`

---

## Step 1: Configure GitHub Secrets

The CI/CD pipeline needs Docker Hub credentials to push images automatically.

### 1.1 Create Docker Hub Access Token

1. Go to [Docker Hub](https://hub.docker.com)
2. Click on your profile → Account Settings
3. Go to **Security** → **New Access Token**
4. Name it: `github-actions-telegram-bot`
5. Set permissions: **Read, Write, Delete**
6. Click **Generate**
7. **Copy the token** (you won't see it again!)

### 1.2 Add Secrets to GitHub

1. Go to your repository: `https://github.com/igorkoishman/telegram`
2. Click **Settings** tab
3. In left sidebar: **Secrets and variables** → **Actions**
4. Click **New repository secret**

Add these two secrets:

**Secret 1:**
- Name: `DOCKER_USERNAME`
- Value: `igorkoishman`

**Secret 2:**
- Name: `DOCKER_PASSWORD`
- Value: `<paste the token you copied>`

---

## Step 2: Push Your Code to GitHub

### If starting fresh:

```bash
cd /Users/ikoishman/IdeaProjects/telegram

# Initialize git (if not already done)
git init

# Add all files
git add .

# Create initial commit
git commit -m "Initial commit: Spring Boot Telegram Video Translation Bot

- Spring Boot 2.7.18 with Java 11
- Telegram bot integration
- Whisper AI transcription (faster-whisper & openai-whisper)
- Multi-language translation (M2M100 & NLLB)
- Subtitle alignment with WhisperX
- Docker support (GPU & CPU)
- CI/CD with GitHub Actions"

# Add remote (already configured)
git remote add origin https://github.com/igorkoishman/telegram.git

# Push to GitHub
git push -u origin main
```

### If you already have commits:

```bash
cd /Users/ikoishman/IdeaProjects/telegram

# Stage new Docker files
git add Dockerfile* docker-compose* .dockerignore .github/ .env.example
git add DOCKER_DEPLOYMENT.md BUILD_AND_RUN.md GITHUB_SETUP.md

# Commit
git commit -m "Add Docker support and CI/CD pipeline

- Added Dockerfile for GPU and CPU variants
- Added docker-compose configurations
- Added GitHub Actions workflow for automated builds
- Added comprehensive deployment documentation"

# Push to GitHub
git push origin main
```

---

## Step 3: Verify CI/CD Pipeline

### 3.1 Check GitHub Actions

1. Go to your repository
2. Click **Actions** tab
3. You should see the "CI/CD Pipeline" workflow running
4. It will:
   - ✅ Build and test Java code
   - ✅ Build Docker images (GPU & CPU)
   - ✅ Push images to Docker Hub

### 3.2 Monitor Build Progress

Click on the running workflow to see:
- Build logs
- Test results
- Docker build progress

First build takes ~10-15 minutes (large AI model base images).

---

## Step 4: Verify Docker Images

After the workflow completes:

1. Go to [Docker Hub](https://hub.docker.com)
2. Navigate to your repositories
3. You should see: `igorkoishman/telegram-translator`
4. Check tags:
   - `latest-gpu`
   - `latest-cpu`
   - `main-gpu`
   - `main-cpu`

---

## Step 5: Create First Release (Optional)

### 5.1 Update Version in pom.xml

Edit `pom.xml` and set version:

```xml
<version>1.0.0</version>
```

### 5.2 Commit and Push

```bash
git add pom.xml
git commit -m "Bump version to 1.0.0"
git push origin main
```

### 5.3 Create GitHub Release

#### Option A: Via GitHub UI

1. Go to repository → **Releases**
2. Click **Create a new release**
3. Click **Choose a tag** → type `v1.0.0` → Create new tag
4. Release title: `v1.0.0 - Initial Release`
5. Auto-generate release notes or write your own
6. Click **Publish release**

#### Option B: Via Command Line

```bash
# Create and push tag
git tag -a v1.0.0 -m "Release v1.0.0 - Initial Production Release"
git push origin v1.0.0

# Create release via GitHub CLI (if installed)
gh release create v1.0.0 \
  --title "v1.0.0 - Initial Release" \
  --notes "First production release with full Docker support" \
  --latest
```

This will trigger the workflow to:
- Build versioned Docker images: `v1.0.0-gpu` and `v1.0.0-cpu`
- Create GitHub release with JAR file attached

---

## Workflow Behavior

### On Merged Pull Request to `main`:

```
PR merged → GitHub Actions triggers:
1. Build and test Java code
2. Determine version bump based on PR labels:
   - Label "major" → v1.0.0 → v2.0.0
   - Label "minor" → v1.0.0 → v1.1.0
   - Label "patch" or no label → v1.0.0 → v1.0.1
3. Create git tag automatically (e.g., v1.2.3)
4. Build Docker images (GPU + CPU)
5. Tag images as:
   - igorkoishman/telegram-translator:v1.2.3-gpu
   - igorkoishman/telegram-translator:v1.2.3-cpu
   - igorkoishman/telegram-translator:latest-gpu
   - igorkoishman/telegram-translator:latest-cpu
6. Push to Docker Hub
7. Create GitHub Release with JAR artifact
```

### Using PR Labels for Versioning:

When creating a PR, add one of these labels to control versioning:
- **major** - Breaking changes (1.0.0 → 2.0.0)
- **minor** - New features (1.0.0 → 1.1.0)
- **patch** - Bug fixes (1.0.0 → 1.0.1, default if no label)

Example:
```bash
# Create PR
gh pr create --title "Add new translation model" --label minor

# Or add label to existing PR
gh pr edit 123 --add-label minor
```

---

## Using Your Docker Images

### Pull and Run GPU Version

```bash
docker pull igorkoishman/telegram-translator:latest-gpu

docker run -d \
  --gpus all \
  -p 8080:8080 \
  -e TELEGRAM_BOT_TOKEN=your_token \
  -v telegram-models:/app/models \
  igorkoishman/telegram-translator:latest-gpu
```

### Pull and Run CPU Version

```bash
docker pull igorkoishman/telegram-translator:latest-cpu

docker run -d \
  -p 8080:8080 \
  -e TELEGRAM_BOT_TOKEN=your_token \
  -v telegram-models:/app/models \
  igorkoishman/telegram-translator:latest-cpu
```

---

## Branch Strategy

Recommended Git workflow:

### Main Branch (Production)

```
main → Stable production code
     → Auto-builds and pushes Docker images
     → Tagged as 'latest'
```

### Develop Branch (Staging)

```bash
# Create develop branch
git checkout -b develop
git push -u origin develop

# Now pushes to develop will build images tagged as 'develop-*'
```

### Feature Branches

```bash
# Create feature branch
git checkout -b feature/add-new-translation-model

# Make changes...
git add .
git commit -m "Add NLLB translation model"
git push -u origin feature/add-new-translation-model

# Create PR on GitHub
# After approval → Auto-merges and builds
```

---

## Troubleshooting

### Build Fails on GitHub Actions

1. Check Actions tab for error logs
2. Common issues:
   - Docker Hub credentials incorrect
   - Build timeout (increase in workflow file)
   - Out of disk space (GitHub runners have limits)

### Docker Images Not Appearing on Docker Hub

1. Verify secrets are set correctly: `DOCKER_USERNAME` and `DOCKER_PASSWORD`
2. Check workflow logs for authentication errors
3. Ensure Docker Hub repository exists (auto-created on first push)

### Can't Push to GitHub

```bash
# If authentication fails, use personal access token
git remote set-url origin https://YOUR_GITHUB_TOKEN@github.com/igorkoishman/telegram.git

# Or use SSH
git remote set-url origin git@github.com:igorkoishman/telegram.git
```

---

## Next Steps

1. ✅ Push code to GitHub
2. ✅ Add Docker Hub secrets
3. ✅ Verify first build completes
4. ✅ Test pulling and running Docker image
5. ✅ Create first release tag
6. Deploy to your Windows/Linux machine with GPU support!

---

## Resources

- **GitHub Repository**: https://github.com/igorkoishman/telegram
- **Docker Hub**: https://hub.docker.com/r/igorkoishman/telegram-translator
- **GitHub Actions**: https://github.com/igorkoishman/telegram/actions
- **Deployment Guide**: [DOCKER_DEPLOYMENT.md](DOCKER_DEPLOYMENT.md)
- **Quick Start**: [BUILD_AND_RUN.md](BUILD_AND_RUN.md)

---

All set! Once you push the code and configure the secrets, the CI/CD pipeline will automatically build and publish your Docker images. 🚀
