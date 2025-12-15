# WSL + Ubuntu + Docker (without Docker Desktop)
## Complete Student Guide – Step by Step

## 🎯 Objective
By the end of this lab, you will be able to run Docker containers directly inside Ubuntu (WSL), without Docker Desktop. This setup:
- avoids compatibility issues,
- uses fewer system resources,
- behaves exactly like Docker on a real Linux server.

---

# ⚙️ 1. Enable and Install WSL2

### 1.1 Open PowerShell as Administrator
Start → type **PowerShell** → right-click → *Run as Administrator*

### 1.2 Install WSL
*(Run this in Windows PowerShell)*

```powershell
wsl --install
```

This:
- installs WSL components,
- sets **WSL2** as the default backend,
- installs Ubuntu.

### 1.3 Restart Windows

---

# 🐧 2. Initial Ubuntu Setup in WSL

After restart, Ubuntu will launch and ask for:
- a Linux username,
- a Linux password.

### 2.1 How to open Ubuntu manually
If Ubuntu does not open automatically, run:

```powershell
wsl
```

or

```powershell
ubuntu
```

---

# 🔄 3. Update Ubuntu Packages

*(Run these inside Ubuntu / WSL)*

```bash
sudo apt update
sudo apt upgrade -y
```

---

# 🐳 4. Install Docker Engine in WSL (Ubuntu)

## Make sure you are inside Ubuntu
Correct prompt looks like:

```
student@DESKTOP-ABC:~$
```

If you are still in PowerShell, run:

```powershell
wsl
```

---

## 4.1 Remove any old Docker versions

```bash
sudo apt remove docker docker-engine docker.io containerd runc
```

---

## 4.2 Install required packages

```bash
sudo apt install -y ca-certificates curl gnupg lsb-release
```

---

## 4.3 Add Docker’s official GPG key

```bash
sudo mkdir -m 0755 -p /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
```

---

## 4.4 Add the Docker repository

```bash
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
```

---

## 4.5 Install Docker Engine + Docker Compose plugin

```bash
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
```

---

# 🧩 5. Allow Docker to run without sudo (global access)

```bash
sudo usermod -aG docker $USER
```

Reload the session:

```bash
exit
```

Re-enter WSL:

```powershell
wsl
```

Test:

```bash
docker ps
```

---

# 🧪 6. Test Docker

## 6.1 Basic test

```bash
docker run hello-world
```

---

## 6.2 Test Docker Compose

Create file:

```bash
nano docker-compose.yml
```

Content:

```yaml
version: '3.9'
services:
  test:
    image: nginx
    ports:
      - "8080:80"
```

Run:

```bash
docker compose up
```

Open browser:

```
http://localhost:8080
```

---

# 🧭 7. Where commands run

### Windows PowerShell commands:
- `wsl --install`
- `wsl`
- `ubuntu`
- `wsl --shutdown`

Prompt looks like:

```
PS C:\Users\Student>
```

### Ubuntu (WSL) commands:
- `sudo apt ...`
- `docker ...`
- `docker compose ...`

Prompt looks like:

```
student@DESKTOP:~$
```

---

# 🔧 8. Recommended WSL Resource Configuration

Create the file:

```
C:\Users\<your-username>\.wslconfig
```

Content:

```
[wsl2]
memory=4GB
processors=4
localhostForwarding=true
```

Apply:

```powershell
wsl --shutdown
```

---

# 🧹 9. Optional: uninstall Docker Desktop

Once Docker works in WSL:

Start → Add/Remove Programs → Docker Desktop → Uninstall

---

# 🆘 10. Troubleshooting

### Docker not running
```bash
sudo service docker start
```

### Permission denied
```bash
sudo usermod -aG docker $USER
```

Then:

```powershell
wsl --shutdown
```

### Networking issues
```powershell
wsl --shutdown
```
