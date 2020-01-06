# react-native-android-wifi
## Link Manually

* In `android/setting.gradle`
```gradle
...
include ':app'
include ':react-native-android-wifi'
project(':react-native-android-wifi').projectDir = new File(rootProject.projectDir, '../node_modules/react-native-android-wifi/android')
```

* In `android/app/build.gradle`

```gradle
...
dependencies {
    ...
  compile project(':react-native-android-wifi')
}
```

On newer versions of React Native (0.29.0+):
* register module (in MainApplication.java)


```java
import com.devstepbcn.wifi.AndroidWifiPackage;  // <--- import

public class MainApplication extends Application implements ReactApplication {
  ......

    @Override
    protected List<ReactPackage> getPackages() {
      return Arrays.<ReactPackage>asList(
          new MainReactPackage(),
          new AndroidWifiPackage(), // <------ add here
      );
    }
  };
```

On older versions of React Native (>=0.18<0.29.0):

* register module (in MainActivity.java)

```java
import com.devstepbcn.wifi.AndroidWifiPackage;  // <--- import

public class MainActivity extends ReactActivity {
  ......
  
  /**
   * A list of packages used by the app. If the app uses additional views
   * or modules besides the default ones, add more packages here.
   */
    @Override
    protected List<ReactPackage> getPackages() {
      return Arrays.<ReactPackage>asList(
        new AndroidWifiPackage(), // <------ add here
        new MainReactPackage());
    }
}
```

On older versions of React Native:

* register module (in MainActivity.java)

```java
import com.devstepbcn.wifi.AndroidWifiPackage;  // <--- import

public class MainActivity extends Activity implements DefaultHardwareBackBtnHandler {
  ......

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mReactRootView = new ReactRootView(this);

    mReactInstanceManager = ReactInstanceManager.builder()
      .setApplication(getApplication())
      .setBundleAssetName("index.android.bundle")
      .setJSMainModuleName("index.android")
      .addPackage(new MainReactPackage())
      .addPackage(new AndroidWifiPackage())              // <------ add here
      .setUseDeveloperSupport(BuildConfig.DEBUG)
      .setInitialLifecycleState(LifecycleState.RESUMED)
      .build();

    mReactRootView.startReactApplication(mReactInstanceManager, "ExampleRN", null);

    setContentView(mReactRootView);
  }

  ......

}
```