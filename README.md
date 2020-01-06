# react-native-android-wifi

A react-native module for viewing and connecting to Wifi networks on Android devices.

![example app](/docs/example-app.gif)

### Installation

### Add it to your android project
```bash
npm install react-native-android-wifi --save
```

### Install the native dependencies
Use react-native link to install native dependencies automatically:
```bash
react-native link react-native-android-wifi
```
or do it manually as described [here](docs/link-manually.md).

### Example usage

```javascript
import wifi from 'react-native-android-wifi';
```

Permissions: Starting with Android API 25, apps must be granted the ACCESS_COARSE_LOCATION (or ACCESS_FINE_LOCATION) permission in order to obtain results.
```javascript
try {
      const granted = await PermissionsAndroid.request(
        PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION,
        {
          'title': 'Wifi networks',
          'message': 'We need your permission in order to find wifi networks'
        }
      )
      if (granted === PermissionsAndroid.RESULTS.GRANTED) {
        console.log("Thank you for your permission! :)");
      } else {
        console.log("You will not able to retrieve wifi available networks list");
      }
    } catch (err) {
      console.warn(err)
    }
```

Wifi connectivity status:
```javascript
wifi.isEnabled((isEnabled) => {
  if (isEnabled) {
    console.log("wifi service enabled");
  } else {
    console.log("wifi service is disabled");
  }
});
```

Enable/Disable wifi service:
```javascript
//Set TRUE to enable and FALSE to disable; 
wifi.setEnabled(true);
```

Sign device into a specific network:
> This method doesn't have a callback when connection succeeded, check [this](https://github.com/devstepbcn/react-native-android-wifi/issues/4) issue.
Added support for 'WPA2 PSK' wifi security mode and handling SSID for Lollipop and Kitkat.

```javascript
//found returns true if ssid is in the range
wifi.findAndConnect(ssid, password, (found) => {
  if (found) {
    console.log("wifi is in range");
  } else {
    console.log("wifi is not in range");
  }
});
```

Disconnect from current wifi network
```javascript
wifi.disconnect();
```

Get current SSID
```javascript
wifi.getSSID((ssid) => {
  console.log(ssid);
});
```

Get current BSSID
```javascript
wifi.getBSSID((bssid) => {
  console.log(bssid);
});
```

Get all wifi networks in range
```javascript
/*
wifiStringList is a stringified JSONArray with the following fields for each scanned wifi
{
  "SSID": "The network name",
  "BSSID": "The address of the access point",
  "capabilities": "Describes the authentication, key management, and encryption schemes supported by the access point"
  "frequency":"The primary 20 MHz frequency (in MHz) of the channel over which the client is communicating with the access point",
  "level":"The detected signal level in dBm, also known as the RSSI. (Remember its a negative value)",
  "timestamp":"Timestamp in microseconds (since boot) when this result was last seen"
}
*/
wifi.loadWifiList((wifiStringList) => {
  var wifiArray = JSON.parse(wifiStringList);
    console.log(wifiArray);
  },
  (error) => {
    console.log(error);
  }
);
```

connectionStatus returns true or false depending on whether device is connected to wifi
```javascript
wifi.connectionStatus((isConnected) => {
  if (isConnected) {
      console.log("is connected");
    } else {
      console.log("is not connected");
  }
});
```

Get connected wifi signal strength
```javascript
//level is the detected signal level in dBm, also known as the RSSI. (Remember its a negative value)
wifi.getCurrentSignalStrength((level) => {
  console.log(level);
});
```

Get connected wifi frequency
```javascript
wifi.getFrequency((frequency) => {
  console.log(frequency);
})
```

Get current IP
```javascript
//get the current network connection IP
wifi.getIP((ip) => {
  console.log(ip);
});
```
Get DHCP Server Adress
```javascript
//get the DHCP server IP
wifi.getDhcpServerAddress((ip) => {
  console.log(ip);
});
```

Remove/Forget the Wifi network from mobile by SSID, returns boolean
This method will remove the wifi network as per the passed SSID from the device list.
``` javascript
wifi.isRemoveWifiNetwork(ssid, (isRemoved) => {
  console.log("Forgetting the wifi device - " + ssid);
});
```

Starts native Android wifi network scanning and returns list
Hard refresh the Android wifi scan, implemented using `BroadcastReceiver` to ensure that it automatically detects new wifi connections available.
``` javascript
wifi.reScanAndLoadWifiList((wifiStringList) => {
  var wifiArray = JSON.parse(wifiStringList);
  console.log('Detected wifi networks - ',wifiArray);
},(error)=>{
  console.log(error);
});
```

Method to force wifi usage. Android by default sends all requests via mobile data if the connected wifi has no internet connection.
``` javascript
//Set true/false to enable/disable forceWifiUsage.
//Is important to enable only when communicating with the device via wifi
//and remember to disable it when disconnecting from device.
wifi.forceWifiUsage(true);
```

Method to get connection status of a forced network (because it takes some time to be set up).
``` javascript
//Callback returns true if the process of forcing network usage is finished
wifi.connectionStatusOfBoundNetwork((isBound) => {
    if (isBound) {
        console.log('Network is bound');
    } else {
        console.log('Network isn\'t bound');
    }
});
```


Add a hidden wifi network and connect to it
``` javascript
//Callback returns true if network added and tried to connect to it successfully
//It may take up to 15s to connect to hidden networks
wifi.connectToHiddenNetwork(ssid, password, (networkAdded) => {});
```