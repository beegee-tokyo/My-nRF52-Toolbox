package tk.giesecke.my_nrf52_tb.lora_setup;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ScrollView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.Objects;
import java.util.UUID;

import no.nordicsemi.android.ble.data.Data;
import tk.giesecke.my_nrf52_tb.FeaturesActivity;
import tk.giesecke.my_nrf52_tb.R;
import tk.giesecke.my_nrf52_tb.profile.BleProfileService;
import tk.giesecke.my_nrf52_tb.profile.BleProfileServiceReadyActivity;

import static tk.giesecke.my_nrf52_tb.profile.BleProfileActivity.doReconnect;

public class LoRa_Settings_Activity extends BleProfileServiceReadyActivity<LoRaSettingsService.TemplateBinder> {

    private final String TAG = "LoRaActivity";
    static String mmDevice;
    static Context appContext;

    static String nodeDeviceEUI = "";
    static String nodeAppEUI = "";
    static String nodeAppKey = "";
    static String nodeDeviceAddr = "";
    static String nodeNwsKey = "";
    static String nodeAppsKey = "";

    static boolean otaaEna = false;
    static boolean adrEna = false;
    static boolean publicNetwork = false;
    static boolean dutyCycleEna = false;

    static long sendRepeatTime = 120000;
    static byte nbTrials = 5;
    static byte txPower = 22;
    static byte dataRate = 3;
    static byte loraClass = 0;
    static byte subBandChannel = 1;
    static byte appPort = 2;
    static byte region = 1;

    static boolean autoJoin = false;
    static boolean confirmedEna = false;
    static boolean loraWanEna = true;

    static long p2pFrequency = 923300000;
    static byte p2pTxPower = 22;
    static byte p2pBW = 0;
    static byte p2pSF = 7;
    static byte p2pCR = 1;
    static byte p2pPreLen = 8;
    static int p2pSymTimeout = 0;

    ScrollView lora_lorawan;
    ScrollView lora_lorap2p;

    CheckBox lora_mode;

    EditText lora_dev_eui;
    EditText lora_app_eui;
    EditText lora_app_key;
    EditText lora_dev_addr;
    EditText lora_nws_key;
    EditText lora_apps_key;

    CheckBox lora_otaa;
    CheckBox lora_pub_net;
    CheckBox lora_duty_cycle;
    CheckBox lora_adr;
    CheckBox lora_join;
    CheckBox lora_conf_msg;

    EditText lora_datarate;
    EditText lora_tx_pwr;
    EditText lora_class;
    EditText lora_subband;
    EditText lora_app_port;
    EditText lora_nb_trials;
    EditText lora_send_repeat;

    EditText lora_p2p_freq;
    EditText lora_p2p_tx_power;
    EditText lora_p2p_bw;
    EditText lora_p2p_sf;
    EditText lora_p2p_cr;
    EditText lora_p2p_pre_len;
    EditText lora_p2p_sym_timeout;

    private Menu thisMenu;

    @Override
    protected void onCreateView(final Bundle savedInstanceState) {
        setContentView(R.layout.activity_feature_lora_setting);

        appContext = this;

        setGUI();
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private void setGUI() {
        int currentOrientation = getResources().getConfiguration().orientation;
        if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        }

        lora_lorawan = findViewById(R.id.lora_lorawan);
        lora_lorap2p = findViewById(R.id.lora_lorap2p);

        lora_mode = findViewById(R.id.lora_mode);

        lora_dev_eui = findViewById(R.id.lora_dev_eui);
        lora_app_eui = findViewById(R.id.lora_app_eui);
        lora_app_key = findViewById(R.id.lora_app_key);
        lora_dev_addr = findViewById(R.id.lora_dev_addr);
        lora_nws_key = findViewById(R.id.lora_nws_key);
        lora_apps_key = findViewById(R.id.lora_apps_key);

        lora_otaa = findViewById(R.id.lora_otaa);
        lora_pub_net = findViewById(R.id.lora_pub_net);
        lora_duty_cycle = findViewById(R.id.lora_duty_cycle);
        lora_adr = findViewById(R.id.lora_adr);
        lora_join = findViewById(R.id.lora_join);
        lora_conf_msg = findViewById(R.id.lora_conf_msg);

        lora_datarate = findViewById(R.id.lora_datarate);
        lora_tx_pwr = findViewById(R.id.lora_tx_pwr);
        lora_class = findViewById(R.id.lora_class);
        lora_subband = findViewById(R.id.lora_subband);
        lora_app_port = findViewById(R.id.lora_app_port);
        lora_nb_trials = findViewById(R.id.lora_nb_trials);
        lora_send_repeat = findViewById(R.id.lora_send_repeat);

        lora_p2p_freq = findViewById(R.id.lora_p2p_freq);
        lora_p2p_tx_power = findViewById(R.id.lora_p2p_tx_power);
        lora_p2p_bw = findViewById(R.id.lora_p2p_bw);
        lora_p2p_sf = findViewById(R.id.lora_p2p_sf);
        lora_p2p_cr = findViewById(R.id.lora_p2p_cr);
        lora_p2p_pre_len = findViewById(R.id.lora_p2p_pre_len);
        lora_p2p_sym_timeout = findViewById(R.id.lora_p2p_sym_timeout);

        lora_mode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    lora_lorap2p.setVisibility(View.GONE);
                    lora_lorawan.setVisibility(View.VISIBLE);
                } else {
                    lora_lorawan.setVisibility(View.GONE);
                    lora_lorap2p.setVisibility(View.VISIBLE);
                }
                loraWanEna = isChecked;
            }
        });
    }

    @Override
    protected void onInitialize(final Bundle savedInstanceState) {
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, makeIntentFilter());
        FeaturesActivity.devicePrefix = "RAK";
    }

    @Override
    protected void onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
        super.onStop();
    }

    @Override
    protected void onServiceBound(LoRaSettingsService.TemplateBinder binder) {
        // not used
    }

    @Override
    protected void onServiceUnbound() {
        // not used
    }

    @Override
    protected Class<? extends BleProfileService> getServiceClass() {
        return LoRaSettingsService.class;
    }

    @Override
    protected void setDefaultUI() {
        // todo clear the UI
    }

    @Override
    protected int getDefaultDeviceName() {
        return R.string.scanner_default_name;
    }

    @Override
    protected UUID getFilterUUID() {
        return null;
//        return LoRa_Settings_Manager.LORA_SERVICE_UUID;
    }

    @Override
    public void onServicesDiscovered(final BluetoothDevice device, final boolean optionalServicesFound) {
        thisMenu.findItem(R.id.connect).setTitle("");
        thisMenu.findItem(R.id.connect).setIcon(android.R.drawable.ic_delete);
        Button enaBt = findViewById(R.id.lora_readBT);
        enaBt.setEnabled(true);
        enaBt = findViewById(R.id.lora_writeBT);
        enaBt.setEnabled(true);
        enaBt = findViewById(R.id.lora_resetBT);
        enaBt.setEnabled(true);
    }

    @Override
    public void onDeviceDisconnected(final BluetoothDevice device) {
        super.onDeviceDisconnected(device);
        thisMenu.findItem(R.id.connect).setIcon(null);
        thisMenu.findItem(R.id.connect).setTitle(getString(R.string.action_connect));
        Objects.requireNonNull(getSupportActionBar()).setTitle(getString(R.string.collector_feature_title));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        Button enaBt = findViewById(R.id.lora_readBT);
        enaBt.setEnabled(false);
        enaBt = findViewById(R.id.lora_writeBT);
        enaBt.setEnabled(false);
        enaBt = findViewById(R.id.lora_resetBT);
        enaBt.setEnabled(false);
    }

    @Override
    protected int getAboutTextId() {
        return R.string.lora_lorawan_about_text;
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.collector_menu, menu);
        thisMenu = menu;
        return true;
    }

    @Override
    protected boolean onOptionsItemSelected(final int itemId) {
        if (itemId == R.id.connect) {
            onMenuConnectClicked();
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        // Disconnect from the device
        onMenuConnectClicked();
    }

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String action = intent.getAction();

            // todo update UI with received settings
            lora_dev_eui.setText(nodeDeviceEUI);
            lora_app_eui.setText(nodeAppEUI);
            lora_app_key.setText(nodeAppKey);
            lora_dev_addr.setText(nodeDeviceAddr);
            lora_nws_key.setText(nodeNwsKey);
            lora_apps_key.setText(nodeAppsKey);
            lora_otaa.setChecked(otaaEna);
            lora_adr.setChecked(adrEna);
            lora_pub_net.setChecked(publicNetwork);
            lora_duty_cycle.setChecked(dutyCycleEna);
            lora_send_repeat.setText(String.valueOf(sendRepeatTime));
            lora_nb_trials.setText(String.valueOf(nbTrials));
            lora_tx_pwr.setText(String.valueOf(txPower));
            lora_datarate.setText(String.valueOf(dataRate));
            lora_class.setText(String.valueOf(loraClass));
            lora_subband.setText(String.valueOf(subBandChannel));
            lora_join.setChecked(autoJoin);
            lora_app_port.setText(String.valueOf(appPort));
            lora_mode.setChecked(loraWanEna);
            lora_p2p_freq.setText(String.valueOf(p2pFrequency));
            lora_p2p_tx_power.setText(String.valueOf(p2pTxPower));
            lora_p2p_bw.setText(String.valueOf(p2pBW));
            lora_p2p_sf.setText(String.valueOf(p2pSF));
            lora_p2p_cr.setText(String.valueOf(p2pCR));
            lora_p2p_pre_len.setText(String.valueOf(p2pPreLen));
            lora_p2p_sym_timeout.setText(String.valueOf(p2pSymTimeout));
            if (loraWanEna) {
                lora_lorap2p.setVisibility(View.GONE);
                lora_lorawan.setVisibility(View.VISIBLE);
            } else {
                lora_lorawan.setVisibility(View.GONE);
                lora_lorap2p.setVisibility(View.VISIBLE);
            }
        }
    };

    private static IntentFilter makeIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(LoRaSettingsService.BROADCAST_DATA_RECVD);

        return intentFilter;
    }

    public void onClickLoRaWrite(View v) {
        // Send settings to device

        byte[] newSettings = prepareSettings(false);
//        byte[] newSettings = new byte[105];
//
//        newSettings[0] = -86;
//        newSettings[1] = 0x55;
//        nodeDeviceEUI = lora_dev_eui.getText().toString();
//        newSettings[2] = (byte)Integer.parseInt(nodeDeviceEUI.substring(0,2),16);
//        newSettings[3] = (byte)Integer.parseInt(nodeDeviceEUI.substring(2,4),16);
//        newSettings[4] = (byte)Integer.parseInt(nodeDeviceEUI.substring(4,6),16);
//        newSettings[5] = (byte)Integer.parseInt(nodeDeviceEUI.substring(6,8),16);
//        newSettings[6] = (byte)Integer.parseInt(nodeDeviceEUI.substring(8,10),16);
//        newSettings[7] = (byte)Integer.parseInt(nodeDeviceEUI.substring(10,12),16);
//        newSettings[8] = (byte)Integer.parseInt(nodeDeviceEUI.substring(12,14),16);
//        newSettings[9] = (byte)Integer.parseInt(nodeDeviceEUI.substring(14),16);
//
//        nodeAppEUI = lora_app_eui.getText().toString();
//        newSettings[10] = (byte)Integer.parseInt(nodeAppEUI.substring(0,2),16);
//        newSettings[11] = (byte)Integer.parseInt(nodeAppEUI.substring(2,4),16);
//        newSettings[12] = (byte)Integer.parseInt(nodeAppEUI.substring(4,6),16);
//        newSettings[13] = (byte)Integer.parseInt(nodeAppEUI.substring(6,8),16);
//        newSettings[14] = (byte)Integer.parseInt(nodeAppEUI.substring(8,10),16);
//        newSettings[15] = (byte)Integer.parseInt(nodeAppEUI.substring(10,12),16);
//        newSettings[16] = (byte)Integer.parseInt(nodeAppEUI.substring(12,14),16);
//        newSettings[17] = (byte)Integer.parseInt(nodeAppEUI.substring(14),16);
//
//        nodeAppKey = lora_app_key.getText().toString();
//        newSettings[18] = (byte)Integer.parseInt(nodeAppKey.substring(0,2),16);
//        newSettings[19] = (byte)Integer.parseInt(nodeAppKey.substring(2,4),16);
//        newSettings[20] = (byte)Integer.parseInt(nodeAppKey.substring(4,6),16);
//        newSettings[21] = (byte)Integer.parseInt(nodeAppKey.substring(6,8),16);
//        newSettings[22] = (byte)Integer.parseInt(nodeAppKey.substring(8,10),16);
//        newSettings[23] = (byte)Integer.parseInt(nodeAppKey.substring(10,12),16);
//        newSettings[24] = (byte)Integer.parseInt(nodeAppKey.substring(12,14),16);
//        newSettings[25] = (byte)Integer.parseInt(nodeAppKey.substring(14,16),16);
//        newSettings[26] = (byte)Integer.parseInt(nodeAppKey.substring(16,18),16);
//        newSettings[27] = (byte)Integer.parseInt(nodeAppKey.substring(18,20),16);
//        newSettings[28] = (byte)Integer.parseInt(nodeAppKey.substring(20,22),16);
//        newSettings[29] = (byte)Integer.parseInt(nodeAppKey.substring(22,24),16);
//        newSettings[30] = (byte)Integer.parseInt(nodeAppKey.substring(24,26),16);
//        newSettings[31] = (byte)Integer.parseInt(nodeAppKey.substring(26,28),16);
//        newSettings[32] = (byte)Integer.parseInt(nodeAppKey.substring(28,30),16);
//        newSettings[33] = (byte)Integer.parseInt(nodeAppKey.substring(30),16);
//
//        newSettings[34] = 0;
//        newSettings[35] = 0;
//
//        nodeDeviceAddr = lora_dev_addr.getText().toString();
//        newSettings[39] = (byte)Integer.parseInt(nodeDeviceAddr.substring(0,2),16);
//        newSettings[38] = (byte)Integer.parseInt(nodeDeviceAddr.substring(2,4),16);
//        newSettings[37] = (byte)Integer.parseInt(nodeDeviceAddr.substring(4,6),16);
//        newSettings[36] = (byte)Integer.parseInt(nodeDeviceAddr.substring(6,8),16);
//
//        nodeNwsKey = lora_nws_key.getText().toString();
//        newSettings[40] = (byte)Integer.parseInt(nodeNwsKey.substring(0,2),16);
//        newSettings[41] = (byte)Integer.parseInt(nodeNwsKey.substring(2,4),16);
//        newSettings[42] = (byte)Integer.parseInt(nodeNwsKey.substring(4,6),16);
//        newSettings[43] = (byte)Integer.parseInt(nodeNwsKey.substring(6,8),16);
//        newSettings[44] = (byte)Integer.parseInt(nodeNwsKey.substring(8,10),16);
//        newSettings[45] = (byte)Integer.parseInt(nodeNwsKey.substring(10,12),16);
//        newSettings[46] = (byte)Integer.parseInt(nodeNwsKey.substring(12,14),16);
//        newSettings[47] = (byte)Integer.parseInt(nodeNwsKey.substring(14,16),16);
//        newSettings[48] = (byte)Integer.parseInt(nodeNwsKey.substring(16,18),16);
//        newSettings[49] = (byte)Integer.parseInt(nodeNwsKey.substring(18,20),16);
//        newSettings[50] = (byte)Integer.parseInt(nodeNwsKey.substring(20,22),16);
//        newSettings[51] = (byte)Integer.parseInt(nodeNwsKey.substring(22,24),16);
//        newSettings[52] = (byte)Integer.parseInt(nodeNwsKey.substring(24,26),16);
//        newSettings[53] = (byte)Integer.parseInt(nodeNwsKey.substring(26,28),16);
//        newSettings[54] = (byte)Integer.parseInt(nodeNwsKey.substring(28,30),16);
//        newSettings[55] = (byte)Integer.parseInt(nodeNwsKey.substring(30),16);
//
//        nodeAppsKey = lora_apps_key.getText().toString();
//        newSettings[56] = (byte)Integer.parseInt(nodeAppsKey.substring(0,2),16);
//        newSettings[57] = (byte)Integer.parseInt(nodeAppsKey.substring(2,4),16);
//        newSettings[58] = (byte)Integer.parseInt(nodeAppsKey.substring(4,6),16);
//        newSettings[59] = (byte)Integer.parseInt(nodeAppsKey.substring(6,8),16);
//        newSettings[60] = (byte)Integer.parseInt(nodeAppsKey.substring(8,10),16);
//        newSettings[61] = (byte)Integer.parseInt(nodeAppsKey.substring(10,12),16);
//        newSettings[62] = (byte)Integer.parseInt(nodeAppsKey.substring(12,14),16);
//        newSettings[63] = (byte)Integer.parseInt(nodeAppsKey.substring(14,16),16);
//        newSettings[64] = (byte)Integer.parseInt(nodeAppsKey.substring(16,18),16);
//        newSettings[65] = (byte)Integer.parseInt(nodeAppsKey.substring(18,20),16);
//        newSettings[66] = (byte)Integer.parseInt(nodeAppsKey.substring(20,22),16);
//        newSettings[67] = (byte)Integer.parseInt(nodeAppsKey.substring(22,24),16);
//        newSettings[68] = (byte)Integer.parseInt(nodeAppsKey.substring(24,26),16);
//        newSettings[69] = (byte)Integer.parseInt(nodeAppsKey.substring(26,28),16);
//        newSettings[70] = (byte)Integer.parseInt(nodeAppsKey.substring(28,30),16);
//        newSettings[71] = (byte)Integer.parseInt(nodeAppsKey.substring(30),16);
//
//        newSettings[72] = lora_otaa.isChecked() ? (byte)1 : (byte)0;
//        newSettings[73] = lora_adr.isChecked() ? (byte)1 : (byte)0;
//        newSettings[74] = lora_pub_net.isChecked() ? (byte)1 : (byte)0;
//        newSettings[75] = lora_duty_cycle.isChecked() ? (byte)1 : (byte)0;
//
//        sendRepeatTime = Long.parseLong(lora_send_repeat.getText().toString());
//        String value = String.format("%08X",sendRepeatTime);
//        newSettings[79] = (byte)Integer.parseInt(value.substring(0,2),16);
//        newSettings[78] = (byte)Integer.parseInt(value.substring(2,4),16);
//        newSettings[77] = (byte)Integer.parseInt(value.substring(4,6),16);
//        newSettings[76] = (byte)Integer.parseInt(value.substring(6,8),16);
//
//        nbTrials = (byte)Integer.parseInt(lora_nb_trials.getText().toString());
//        newSettings[80] = nbTrials;
//        txPower = (byte)Integer.parseInt(lora_tx_pwr.getText().toString());
//        newSettings[81] = txPower;
//        dataRate = (byte)Integer.parseInt(lora_datarate.getText().toString());
//        newSettings[82] = dataRate;
//        loraClass = (byte)Integer.parseInt(lora_class.getText().toString());
//        newSettings[83] = loraClass;
//        subBandChannel = (byte)Integer.parseInt(lora_subband.getText().toString());
//        newSettings[84] = subBandChannel;
//
//        newSettings[85] = lora_join.isChecked() ? (byte)1 : (byte)0;
//
//        appPort = (byte)Integer.parseInt(lora_app_port.getText().toString());
//        newSettings[86] = appPort;
//
//        confirmedEna = lora_conf_msg.isChecked();
//        newSettings[87] = confirmedEna ? (byte)1 : (byte)0;
//
//        newSettings[88] = 1;
//
//        loraWanEna = lora_conf_msg.isChecked();
//        newSettings[89] = loraWanEna ? (byte)1 : (byte)0;
//
//        newSettings[90] = 0;
//        newSettings[91] = 0;
//
//        p2pFrequency = Long.parseLong(lora_p2p_freq.getText().toString());
//        value = String.format("%08X",p2pFrequency);
//        newSettings[95] = (byte)Integer.parseInt(value.substring(0,2),16);
//        newSettings[94] = (byte)Integer.parseInt(value.substring(2,4),16);
//        newSettings[93] = (byte)Integer.parseInt(value.substring(4,6),16);
//        newSettings[92] = (byte)Integer.parseInt(value.substring(6,8),16);
//
//        p2pTxPower = (byte)Integer.parseInt(lora_p2p_tx_power.getText().toString());
//        newSettings[96] = p2pTxPower;
//        p2pBW = (byte)Integer.parseInt(lora_p2p_bw.getText().toString());
//        newSettings[97] = p2pBW;
//        p2pSF = (byte)Integer.parseInt(lora_p2p_sf.getText().toString());
//        newSettings[98] = p2pSF;
//        p2pCR = (byte)Integer.parseInt(lora_p2p_cr.getText().toString());
//        newSettings[99] = p2pCR;
//        p2pPreLen = (byte)Integer.parseInt(lora_p2p_pre_len.getText().toString());
//        newSettings[100] = p2pPreLen;
//
//        newSettings[101] = 0;
//
//        p2pSymTimeout = Integer.parseInt(lora_p2p_sym_timeout.getText().toString());
//        value = String.format("%04X",p2pSymTimeout);
//        newSettings[103] = (byte)Integer.parseInt(value.substring(0,2),16);
//        newSettings[102] = (byte)Integer.parseInt(value.substring(2,4),16);
//

        Log.d(TAG, "Sending: " + new Data(newSettings));
        getService().writeSettings(new Data(newSettings));
    }

    @SuppressWarnings("unused")
    public void onClickLoRaRead(View v) {
        getService().readSettings();
    }

    public void onClickLoRaReset(View v) {
        // Send reset command to LoRa node
        byte[] newSettings = prepareSettings(true);
        Log.d(TAG, "Sending: " + new Data(newSettings));
        getService().writeSettings(new Data(newSettings));
    }

    byte[] prepareSettings(boolean reqReset)
    {
        byte[] newSettings = new byte[108];

        newSettings[0] = -86;
        newSettings[1] = 0x55;
        nodeDeviceEUI = lora_dev_eui.getText().toString();
        newSettings[2] = (byte)Integer.parseInt(nodeDeviceEUI.substring(0,2),16);
        newSettings[3] = (byte)Integer.parseInt(nodeDeviceEUI.substring(2,4),16);
        newSettings[4] = (byte)Integer.parseInt(nodeDeviceEUI.substring(4,6),16);
        newSettings[5] = (byte)Integer.parseInt(nodeDeviceEUI.substring(6,8),16);
        newSettings[6] = (byte)Integer.parseInt(nodeDeviceEUI.substring(8,10),16);
        newSettings[7] = (byte)Integer.parseInt(nodeDeviceEUI.substring(10,12),16);
        newSettings[8] = (byte)Integer.parseInt(nodeDeviceEUI.substring(12,14),16);
        newSettings[9] = (byte)Integer.parseInt(nodeDeviceEUI.substring(14),16);

        nodeAppEUI = lora_app_eui.getText().toString();
        newSettings[10] = (byte)Integer.parseInt(nodeAppEUI.substring(0,2),16);
        newSettings[11] = (byte)Integer.parseInt(nodeAppEUI.substring(2,4),16);
        newSettings[12] = (byte)Integer.parseInt(nodeAppEUI.substring(4,6),16);
        newSettings[13] = (byte)Integer.parseInt(nodeAppEUI.substring(6,8),16);
        newSettings[14] = (byte)Integer.parseInt(nodeAppEUI.substring(8,10),16);
        newSettings[15] = (byte)Integer.parseInt(nodeAppEUI.substring(10,12),16);
        newSettings[16] = (byte)Integer.parseInt(nodeAppEUI.substring(12,14),16);
        newSettings[17] = (byte)Integer.parseInt(nodeAppEUI.substring(14),16);

        nodeAppKey = lora_app_key.getText().toString();
        newSettings[18] = (byte)Integer.parseInt(nodeAppKey.substring(0,2),16);
        newSettings[19] = (byte)Integer.parseInt(nodeAppKey.substring(2,4),16);
        newSettings[20] = (byte)Integer.parseInt(nodeAppKey.substring(4,6),16);
        newSettings[21] = (byte)Integer.parseInt(nodeAppKey.substring(6,8),16);
        newSettings[22] = (byte)Integer.parseInt(nodeAppKey.substring(8,10),16);
        newSettings[23] = (byte)Integer.parseInt(nodeAppKey.substring(10,12),16);
        newSettings[24] = (byte)Integer.parseInt(nodeAppKey.substring(12,14),16);
        newSettings[25] = (byte)Integer.parseInt(nodeAppKey.substring(14,16),16);
        newSettings[26] = (byte)Integer.parseInt(nodeAppKey.substring(16,18),16);
        newSettings[27] = (byte)Integer.parseInt(nodeAppKey.substring(18,20),16);
        newSettings[28] = (byte)Integer.parseInt(nodeAppKey.substring(20,22),16);
        newSettings[29] = (byte)Integer.parseInt(nodeAppKey.substring(22,24),16);
        newSettings[30] = (byte)Integer.parseInt(nodeAppKey.substring(24,26),16);
        newSettings[31] = (byte)Integer.parseInt(nodeAppKey.substring(26,28),16);
        newSettings[32] = (byte)Integer.parseInt(nodeAppKey.substring(28,30),16);
        newSettings[33] = (byte)Integer.parseInt(nodeAppKey.substring(30),16);

        newSettings[34] = 0;
        newSettings[35] = 0;

        nodeDeviceAddr = lora_dev_addr.getText().toString();
        newSettings[39] = (byte)Integer.parseInt(nodeDeviceAddr.substring(0,2),16);
        newSettings[38] = (byte)Integer.parseInt(nodeDeviceAddr.substring(2,4),16);
        newSettings[37] = (byte)Integer.parseInt(nodeDeviceAddr.substring(4,6),16);
        newSettings[36] = (byte)Integer.parseInt(nodeDeviceAddr.substring(6,8),16);

        nodeNwsKey = lora_nws_key.getText().toString();
        newSettings[40] = (byte)Integer.parseInt(nodeNwsKey.substring(0,2),16);
        newSettings[41] = (byte)Integer.parseInt(nodeNwsKey.substring(2,4),16);
        newSettings[42] = (byte)Integer.parseInt(nodeNwsKey.substring(4,6),16);
        newSettings[43] = (byte)Integer.parseInt(nodeNwsKey.substring(6,8),16);
        newSettings[44] = (byte)Integer.parseInt(nodeNwsKey.substring(8,10),16);
        newSettings[45] = (byte)Integer.parseInt(nodeNwsKey.substring(10,12),16);
        newSettings[46] = (byte)Integer.parseInt(nodeNwsKey.substring(12,14),16);
        newSettings[47] = (byte)Integer.parseInt(nodeNwsKey.substring(14,16),16);
        newSettings[48] = (byte)Integer.parseInt(nodeNwsKey.substring(16,18),16);
        newSettings[49] = (byte)Integer.parseInt(nodeNwsKey.substring(18,20),16);
        newSettings[50] = (byte)Integer.parseInt(nodeNwsKey.substring(20,22),16);
        newSettings[51] = (byte)Integer.parseInt(nodeNwsKey.substring(22,24),16);
        newSettings[52] = (byte)Integer.parseInt(nodeNwsKey.substring(24,26),16);
        newSettings[53] = (byte)Integer.parseInt(nodeNwsKey.substring(26,28),16);
        newSettings[54] = (byte)Integer.parseInt(nodeNwsKey.substring(28,30),16);
        newSettings[55] = (byte)Integer.parseInt(nodeNwsKey.substring(30),16);

        nodeAppsKey = lora_apps_key.getText().toString();
        newSettings[56] = (byte)Integer.parseInt(nodeAppsKey.substring(0,2),16);
        newSettings[57] = (byte)Integer.parseInt(nodeAppsKey.substring(2,4),16);
        newSettings[58] = (byte)Integer.parseInt(nodeAppsKey.substring(4,6),16);
        newSettings[59] = (byte)Integer.parseInt(nodeAppsKey.substring(6,8),16);
        newSettings[60] = (byte)Integer.parseInt(nodeAppsKey.substring(8,10),16);
        newSettings[61] = (byte)Integer.parseInt(nodeAppsKey.substring(10,12),16);
        newSettings[62] = (byte)Integer.parseInt(nodeAppsKey.substring(12,14),16);
        newSettings[63] = (byte)Integer.parseInt(nodeAppsKey.substring(14,16),16);
        newSettings[64] = (byte)Integer.parseInt(nodeAppsKey.substring(16,18),16);
        newSettings[65] = (byte)Integer.parseInt(nodeAppsKey.substring(18,20),16);
        newSettings[66] = (byte)Integer.parseInt(nodeAppsKey.substring(20,22),16);
        newSettings[67] = (byte)Integer.parseInt(nodeAppsKey.substring(22,24),16);
        newSettings[68] = (byte)Integer.parseInt(nodeAppsKey.substring(24,26),16);
        newSettings[69] = (byte)Integer.parseInt(nodeAppsKey.substring(26,28),16);
        newSettings[70] = (byte)Integer.parseInt(nodeAppsKey.substring(28,30),16);
        newSettings[71] = (byte)Integer.parseInt(nodeAppsKey.substring(30),16);

        newSettings[72] = lora_otaa.isChecked() ? (byte)1 : (byte)0;
        newSettings[73] = lora_adr.isChecked() ? (byte)1 : (byte)0;
        newSettings[74] = lora_pub_net.isChecked() ? (byte)1 : (byte)0;
        newSettings[75] = lora_duty_cycle.isChecked() ? (byte)1 : (byte)0;

        sendRepeatTime = Long.parseLong(lora_send_repeat.getText().toString());
        String value = String.format("%08X",sendRepeatTime);
        newSettings[79] = (byte)Integer.parseInt(value.substring(0,2),16);
        newSettings[78] = (byte)Integer.parseInt(value.substring(2,4),16);
        newSettings[77] = (byte)Integer.parseInt(value.substring(4,6),16);
        newSettings[76] = (byte)Integer.parseInt(value.substring(6,8),16);

        nbTrials = (byte)Integer.parseInt(lora_nb_trials.getText().toString());
        newSettings[80] = nbTrials;
        txPower = (byte)Integer.parseInt(lora_tx_pwr.getText().toString());
        newSettings[81] = txPower;
        dataRate = (byte)Integer.parseInt(lora_datarate.getText().toString());
        newSettings[82] = dataRate;
        loraClass = (byte)Integer.parseInt(lora_class.getText().toString());
        newSettings[83] = loraClass;
        subBandChannel = (byte)Integer.parseInt(lora_subband.getText().toString());
        newSettings[84] = subBandChannel;

        newSettings[85] = lora_join.isChecked() ? (byte)1 : (byte)0;

        appPort = (byte)Integer.parseInt(lora_app_port.getText().toString());
        newSettings[86] = appPort;

        confirmedEna = lora_conf_msg.isChecked();
        newSettings[87] = confirmedEna ? (byte)1 : (byte)0;

        newSettings[88] = 1;

        loraWanEna = lora_conf_msg.isChecked();
        newSettings[89] = loraWanEna ? (byte)1 : (byte)0;

        newSettings[90] = 0;
        newSettings[91] = 0;

        p2pFrequency = Long.parseLong(lora_p2p_freq.getText().toString());
        value = String.format("%08X",p2pFrequency);
        newSettings[95] = (byte)Integer.parseInt(value.substring(0,2),16);
        newSettings[94] = (byte)Integer.parseInt(value.substring(2,4),16);
        newSettings[93] = (byte)Integer.parseInt(value.substring(4,6),16);
        newSettings[92] = (byte)Integer.parseInt(value.substring(6,8),16);

        p2pTxPower = (byte)Integer.parseInt(lora_p2p_tx_power.getText().toString());
        newSettings[96] = p2pTxPower;
        p2pBW = (byte)Integer.parseInt(lora_p2p_bw.getText().toString());
        newSettings[97] = p2pBW;
        p2pSF = (byte)Integer.parseInt(lora_p2p_sf.getText().toString());
        newSettings[98] = p2pSF;
        p2pCR = (byte)Integer.parseInt(lora_p2p_cr.getText().toString());
        newSettings[99] = p2pCR;
        p2pPreLen = (byte)Integer.parseInt(lora_p2p_pre_len.getText().toString());
        newSettings[100] = p2pPreLen;

        newSettings[101] = 0;

        p2pSymTimeout = Integer.parseInt(lora_p2p_sym_timeout.getText().toString());
        value = String.format("%04X",p2pSymTimeout);
        newSettings[103] = (byte)Integer.parseInt(value.substring(0,2),16);
        newSettings[102] = (byte)Integer.parseInt(value.substring(2,4),16);

        newSettings[104] = reqReset ? (byte)1 : (byte)0;
        newSettings[105] = 0;
        newSettings[106] = 0;
        newSettings[107] = 0;
        return newSettings;
    }
}
