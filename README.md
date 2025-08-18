# SmartWatch Data Collection System for Galaxy Watch 4

A sensor data collection application compatible with WearOS smartwatches, designed for the CareWear Project. This repository contains several Galaxy Watch app versions for sensor data acquisition and at-home monitoring.

![System Overview](https://github.com/wearablebiosensing/SmartWatch-DataCollection-system/blob/main/Overview.png)

---

## Table of Contents
- [SmartWatch Data Collection System for Galaxy Watch 4](#smartwatch-data-collection-system-for-galaxy-watch-4)
  - [Table of Contents](#table-of-contents)
  - [Features](#features)
  - [Versions](#versions)
  - [Architecture](#architecture)
  - [Installation](#installation)
  - [Usage](#usage)
  - [Resources](#resources)
  - [License](#license)
  - [Contributing](#contributing)

---

## Features

- Collects sensor data (accelerometer, gyroscope, etc.) from Galaxy Watch 4 and other WearOS devices.
- Multiple app versions for different data collection needs.
- Wireless data transfer via MQTT (v3.0.0).
- Companion Python Flask dashboard for live data visualization (v3.0.0).
- Minimal or customizable UI for background data collection.
- Data saved locally or transmitted wirelessly.
- Open architecture for expanding sensor support.

---

## Versions

- **v0.0.0 CareWearDAQ:**  
  - Background timers and threading.
  - Customizable GUI for data collection.
  - Pre-release test version.

- **v1.0.0 CareWearDAQ:**  
  - Background timers and threading.
  - Minimal UI: runs in the background, collects data, and saves files to the device.

- **v2.0.0 CareWearDAQ:**  
  - Removed background timers and threading.
  - Simple UI with start/stop buttons for sensor data collection.

- **v3.0.0 CareWearDAQ:**  
  - Wireless data collection system for at-home monitoring.
  - Features an Android app (MQTT-based) and a companion Python Flask dashboard for sensor data visualization.

---

## Architecture

![App Architecture](https://github.com/wearablebiosensing/SmartWatch-DataCollection-system/blob/main/v1.0.0/carewear_apparch.png)

---

## Installation

1. **Clone the repository:**
   ```sh
   git clone https://github.com/wearablebiosensing/SmartWatch-DataCollection-system.git
   ```
2. **Open the relevant app version in Android Studio.**
3. **Build and install the APK on your Galaxy Watch 4 (or compatible WearOS device).**
4. For v3.0.0, install the companion Python Flask dashboard:
   ```sh
   cd v3.0.0/python-dashboard
   pip install -r requirements.txt
   python app.py
   ```

---

## Usage

- Launch the app on your smartwatch.
- For background data collection (v1.0.0), simply start the app; data will be collected and saved automatically.
- For manual data collection (v2.0.0), use the start/stop buttons.
- For wireless data collection (v3.0.0), ensure both the Android app and Python dashboard are running and connected to the same network.

---

## Resources

- [Android Sensors Manager API](https://developer.android.com/reference/android/hardware/SensorManager)
- [Miro Board Link](https://miro.com/app/board/uXjVMT6jOEA=/?share_link_id=543197703029)

---

## License

_Include your license information here, e.g., MIT License, if applicable._

---

## Contributing

We welcome contributions! Please open issues or pull requests for improvements, bug fixes, or new features.

---

**Contact:**  
For questions or support, please open an issue or contact the maintainers. Shehjar Sadhu (She/Her) shehjar_sadhu@uri.edu.
