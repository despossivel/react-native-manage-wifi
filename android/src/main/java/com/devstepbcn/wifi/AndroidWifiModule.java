package com.devstepbcn.wifi;

import com.facebook.react.uimanager.*;
import com.facebook.react.bridge.*;
import com.facebook.systrace.Systrace;
import com.facebook.systrace.SystraceMessage;
import com.facebook.react.ReactInstanceManager;
import com.facebook.react.ReactRootView;
import com.facebook.react.modules.core.DefaultHardwareBackBtnHandler;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.shell.MainReactPackage;
import com.facebook.soloader.SoLoader;
import android.provider.Settings;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiConfiguration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.NetworkCapabilities;
import android.net.Network;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;
import java.util.List;
import java.lang.Thread;
import android.net.DhcpInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class AndroidWifiModule extends ReactContextBaseJavaModule {

	//WifiManager Instance
	WifiManager wifi;
	ReactApplicationContext reactContext;

	//Constructor
	public AndroidWifiModule(ReactApplicationContext reactContext) {
		super(reactContext);

		wifi = (WifiManager)reactContext.getSystemService(Context.WIFI_SERVICE);
		this.reactContext = reactContext;
	}

	//Name for module register to use:
	@Override
	public String getName() {
		return "AndroidWifiModule";
	}

	//Method to load wifi list into string via Callback. Returns a stringified JSONArray
	@ReactMethod
	public void loadWifiList(Callback successCallback, Callback errorCallback) {
		try {
			List < ScanResult > results = wifi.getScanResults();
			JSONArray wifiArray = new JSONArray();

			for (ScanResult result: results) {
				JSONObject wifiObject = new JSONObject();
				if(!result.SSID.equals("")){
					try {
			            wifiObject.put("SSID", result.SSID);
			            wifiObject.put("BSSID", result.BSSID);
			            wifiObject.put("capabilities", result.capabilities);
			            wifiObject.put("frequency", result.frequency);
			            wifiObject.put("level", result.level);
			            wifiObject.put("timestamp", result.timestamp);
			            //Other fields not added
			            //wifiObject.put("operatorFriendlyName", result.operatorFriendlyName);
			            //wifiObject.put("venueName", result.venueName);
			            //wifiObject.put("centerFreq0", result.centerFreq0);
			            //wifiObject.put("centerFreq1", result.centerFreq1);
			            //wifiObject.put("channelWidth", result.channelWidth);
					} catch (JSONException e) {
          				errorCallback.invoke(e.getMessage());
					}
					wifiArray.put(wifiObject);
				}
			}
			successCallback.invoke(wifiArray.toString());
		} catch (IllegalViewOperationException e) {
			errorCallback.invoke(e.getMessage());
		}
	}

	//Method to force wifi usage if the user needs to send requests via wifi
	//if it does not have internet connection. Useful for IoT applications, when
	//the app needs to communicate and send requests to a device that have no 
	//internet connection via wifi.

	//Receives a boolean to enable forceWifiUsage if true, and disable if false.
	//Is important to enable only when communicating with the device via wifi 
	//and remember to disable it when disconnecting from device.
	@ReactMethod
	public void forceWifiUsage(boolean useWifi) {
        boolean canWriteFlag = false;
		
        if (useWifi) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    canWriteFlag = true;
                    // Only need ACTION_MANAGE_WRITE_SETTINGS on 6.0.0, regular permissions suffice on later versions
                } else if (Build.VERSION.RELEASE.toString().equals("6.0.1")) {
                    canWriteFlag = true;
                    // Don't need ACTION_MANAGE_WRITE_SETTINGS on 6.0.1, if we can positively identify it treat like 7+
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // On M 6.0.0 (N+ or higher and 6.0.1 hit above), we need ACTION_MANAGE_WRITE_SETTINGS to forceWifi.
                    canWriteFlag = Settings.System.canWrite(reactContext);
                    if (!canWriteFlag) {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                        intent.setData(Uri.parse("package:" + reactContext.getPackageName()));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                        reactContext.startActivity(intent);
                    }
                }

                if (((Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) && canWriteFlag) || ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) && !(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M))) {
                    final ConnectivityManager manager = (ConnectivityManager) reactContext
                            .getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkRequest.Builder builder;
                    builder = new NetworkRequest.Builder();
                    //set the transport type do WIFI
                    builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);


                    manager.requestNetwork(builder.build(), new ConnectivityManager.NetworkCallback() {
                        @Override
                        public void onAvailable(Network network) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                manager.bindProcessToNetwork(network);
                            } else {
                                //This method was deprecated in API level 23
                                ConnectivityManager.setProcessDefaultNetwork(network);
                            }
                            try {
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            manager.unregisterNetworkCallback(this);
                        }
                    });
                }
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ConnectivityManager manager = (ConnectivityManager) reactContext
                        .getSystemService(Context.CONNECTIVITY_SERVICE);
                manager.bindProcessToNetwork(null);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ConnectivityManager.setProcessDefaultNetwork(null);
            }
        }
    }

	//Use this method to make sure that your forced network already bound
	@ReactMethod
	public void connectionStatusOfBoundNetwork(Callback connectionStatusResult) {
		ConnectivityManager connManager = (ConnectivityManager) getReactApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
		Network network = connManager.getBoundNetworkForProcess();
		if (network != null) {
			connectionStatusResult.invoke(true);
		} else {
			connectionStatusResult.invoke(false);
		}
	}

	//Method to check if wifi is enabled
	@ReactMethod
	public void isEnabled(Callback isEnabled) {
		isEnabled.invoke(wifi.isWifiEnabled());
	}

	//Method to connect/disconnect wifi service
	@ReactMethod
	public void setEnabled(Boolean enabled) {
		wifi.setWifiEnabled(enabled);
	}

	//Send the ssid and password of a Wifi network into this to connect to the network.
	//Example:  wifi.findAndConnect(ssid, password);
	//After 10 seconds, a post telling you whether you are connected will pop up.
	//Callback returns true if ssid is in the range
	@ReactMethod
	public void findAndConnect(String ssid, String password, Callback ssidFound) {
		List < ScanResult > results = wifi.getScanResults();
		boolean connected = false;
		for (ScanResult result: results) {
			String resultString = "" + result.SSID;
			if (ssid.equals(resultString)) {
				connected = connectTo(result, password, ssid);
			}
		}
		ssidFound.invoke(connected);
	}

	//Use this method to check if the device is currently connected to Wifi.
	@ReactMethod
	public void connectionStatus(Callback connectionStatusResult) {
		ConnectivityManager connManager = (ConnectivityManager) getReactApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		if (mWifi.isConnected()) {
			connectionStatusResult.invoke(true);
		} else {
			connectionStatusResult.invoke(false);
		}
	}

	//Method to connect to WIFI Network
	public Boolean connectTo(ScanResult result, String password, String ssid) {
		//Make new configuration
		WifiConfiguration conf = new WifiConfiguration();

    	//clear alloweds
		conf.allowedAuthAlgorithms.clear();
		conf.allowedGroupCiphers.clear();
		conf.allowedKeyManagement.clear();
		conf.allowedPairwiseCiphers.clear();
		conf.allowedProtocols.clear();

    	// Quote ssid and password
		conf.SSID = String.format("\"%s\"", ssid);
	
    	WifiConfiguration tempConfig = this.IsExist(conf.SSID);
		if (tempConfig != null) {
			wifi.removeNetwork(tempConfig.networkId);
		}

		String capabilities = result.capabilities;
		
    	// appropriate ciper is need to set according to security type used
		if (capabilities.contains("WPA") || capabilities.contains("WPA2") || capabilities.contains("WPA/WPA2 PSK")) {

			// This is needed for WPA/WPA2 
			// Reference - https://android.googlesource.com/platform/frameworks/base/+/refs/heads/master/wifi/java/android/net/wifi/WifiConfiguration.java#149
			conf.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);

			conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
			conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);

			conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);

			conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
			conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);

			conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
			conf.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
			conf.status = WifiConfiguration.Status.ENABLED;
			conf.preSharedKey = String.format("\"%s\"", password);
      
		} else if (capabilities.contains("WEP")) {
			// This is needed for WEP
			// Reference - https://android.googlesource.com/platform/frameworks/base/+/refs/heads/master/wifi/java/android/net/wifi/WifiConfiguration.java#149
			conf.wepKeys[0] = "\"" + password + "\"";
			conf.wepTxKeyIndex = 0;
			conf.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
			conf.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
			conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
			conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
    	} else {
			conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
		}

		List<WifiConfiguration> mWifiConfigList = wifi.getConfiguredNetworks();
		if (mWifiConfigList == null) {
		    return false;
        }

		int updateNetwork = -1;

		// Use the existing network config if exists
		for (WifiConfiguration wifiConfig : mWifiConfigList) {
			if (wifiConfig.SSID.equals(conf.SSID)) {
        		conf=wifiConfig;
				updateNetwork=conf.networkId;
			}
		}

		// If network not already in configured networks add new network
		if ( updateNetwork == -1 ) {
	      updateNetwork = wifi.addNetwork(conf);
	      wifi.saveConfiguration();
		}

    	// if network not added return false
		if ( updateNetwork == -1 ) {
			return false;
		}

    	// disconnect current network
		boolean disconnect = wifi.disconnect();
		if ( !disconnect ) {
			return false;
		}

   		// enable new network
		boolean enableNetwork = wifi.enableNetwork(updateNetwork, true);
		if ( !enableNetwork ) {
			return false;
		}

		return true;
	}
	
	//add configuration of hidden network and return it's networkId
	public int setWifiConfig(String ssid, String sharedKey) {
		WifiConfiguration conf = new WifiConfiguration();

		conf.SSID = "\"" + ssid + "\"";
		conf.preSharedKey = "\"" + sharedKey + "\"";
	
		conf.hiddenSSID = true;
		conf.status = WifiConfiguration.Status.ENABLED;
		conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
		conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
		conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
		conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
		conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
		conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
		conf.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
	
		return wifi.addNetwork(conf);
	}

	//Add a hidden wifi network and connect to it
	//Example:  wifi.connectToHiddenNetwork(ssid, password, (networkAdded) => {});
	//Callback returns true if network added and tried to connect to it successfully
	//It may take up to 15s to connect to hidden networks
	@ReactMethod
	public void connectToHiddenNetwork(String ssid, String password, Callback networkAdded) {
		List<WifiConfiguration> list = wifi.getConfiguredNetworks();
		if (list == null) {
			networkAdded.invoke(false);
		    return;
        }

		int updateNetwork = -1;

		// check if network config exists and it's hidden
		for (WifiConfiguration wifiConfig : list) {
			if (wifiConfig.SSID.equals("\"" + ssid + "\"") && wifiConfig.hiddenSSID) {
				updateNetwork = wifiConfig.networkId;
			}
		}

		// If network not already in configured networks add new network
		if (updateNetwork == -1) {
			updateNetwork = setWifiConfig(ssid, password);
		}

		// if network not added return false
		if (updateNetwork == -1) {
			networkAdded.invoke(false);
			return;
		}
		
		// disconnect current network
		boolean disconnect = wifi.disconnect();
		if (!disconnect) {
			networkAdded.invoke(false);
			return;
		}

		// enable new network
		boolean enableNetwork = wifi.enableNetwork(updateNetwork, true);
		if (!enableNetwork) {
			networkAdded.invoke(false);
			return;
		}
	
		// reconnect to new network
		boolean reconnect = wifi.reconnect();
		if (!reconnect) {
			networkAdded.invoke(false);
			return;
		}

		wifi.saveConfiguration();

		networkAdded.invoke(true);
	}

	//Disconnect current Wifi.
	@ReactMethod
	public void disconnect() {
		wifi.disconnect();
	}

	//This method will return current ssid
	@ReactMethod
	public void getSSID(final Callback callback) {
		WifiInfo info = wifi.getConnectionInfo();

		// This value should be wrapped in double quotes, so we need to unwrap it.
		String ssid = info.getSSID();
		if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
			ssid = ssid.substring(1, ssid.length() - 1);
		}

		callback.invoke(ssid);
	}

	//This method will return the basic service set identifier (BSSID) of the current access point
	@ReactMethod
	public void getBSSID(final Callback callback) {
		WifiInfo info = wifi.getConnectionInfo();

		String bssid = info.getBSSID();

		callback.invoke(bssid.toUpperCase());
	}

	//This method will return current wifi signal strength
	@ReactMethod
	public void getCurrentSignalStrength(final Callback callback) {
		int linkSpeed = wifi.getConnectionInfo().getRssi();
		callback.invoke(linkSpeed);
	}

	//This method will return current wifi frequency
	@ReactMethod
	public void getFrequency(final Callback callback) {
		WifiInfo info = wifi.getConnectionInfo();
		int frequency = info.getFrequency();
		callback.invoke(frequency);
	}

	//This method will return current IP
	@ReactMethod
	public void getIP(final Callback callback) {
		WifiInfo info = wifi.getConnectionInfo();
		String stringip = longToIP(info.getIpAddress());
		callback.invoke(stringip);
	}

	//This method will remove the wifi network as per the passed SSID from the device list
	@ReactMethod
	public void isRemoveWifiNetwork(String ssid, final Callback callback) {
	    List<WifiConfiguration> mWifiConfigList = wifi.getConfiguredNetworks();
        if (mWifiConfigList == null) {
            return;
        }

	    for (WifiConfiguration wifiConfig : mWifiConfigList) {
			String comparableSSID = ('"' + ssid + '"'); //Add quotes because wifiConfig.SSID has them
			if(wifiConfig.SSID.equals(comparableSSID)) {
				wifi.removeNetwork(wifiConfig.networkId);
				wifi.saveConfiguration();
				callback.invoke(true);
				return;
			}
	    }
		callback.invoke(false);
	}

	@ReactMethod
	public void reScanAndLoadWifiList(Callback successCallback, Callback errorCallback) {
		WifiReceiver receiverWifi = new WifiReceiver(wifi, successCallback, errorCallback);
	   	getCurrentActivity().registerReceiver(receiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
	    wifi.startScan();
	}

	@ReactMethod
	public void getDhcpServerAddress(Callback callback) {
		DhcpInfo dhcpInfo = wifi.getDhcpInfo();
		String ip = longToIP(dhcpInfo.serverAddress);
		callback.invoke(ip);
	}

	public static String longToIP(int longIp){
		StringBuffer sb = new StringBuffer("");
		String[] strip=new String[4];
		strip[3]=String.valueOf((longIp >>> 24));
		strip[2]=String.valueOf((longIp & 0x00FFFFFF) >>> 16);
		strip[1]=String.valueOf((longIp & 0x0000FFFF) >>> 8);
		strip[0]=String.valueOf((longIp & 0x000000FF));
		sb.append(strip[0]);
		sb.append(".");
		sb.append(strip[1]);
		sb.append(".");
		sb.append(strip[2]);
		sb.append(".");
		sb.append(strip[3]);
		return sb.toString();
	}

	private WifiConfiguration IsExist(String SSID) {
		List<WifiConfiguration> existingConfigs = wifi.getConfiguredNetworks();
		if (existingConfigs == null) {
			return null;
		}

		for (WifiConfiguration existingConfig : existingConfigs) {
			if (existingConfig.SSID.equals("\"" + SSID + "\"")) {
				return existingConfig;
			}
		}
		return null;
	}

	class WifiReceiver extends BroadcastReceiver {

		private Callback successCallback;
		private Callback errorCallback;
		private WifiManager wifi;

		public WifiReceiver(final WifiManager wifi, Callback successCallback, Callback errorCallback) {
			super();
			this.successCallback = successCallback;
			this.errorCallback = errorCallback;
			this.wifi = wifi;
		}

		// This method call when number of wifi connections changed
      	public void onReceive(Context c, Intent intent) {
			
			c.unregisterReceiver(this);

			try {
				List < ScanResult > results = this.wifi.getScanResults();
				JSONArray wifiArray = new JSONArray();

				for (ScanResult result: results) {
					JSONObject wifiObject = new JSONObject();
					if(!result.SSID.equals("")){
						try {
				            wifiObject.put("SSID", result.SSID);
				            wifiObject.put("BSSID", result.BSSID);
				            wifiObject.put("capabilities", result.capabilities);
				            wifiObject.put("frequency", result.frequency);
				            wifiObject.put("level", result.level);
				            wifiObject.put("timestamp", result.timestamp);
						} catch (JSONException e) {
	          				this.errorCallback.invoke(e.getMessage());
							return;
						}
						wifiArray.put(wifiObject);
					}
				}
				this.successCallback.invoke(wifiArray.toString());
				return;
			} catch (IllegalViewOperationException e) {
				this.errorCallback.invoke(e.getMessage());
				return;
			}
		}
	}
}
