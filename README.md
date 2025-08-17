# Smart watch data collection system for Galaxy Watch 4

A Smartwatch sensor data collection compatible with WearOS watch platforms. This repository was made for the CareWear Project.

SmartWatDAQ:

V0: CareWearDAQ: With Bbackground timers and threading along with a customizable GUI for data collection.

![alt text](https://github.com/wearablebiosensing/SmartWatch-DataCollection-system/blob/main/V0/appscreenshot.png)

V1: CareWearDAQ: With Bbackground timers and threading.

App Architecture:

![alt text](https://github.com/wearablebiosensing/SmartWatch-DataCollection-system/blob/main/carewear_apparch.png)

CareWear App Screenshot:
App has minimal to no UI it runs in the background collects data and saves files to the device itself.

![alt text](https://github.com/wearablebiosensing/SmartWatch-DataCollection-system/blob/main/carewear_app.png)

V2: CareWearDAQ: With without bckground timers and threading.

- A simpler version of the app with start and stop button for collection of senosrs data. 
V3: CareWearDAQ: Wireless data collection system for at-home monitoring.
- A wireless MQTT based data collection Andorid app along with a companion Python Flask dashbaord for sensors data collection.

## Resources:

- Sensors Manager API - https://developer.android.com/reference/android/hardware/SensorManager
- MiroBoad Link: https://miro.com/app/board/uXjVMT6jOEA=/?share_link_id=543197703029