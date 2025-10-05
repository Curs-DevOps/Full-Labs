
# 🧪 Laboratory 1 – Introduction to Containers

## 🎯 Objective
This first laboratory introduces **containerization** — one of the key building blocks of DevOps and Cloud Computing.  
You’ll learn what containers are, why they’re useful, and how to set up a local environment using **Docker Hub** or **Podman**.

---

## 🧱 What Are Containers?

Containers package an application together with its dependencies (libraries, runtime, configuration files) into a **single isolated unit**.  
They are lightweight, portable, and consistent across environments — *“it runs on my machine”* becomes *“it runs anywhere.”*

| Traditional Virtual Machine | Container |
|-----------------------------|------------|
| Runs full guest OS | Shares host OS kernel |
| Slower to start | Starts in seconds |
| Heavier (GBs) | Lightweight (MBs) |
| Requires hypervisor | Uses container runtime |

![Containers vs VMs diagram](https://www.docker.com/wp-content/uploads/2022/03/docker-containerized-applications-architecture.svg)

---

## 🐳 Option 1 – Using Docker Hub (+ Docker Desktop)

Docker is the most popular container engine. It’s available for **Windows**, **macOS**, and **Linux**.

<img src="https://1000logos.net/wp-content/uploads/2021/11/Docker-Logo-2013.png" alt="Docker logo" width="250"/>

### 🔹 Step 1 – Create a Docker Hub Account
1. Go to [https://hub.docker.com/](https://hub.docker.com/)
2. Click **Sign Up** and create a free account.
3. Verify your email and log in.

### 🔹 Step 2 – Install Docker Desktop

#### 🪟 Windows
1. Download **Docker Desktop for Windows**  
   👉 [https://docs.docker.com/desktop/install/windows-install/](https://docs.docker.com/desktop/install/windows-install/)
2. Run the installer and keep **WSL 2** enabled when prompted.
3. After installation, open PowerShell and verify:
   ```powershell
   docker version
   docker run hello-world
   ```
4. You should see a message:  
   `Hello from Docker!`

#### 🍎 macOS
1. Download **Docker Desktop for Mac**  
   👉 [https://docs.docker.com/desktop/install/mac-install/](https://docs.docker.com/desktop/install/mac-install/)
2. Drag **Docker.app** to **Applications**.
3. Start Docker from Launchpad → verify:
   ```bash
   docker version
   docker run hello-world
   ```

#### 🐧 Linux
1. Follow the official guide for your distro:  
   👉 [https://docs.docker.com/engine/install/](https://docs.docker.com/engine/install/)
2. Add your user to the Docker group (so you can run Docker without `sudo`):
   ```bash
   sudo usermod -aG docker $USER
   newgrp docker
   ```
3. Verify:
   ```bash
   docker run hello-world
   ```

### 🔹 Step 3 – Log in to Docker Hub
```bash
docker login
```
Enter your Docker Hub username and password.

---

## 🦭 Option 2 – Using Podman (Docker-compatible alternative)

[Podman](https://podman.io/) is a daemonless, open-source container engine that’s fully compatible with Docker CLI commands.

| Feature | Docker | Podman |
|----------|---------|--------|
| Requires background service | ✅ Yes | ❌ No |
| Rootless containers | ⚙️ Optional | ✅ Default |
| Compatible with Docker CLI | ✅ | ✅ (alias) |

  <img src="https://raw.githubusercontent.com/containers/common/main/logos/podman-logo-full-vert.png" alt="Podman logo" width="220"/>

---

### 🔹 Installation – Windows
1. Download the official MSI installer or use Winget:
   ```powershell
   winget install -e --id RedHat.Podman
   ```
2. Verify installation:
   ```powershell
   podman --version
   ```
3. Enable Docker-CLI compatibility:
   ```powershell
   podman machine init
   podman machine start
   ```

---

### 🔹 Installation – macOS
1. Use Homebrew:
   ```bash
   brew install podman
   ```
2. Initialize and start the virtual machine:
   ```bash
   podman machine init
   podman machine start
   ```
3. Test:
   ```bash
   podman run hello-world
   ```

---

### 🔹 Installation – Linux
Podman is included in most distributions.

**Debian / Ubuntu**
```bash
sudo apt update
sudo apt install podman -y
```

**Fedora / RHEL**
```bash
sudo dnf install podman -y
```

**Arch / Manjaro**
```bash
sudo pacman -S podman
```

Verify:
```bash
podman run hello-world
```

---

## ⚙️ Verify Your Setup

Run this simple Nginx container to test networking and port mapping:

```bash
docker run -d -p 9000:80 nginx
# or using podman
podman run -d -p 9000:80 nginx
```

Then open your browser at:

👉 [http://localhost:9000](http://localhost:9000)

You should see the **Nginx welcome page**.

![Nginx welcome screen](https://upload.wikimedia.org/wikipedia/commons/c/c5/Nginx_default_index_page.png)

---

## 🧭 Lab Work

Now that your environment is ready, it’s time to experiment!

### 🧩 Task
Pull and run a different container image from Docker Hub — for example, **httpd (Apache Web Server)**.

### 🔹 Step 1 – Find an Image
1. Go to [https://hub.docker.com/](https://hub.docker.com/)
2. Search for **httpd** (the official Apache image).  
   You’ll find it here: [https://hub.docker.com/_/httpd](https://hub.docker.com/_/httpd)

### 🔹 Step 2 – Pull and Run It
Run the following in your terminal:
```bash
docker pull httpd
docker run -d -p 8080:80 httpd
```
Or, if you use Podman:
```bash
podman pull httpd
podman run -d -p 8080:80 httpd
```

### 🔹 Step 3 – Test It
Open your browser and visit:
👉 [http://localhost:8080](http://localhost:8080)

You should see the **Apache HTTP Server default page**.

### 🧠 Optional Challenge
Try running a different image such as:
- `mysql`
- `redis`
- `python`
- `alpine`

Use `docker run -it image_name /bin/sh` to open an interactive shell inside the container.

---

## 🏁 Summary

✅ You now have a working container runtime (Docker or Podman).  
✅ You can pull images, run containers, and expose them to your browser.  
✅ You’ve run your first test using a real-world web server. 

---

## 📚 Homework

No homework is assigned for this lab.  
Make sure your container environment (Docker or Podman) is working correctly, as it will be required for the next sessions.

