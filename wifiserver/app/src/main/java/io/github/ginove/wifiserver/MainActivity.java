  package io.github.ginove.wifiserver;

  import android.annotation.TargetApi;
  import android.app.Activity;
  import android.content.BroadcastReceiver;
  import android.content.Context;
  import android.content.Intent;
  import android.content.IntentFilter;
  import android.net.ConnectivityManager;
  import android.net.NetworkInfo;
  import android.net.Uri;
  import android.net.wifi.WifiConfiguration;
  import android.net.wifi.WifiInfo;
  import android.net.wifi.WifiManager;
  import android.os.Build;
  import android.os.Bundle;
  import android.os.Environment;
  import android.os.Handler;
  import android.os.Message;
  import android.os.StrictMode;
  import android.provider.Settings;
  import android.support.v7.app.AppCompatActivity;
  import android.util.Log;
  import android.view.View;
  import android.widget.TextView;
  import android.widget.Toast;

  import io.github.ginove.wifiserver.thread.ConnectThread;
  import io.github.ginove.wifiserver.thread.ListenerThread;


  import io.github.ginove.wifiserver.thread.ConnectThread;
  import io.github.ginove.wifiserver.thread.ListenerThread;

  import java.io.BufferedReader;
  import java.io.File;
  import java.io.FileReader;
  import java.io.IOException;
  import java.lang.reflect.InvocationTargetException;
  import java.lang.reflect.Method;
  import java.net.InetAddress;
  import java.net.NetworkInterface;
  import java.net.Socket;
  import java.net.SocketException;
  import java.util.ArrayList;
  import java.util.Enumeration;
  import java.util.List;

  import ru.bartwell.exfilepicker.ExFilePicker;
  import ru.bartwell.exfilepicker.data.ExFilePickerResult;

  import static io.github.ginove.wifiserver.R.id.create_wifi;

  public class MainActivity extends AppCompatActivity implements View.OnClickListener {

      public static final int DEVICE_CONNECTING = 1;//有设备正在连接热点
      public static final int DEVICE_CONNECTED  = 2;//有设备连上热点
      public static final int SEND_MSG_SUCCSEE  = 3;//发送消息成功
      public static final int SEND_MSG_ERROR    = 4;//发送消息失败
      public static final int GET_MSG           = 6;//获取新消息
      private TextView text_state;
      private WifiManager wifiManager;
      /**
       * 连接线程
       */
      private ConnectThread connectThread;


      /**
       * 监听线程
       */
      private ListenerThread listenerThread;
      /**
       * 热点名称
       */
      private static final String WIFI_HOTSPOT_SSID = "TEST";
      /**
       * 端口号
       */
      private static final int    PORT              = 54321;


      private static final int EX_FILE_PICKER_RESULT = 0;
      @Override
      protected void onCreate(Bundle savedInstanceState) {
          super.onCreate(savedInstanceState);
          setContentView(R.layout.activity_main);

          StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();

          StrictMode.setThreadPolicy(policy);

          findViewById(R.id.create_wifi).setOnClickListener(this);
          findViewById(R.id.close_wifi).setOnClickListener(this);
          findViewById(R.id.send).setOnClickListener(this);
          text_state = (TextView) findViewById(R.id.receive);


          wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
          /**
           * 先开启监听线程，在开启连接
           */
          listenerThread = new ListenerThread(PORT, handler);
          listenerThread.start();
          try {
              Thread.sleep(1000);
          } catch (InterruptedException e) {
              e.printStackTrace();
          }
          //        开启连接线程
          //  initBroadcastReceiver();
          new Thread(new Runnable() {
              @Override
              public void run() {
                  try {

                      try {
                          ArrayList<String> connectedIP = getConnectedIP();
                          if (connectedIP != null) {
                              for (String ip : connectedIP) {
                                  Log.d("Thread", ip);
                                  if (ip.contains(".")) {
                                      Log.i("AAA", "IP:" + ip);
                                      Socket socket = new Socket(ip, PORT);
                                      connectThread = new ConnectThread(socket, handler);
                                      connectThread.start();

                                  }
                              }
                          }
                      }catch (Exception e) {
                          e.printStackTrace();
                          text_state.setText("没有发现连接上的");
                      }
                  } catch (Exception e) {
                      e.printStackTrace();
                      text_state.setText("没有发现连接上的");
                  }


              }
          }).start();



      }


      @Override
      public void onClick(View view) {
          switch (view.getId()) {
              case create_wifi:
                  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                      // 判断是否有WRITE_SETTINGS权限if(!Settings.System.canWrite(this))
                      if (!Settings.System.canWrite(this)) {
                          Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                                  Uri.parse("package:" + getPackageName()));
                          startActivityForResult(intent, 1);
                      } else {
                          createWifiHotspot();
                      }
                  }

                  break;
              case R.id.close_wifi:
                  //TODO implement
                  closeWifiHotspot();
                  break;
              case R.id.send:
                  ExFilePicker exFilePicker = new ExFilePicker();
                  exFilePicker.setShowOnlyExtensions("jpg");
                  exFilePicker.setQuitButtonEnabled(true);
                  exFilePicker.setStartDirectory(Environment.getExternalStorageDirectory()+"/DCIM/");
                  exFilePicker.start(this,EX_FILE_PICKER_RESULT);

                  break;

          }
      }

      @Override
      public void onActivityResult(int requestCode, int resultCode, Intent data) {
          if (requestCode == EX_FILE_PICKER_RESULT) {
              ExFilePickerResult result = ExFilePickerResult.getFromIntent(data);

              if (result != null && result.getCount() > 0) {
                  String path = result.getPath();

                  List<String> names = result.getNames();
                  for (int i = 0; i < names.size(); i++) {
                      File f = new File(path, names.get(i));
                      try {
                          Uri uri = Uri.fromFile(f); //这里获取了真实可用的文件资源
                          Toast.makeText(this, "选择文件:" + uri.getPath(), Toast.LENGTH_SHORT)
                                  .show();
                          if (connectThread != null) {
                              connectThread.sendData(f);
                          }  else {
                              Log.i("AAA", "connectThread == null");
                          }

                      } catch (Exception e) {
                          e.printStackTrace();
                      }
                  }
              }
          }
      }
      /**
       * 创建Wifi热点
       */
      private void createWifiHotspot() {

          if (wifiManager.isWifiEnabled()) {
              //如果wifi处于打开状态，则关闭wifi,
              wifiManager.setWifiEnabled(false);
          }
          WifiConfiguration config = new WifiConfiguration();
          config.SSID = WIFI_HOTSPOT_SSID;
          config.preSharedKey = "123456789";
          config.hiddenSSID = true;
          config.status = WifiConfiguration.Status.ENABLED;
          config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
          config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
          config.allowedKeyManagement.set(4);
          config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
          config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
          try {

              Method method = wifiManager.getClass().getMethod(
                      "setWifiApEnabled", WifiConfiguration.class, Boolean.TYPE);
              boolean enable = (Boolean) method.invoke(wifiManager, config, true);
              if (enable) {
                  text_state.setText("热点已开启 SSID:" + WIFI_HOTSPOT_SSID + " password:123456789");

                  // listenerThread = new ListenerThread(PORT, handler);
                  //       listenerThread.start();

              } else {
                  text_state.setText("创建热点失败1.0");
              }
          } catch (Exception e) {
              e.printStackTrace();
              text_state.setText("创建热点失败2.0");
          }
      }



      /**
       * 关闭WiFi热点
       */
      public void closeWifiHotspot() {
          try {
              Method method = wifiManager.getClass().getMethod("getWifiApConfiguration");
              method.setAccessible(true);
              WifiConfiguration config = (WifiConfiguration) method.invoke(wifiManager);
              Method method2 = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
              method2.invoke(wifiManager, config, false);
          } catch (NoSuchMethodException e) {
              e.printStackTrace();
          } catch (IllegalArgumentException e) {
              e.printStackTrace();
          } catch (IllegalAccessException e) {
              e.printStackTrace();
          } catch (InvocationTargetException e) {
              e.printStackTrace();
          }
          text_state.setText("热点已关闭");
      }





      private Handler handler = new Handler() {
          @Override
          public void handleMessage(Message msg) {
              switch (msg.what) {
                  case DEVICE_CONNECTING:
                      connectThread = new ConnectThread(listenerThread.getSocket(), handler);
                      connectThread.start();
                      break;
                  case DEVICE_CONNECTED:
                      text_state.setText("设备连接成功");
                      break;
                  case SEND_MSG_SUCCSEE:
                      text_state.setText("发送消息成功:" + msg.getData().getString("MSG"));
                      break;
                  case SEND_MSG_ERROR:
                      text_state.setText("发送消息失败:" + msg.getData().getString("MSG"));
                      break;
                  case GET_MSG:
                      text_state.setText("收到消息:" + msg.getData().getString("MSG"));
                      break;
              }
          }
      };

      private ArrayList<String> getConnectedIP() {
          ArrayList<String> connectedIP = new ArrayList<String>();
          try {
              BufferedReader br = new BufferedReader(new FileReader(
                      "/proc/net/arp"));
              String line;

              while ((line = br.readLine()) != null) {
                  String[] splitted = line.split(" +");
                  if (splitted != null && splitted.length >= 4) {
                      String ip = splitted[0];
                      connectedIP.add(ip);
                  }
              }
          } catch (Exception e) {
              e.printStackTrace();
          }
          //        Log.i("connectIp:", connectedIP);
          return connectedIP;
      }

      @Override
      protected void onDestroy() {
          super.onDestroy();
          System.exit(0);
      }
  }