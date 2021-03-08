|   |   |   |
| :-: | :-: | :-: |
| <img src="./assets/rakstar.jpg" width="150"> | <img src="./assets/Icon.png" width="150"> | <img src="./assets/RAK-Whirls.png" width="150"> |

# My-nRF52-Toolbox
Simplified nRF52 toolbox created from [Nordic's nRF52 Toolbox](https://github.com/NordicSemiconductor/Android-nRF-Toolbox)

This application supports right now 4 functions
- Firmware update over BLE with OTA DFU for nRF52 chips
- BLE UART application
- Setup LoRa® and LoRaWAN® parameters of RAK4631 devices over BLE
- Setup ESP32 WiFi credentials over BLE

# WORK IN PROGRESS => NO GUARANTEE THAT IT WORKS ON YOUR PHONE

## OTA DFU
More details in the [NordicSemiconductor/Android-DFU-Libray](https://github.com/NordicSemiconductor/Android-DFU-Library)

## BLE UART
More details to be added

## ESP32 WiFi configuration
More details to be added    
Works only with [RAK11200-WiFi-setup-over-BLE](https://github.com/beegee-tokyo?tab=repositories) example code running on the ESP32

### Screenshots

## LoRa® / LoRaWAN® configuration
- Setup nodes that support LoRa® P2P
- Setup nodes that support LoRaWAN®
- Setup nodes that support both LoRa® P2P and LoRaWAN®
- Build in QR scanner to read device/join EUI from a label on the device
Works only with [RAK4631-LoRa-BLE-Config](https://github.com/beegee-tokyo/RAK4631-LoRa-BLE-Config) examples

### Screenshots

LPWAN OTAA setup    
<img src="./assets/large-7.jpg" alt="LPWAN 1" width="250">

----
LPWAN ABP setup    
<img src="./assets/large-9.jpg" alt="LPWAN 2" width="250">

----
P2P setup    
<img src="./assets/large-8.jpg" alt="P2P" width="250">

----

Scan QR code    
<img src="./assets/Scan-QR.gif" alt="QR code" width="250">