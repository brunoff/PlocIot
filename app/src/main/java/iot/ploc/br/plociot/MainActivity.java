package iot.ploc.br.plociot;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.NumberPicker;
import android.widget.Switch;
import android.widget.TextView;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private final String passwd = "1256784512";
    WifiManager mainWifi;
    WifiReceiver receiverWifi;

    StringBuilder sb = new StringBuilder();
    private String ssidpattern = "plocwifi_1";

    MqttAndroidClient mqttAndroidClient;

    final String serverUri = "tcp://m12.cloudmqtt.com:17349";
    final String userMqtt = "yhwoepqv";
    final String passMqtt = "i6zwCPjvGHVN";
//    TextView txtOutput;


    String clientId = "ExampleAndroidClient";
    final String subscriptionTopic = "state/connect";
//    final String publishTopic = "exampleAndroidPublishTopic";
//    final String publishMessage = "Hello World!";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        txtOutput = findViewById(R.id.output);

        GridView gv = findViewById(R.id.gridview);
        gv.setNumColumns(2);
        gv.setAdapter(new ImageAdapter(this));

//
        mainWifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        receiverWifi = new WifiReceiver();
        registerReceiver(receiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        if (mainWifi.isWifiEnabled() == false) {
            mainWifi.setWifiEnabled(true);
        }

        try {
            connectAt("plocwifi_1");
        } catch (Exception e) {
            e.printStackTrace();
        }

//        new AsyncTask<Void, Void, Void>(){
//
//            @Override
//            protected Void doInBackground(Void... voids) {
//                try {
//
//                    int x = 0;
//                    while (x==0) {
//                        List<WifiConfiguration> list = mainWifi.getConfiguredNetworks();
//                        for (WifiConfiguration i : list) {
//                            if (i.SSID != null && i.SSID.equals("\"" + "plocwifi_1" + "\"")) {
//                                Log.v("config", "WifiConfiguration SSID " + i.SSID);
//
//                                boolean isDisconnected = mainWifi.disconnect();
//                                Log.v("config", "isDisconnected : " + isDisconnected);
//
//                                boolean isEnabled = mainWifi.enableNetwork(i.networkId, true);
//                                Log.v("config", "isEnabled : " + isEnabled);
//
//                                boolean isReconnected = mainWifi.reconnect();
//                                Log.v("config", "isReconnected : " + isReconnected);
//
////                wfMgr.enableNetwork(i.networkId, true);
////                Log.i("config", "connectToWifi: will enable " + i.SSID);
////                wfMgr.reconnect();
//
//
//                            }
//                        }
//
//
//                        two();
//                        Thread.sleep(3000);
//
//                    }
//
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//                return null;
//            }
//        }.execute((Void) null);
        initMqtt();

    }

    private void sendChannelOutput(int channel, boolean isEnabled) {
        publishMessageMyChannel(channel+"="+(isEnabled?"1":"0"));
    }

    private void publishMessageMyChannel(String s) {

        publishMessage("output/13666860/", s);
    }

    private void callInfo() {
        publishMessage("output/13666860/", "?");
    }

    private void initMqtt() {
        mqttAndroidClient = new MqttAndroidClient(getApplicationContext(), serverUri, clientId);
        mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {

                if (reconnect) {
                    Log.e("MQTT", "Reconnected to : " + serverURI);
//                    // Because Clean Session is true, we need to re-subscribe//                    subscribeToTopic();
                } else {
                    Log.e("MQTT", "Connected to: " + serverURI);
                }
                callInfo();
            }

            @Override
            public void connectionLost(Throwable cause) {
                Log.e("MQTT", "The Connection was lost.");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                printOuput(topic,message);
                Log.e("MQTT", "Message: " + topic + " : " + new String(message.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });

        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setUserName(userMqtt);
        mqttConnectOptions.setPassword(passMqtt.toCharArray());
        mqttConnectOptions.setCleanSession(false);


        try {
            //addToHistory("Connecting to " + serverUri);
            mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                    disconnectedBufferOptions.setBufferEnabled(true);
                    disconnectedBufferOptions.setBufferSize(100);
                    disconnectedBufferOptions.setPersistBuffer(false);
                    disconnectedBufferOptions.setDeleteOldestMessages(false);
                    mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
                    subscribeToTopic();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e("MQTT", "Failed to connect to: " + serverUri);
                }
            });


        } catch (MqttException ex) {
            ex.printStackTrace();
        }

    }

    public void subscribeToTopic() {
        try {
            String[] meandgroup = new String[]{"action/#","input/#"};
            int[] arr = new int[]{0,0};
            mqttAndroidClient.subscribe(meandgroup, arr,
            null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.e("MQTT", "Subscribed!");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e("MQTT", "Failed to subscribe");
                }
            });

            // THIS DOES NOT WORK!
            mqttAndroidClient.subscribe(subscriptionTopic, 0, new IMqttMessageListener() {
                @Override
                public void messageArrived(final String topic, final MqttMessage message) throws Exception {
                    // message Arrived!
                    printOuput(topic, message);

                    Log.e("MQTT", "Message: " + topic + " : " + new String(message.getPayload()));
                }
            });

        } catch (MqttException ex) {
            System.err.println("Exception whilst subscribing");
            ex.printStackTrace();
        }
    }

    private void printOuput(final String topic, final MqttMessage message) {
        processMessage(new String(message.getPayload()));

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String msg = topic+"  "+ new String(message.getPayload())+"\n";
//                txtOutput.append(msg);
            }
        });
    }

    private void processMessage(String s) {
        if (s.equalsIgnoreCase("1=1"))
        {
//            swOnOff.setChecked(true);
        }else if (s.equalsIgnoreCase("1=0")){

//            swOnOff.setChecked(false);
        }
    }

    public void publishMessage(String topic, String txt) {

        try {
            MqttMessage message = new MqttMessage();
            message.setPayload(txt.getBytes());
            mqttAndroidClient.publish(topic, message);
//            addToHistory("Message Published");
            if (!mqttAndroidClient.isConnected()) {
//                addToHistory(mqttAndroidClient.getBufferedMessageCount() + " messages in buffer.");
            }
        } catch (MqttException e) {
            System.err.println("Error Publishing: " + e.getMessage());
            e.printStackTrace();
        }
    }


    @Override
    protected void onPause() {
        unregisterReceiver(receiverWifi);
        super.onPause();
    }

    @Override
    protected void onResume() {
        registerReceiver(receiverWifi, new IntentFilter(
                WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        super.onResume();
    }


    class WifiReceiver extends BroadcastReceiver {
        public void onReceive(Context c, Intent intent) {

            ArrayList<ScanResult> connections = new ArrayList<>();
            ArrayList<Float> Signal_Strenth = new ArrayList<Float>();

            sb = new StringBuilder();
            List<ScanResult> wifiList;
            wifiList = mainWifi.getScanResults();
            for (int i = 0; i < wifiList.size(); i++) {
                if (wifiList.get(i).SSID.startsWith(ssidpattern)) {
                    connections.add(wifiList.get(i));
                }
            }


        }
    }

    private void connectAt(String scanResult) throws Exception {
        WifiConfiguration wfc = new WifiConfiguration();

        wfc.SSID = "\"".concat(scanResult).concat("\"");
        wfc.status = WifiConfiguration.Status.DISABLED;
        wfc.priority = 40;

        wfc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        wfc.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
        wfc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        wfc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        wfc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
        wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
        wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);

        wfc.preSharedKey = "\"".concat(passwd).concat("\"");


//        int networkId = wfMgr.addNetwork(wfc);
//        if (networkId != -1) {
//            // success, can call wfMgr.enableNetwork(networkId, true) to connect
//        }

        int networkId = mainWifi.addNetwork(wfc);

        Log.v("config", "Add result " + networkId);

//        List<WifiConfiguration> list = wfMgr.getConfiguredNetworks();
//        for (WifiConfiguration i : list) {
//            if (i.SSID != null && i.SSID.equals("\"" + scanResult + "\"")) {
//                Log.v("config", "WifiConfiguration SSID " + i.SSID);
//
//                boolean isDisconnected = wfMgr.disconnect();
//                Log.v("config", "isDisconnected : " + isDisconnected);
//
//                boolean isEnabled = wfMgr.enableNetwork(i.networkId, true);
//                Log.v("config", "isEnabled : " + isEnabled);
//
//                boolean isReconnected = wfMgr.reconnect();
//                Log.v("config", "isReconnected : " + isReconnected);
//
////                wfMgr.enableNetwork(i.networkId, true);
////                Log.i("config", "connectToWifi: will enable " + i.SSID);
////                wfMgr.reconnect();
//
//
//
//            }
//        }
    }

    private OkHttpClient client = new OkHttpClient().newBuilder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    public void two() {
        Request request = new Request.Builder()
                .url("http://192.168.4.1/?s=PlocWifi&p=1256784512")
                .build();
        try {
            Log.e("config", client.newCall(request).execute().body().string());
        } catch (Exception e) {
            Log.e("config", "exc", e);
        }


    }


    public class ImageAdapter extends BaseAdapter {
        private final LayoutInflater layoutInflator;
        private Context mContext;
        Switch swOnOff;

        public ImageAdapter(Context c) {
            mContext = c;
            layoutInflator = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        }

        public int getCount() {
            return mThumbIds.length;
        }

        public Object getItem(int position) {
            return null;
        }

        public long getItemId(int position) {
            return 0;
        }

        // create a new ImageView for each item referenced by the Adapter
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = layoutInflator.inflate(R.layout.output_device_item, null);
                convertView.findViewById(R.id.icon_timer).setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                final Dialog d = new Dialog(MainActivity.this);
                                d.setTitle("NumberPicker");
                                d.setContentView(R.layout.dialog_timer);
                                Button b1 = (Button) d.findViewById(R.id.button1);
//                        Button b2 = (Button) d.findViewById(R.id.button2);
                                final NumberPicker np = d.findViewById(R.id.numberPicker1);
                                np.setMaxValue(100); // max value 100
                                np.setMinValue(0);   // min value 0
                                np.setWrapSelectorWheel(false);
                                np.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
                                    @Override
                                    public void onValueChange(NumberPicker picker, int oldVal, int newVal) {

                                    }
                                });
                                b1.setOnClickListener(new View.OnClickListener()
                                {
                                    @Override
                                    public void onClick(View v) {
                                        publishMessageMyChannel("1=TM"+np.getValue());
//                                tv.setText(String.valueOf(np.getValue())); //set the value to textview
                                        d.dismiss();
                                    }
                                });
//                        b2.setOnClickListener(new OnClickListener()
//                        {
//                            @Override
//                            public void onClick(View v) {
//                                d.dismiss(); // dismiss the dialog
//                            }
//                        });
                                d.show();
                            }
                        }
                );
                swOnOff = convertView.findViewById(R.id.switch_onoff);
                swOnOff.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        sendChannelOutput(1, swOnOff.isChecked());
                    }
                });
            } else {

            }
            return convertView;
        }

        // references to our images
        private Integer[] mThumbIds = {
                R.drawable.airfrayer, R.drawable.airfrayer, R.drawable.airfrayer
        };
    }
}
