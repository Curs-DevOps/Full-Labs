const GATEWAY_URL = "http://localhost:8081/api/gateway/data";
const POLL_INTERVAL_MS = 5000;

let intervalId = null;

// ---------- DOM elements ----------

const connectionStatusEl = document.getElementById("connectionStatus");
const lastUpdateEl = document.getElementById("lastUpdate");

const sensorIdEl = document.getElementById("sensorId");
const sensorStatusEl = document.getElementById("sensorStatus");
const sensorTemperatureEl = document.getElementById("sensorTemperature");
const sensorHumidityEl = document.getElementById("sensorHumidity");
const sensorTimestampEl = document.getElementById("sensorTimestamp");

const analyticsStatusEl = document.getElementById("analyticsStatus");

const avgTemperatureEl = document.getElementById("avgTemperature");
const minTemperatureEl = document.getElementById("minTemperature");
const maxTemperatureEl = document.getElementById("maxTemperature");

const avgHumidityEl = document.getElementById("avgHumidity");
const minHumidityEl = document.getElementById("minHumidity");
const maxHumidityEl = document.getElementById("maxHumidity");

const totalReadingsEl = document.getElementById("totalReadings");

const alertsListEl = document.getElementById("alertsList");
const errorMessageEl = document.getElementById("errorMessage");

const startButton = document.getElementById("startButton");
const stopButton = document.getElementById("stopButton");
const refreshButton = document.getElementById("refreshButton");

// ---------- Helpers ----------

function setConnectionStatus(online, message) {
  const text = message || (online ? "connected" : "disconnected");
  connectionStatusEl.textContent = `Status: ${text}`;
}

function setError(message) {
  errorMessageEl.textContent = message || "None";
}

function formatNumber(value, digits = 2) {
  if (value === null || value === undefined || isNaN(value)) {
    return "-";
  }
  return Number(value).toFixed(digits);
}

// ---------- Update UI from gateway response ----------

function updateDashboard(gatewayResponse) {
  const sensor = gatewayResponse.sensor_data || {};
  const analytics = gatewayResponse.analytics || {};
  const stats = analytics.stats || {};

  // ----- Sensor data -----
  sensorIdEl.textContent = sensor.sensor_id || sensor.sensorId || "-";
  sensorStatusEl.textContent = sensor.status || "-";
  sensorTemperatureEl.textContent = formatNumber(sensor.temperature);
  sensorHumidityEl.textContent = formatNumber(sensor.humidity);
  sensorTimestampEl.textContent = sensor.timestamp || "-";

  // ----- Analytics status -----
  analyticsStatusEl.textContent = analytics.status || "-";

  // ----- Stats -----
  const avgTemp = stats.average_temperature ?? stats.averageTemperature;
  const minTemp = stats.min_temperature ?? stats.minTemperature;
  const maxTemp = stats.max_temperature ?? stats.maxTemperature;

  const avgHum = stats.average_humidity ?? stats.averageHumidity;
  const minHum = stats.min_humidity ?? stats.minHumidity;
  const maxHum = stats.max_humidity ?? stats.maxHumidity;

  const total = stats.total_readings ?? stats.totalReadings;

  avgTemperatureEl.textContent = formatNumber(avgTemp);
  minTemperatureEl.textContent = formatNumber(minTemp);
  maxTemperatureEl.textContent = formatNumber(maxTemp);

  avgHumidityEl.textContent = formatNumber(avgHum);
  minHumidityEl.textContent = formatNumber(minHum);
  maxHumidityEl.textContent = formatNumber(maxHum);

  totalReadingsEl.textContent = total ?? "-";

  // ----- Alerts -----
  alertsListEl.innerHTML = "";
  const alerts = analytics.alerts || [];

  if (alerts.length === 0) {
    const li = document.createElement("li");
    li.textContent = "No active alerts.";
    alertsListEl.appendChild(li);
  } else {
    alerts.forEach((alert) => {
      const li = document.createElement("li");

      const type = alert.type || "ALERT";
      const msg = alert.message || "";

      const sensorId =
        alert.sensor_id ||
        alert.sensorId ||
        sensor.sensor_id ||
        sensor.sensorId ||
        "";

      li.textContent = `[${type}] ${msg}${
        sensorId ? " (sensor " + sensorId + ")" : ""
      }`;

      alertsListEl.appendChild(li);
    });
  }

  // ----- Meta -----
  lastUpdateEl.textContent =
    "Last update: " + new Date().toLocaleTimeString();

  setConnectionStatus(true);
  setError(null);
}

// ---------- Fetch logic ----------

async function fetchGatewayData() {
  try {
    const response = await fetch(GATEWAY_URL);
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }

    const data = await response.json();
    updateDashboard(data);
  } catch (err) {
    console.error("Error fetching gateway data:", err);
    setConnectionStatus(false, "error");
    setError(`Failed to fetch data: ${err.message}`);
  }
}

// ---------- Polling ----------

function startPolling() {
  if (intervalId !== null) return;

  fetchGatewayData();
  intervalId = setInterval(fetchGatewayData, POLL_INTERVAL_MS);

  startButton.disabled = true;
  stopButton.disabled = false;
}

function stopPolling() {
  if (intervalId !== null) {
    clearInterval(intervalId);
    intervalId = null;
  }

  startButton.disabled = false;
  stopButton.disabled = true;
}

// ---------- Events ----------

startButton.addEventListener("click", startPolling);
stopButton.addEventListener("click", stopPolling);
refreshButton.addEventListener("click", fetchGatewayData);

// ---------- Init ----------

window.addEventListener("load", () => {
  setConnectionStatus(false, "idle");
  setError(null);
});
