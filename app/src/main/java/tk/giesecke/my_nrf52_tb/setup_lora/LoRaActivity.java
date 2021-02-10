/*
 * Copyright (c) 2015, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package tk.giesecke.my_nrf52_tb.setup_lora;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import no.nordicsemi.android.ble.data.Data;
import tk.giesecke.my_nrf52_tb.AppHelpFragment;
import tk.giesecke.my_nrf52_tb.R;
import tk.giesecke.my_nrf52_tb.profile.BleProfileService;
import tk.giesecke.my_nrf52_tb.profile.BleProfileServiceReadyActivity;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static java.lang.StrictMath.abs;
import static tk.giesecke.my_nrf52_tb.setup_lora.LoRaService.BROADCAST_DATA_RECVD;
import static tk.giesecke.my_nrf52_tb.uart.UARTService.SETTINGS_CLOSE;

/**
 * LoRa / LoRaWAN settings activity
 */
public class LoRaActivity extends BleProfileServiceReadyActivity<LoRaService.TemplateBinder> {

    private static final String TAG = "LoRaActivity";

    private static String jsonConfig;

    static String mmDevice;
    static Context appContext;

    public static String nodeDeviceEUI = "";
    public static String nodeAppEUI = "";
    public static String nodeAppKey = "";
    public static String nodeDeviceAddr = "";
    public static String nodeNwsKey = "";
    public static String nodeAppsKey = "";

    public static boolean otaaEna = false;
    public static boolean adrEna = false;
    public static boolean publicNetwork = false;
    public static boolean dutyCycleEna = false;

    public static long sendRepeatTime = 120000;
    public static byte nbTrials = 5;
    public static byte txPower = 0;
    public static byte dataRate = 3;
    public static byte loraClass = 0;
    public static byte subBandChannel = 1;
    public static byte appPort = 2;
    public static byte region = 1;

    public static boolean autoJoin = false;
    public static boolean confirmedEna = false;
    public static boolean loraWanEna = true;

    public static long p2pFrequency = 923300000;
    public static byte p2pTxPower = 22;
    public static byte p2pBW = 0;
    public static byte p2pSF = 7;
    public static byte p2pCR = 1;
    public static byte p2pPreLen = 8;
    public static int p2pSymTimeout = 0;

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

    SeekBar lora_datarate;
    SeekBar lora_tx_pwr;
    SeekBar lora_class;
    SeekBar lora_subband;
    SeekBar lora_app_port;
    SeekBar lora_nb_trials;
    NumberPicker lora_send_repeat;

    RadioButton lora_p2p_freq4_sel;
    RadioButton lora_p2p_freq7_sel;
    RadioButton lora_p2p_freq9_sel;

    NumberPicker lora_p2p_freq4;
    NumberPicker lora_p2p_freq7;
    NumberPicker lora_p2p_freq9;
    SeekBar lora_p2p_tx_power;
    RadioButton lora_p2p_bw125;
    RadioButton lora_p2p_bw250;
    RadioButton lora_p2p_bw500;
    SeekBar lora_p2p_sf;
    RadioButton lora_p2p_cr5;
    RadioButton lora_p2p_cr6;
    RadioButton lora_p2p_cr7;
    RadioButton lora_p2p_cr8;
    SeekBar lora_p2p_pre_len;
    EditText lora_p2p_sym_timeout;

    final String[] repeatValues = {"10", "20", "30", "40", "50", "60", "90", "120", "240", "360", "480", "600", "720", "840", "960", "1080", "1200", "1500", "1800", "2100", "2400", "2700", "3000", "3300", "3600"};
    final String[] freq400Val = new String[196];
    final String[] freq700Val = new String[116];
    final String[] freq900Val = new String[261];

    private Menu thisMenu;

    static File appFileStorage = null;

    @Override
    protected void onCreateView(final Bundle savedInstanceState) {
        // TODO modify the layout file(s). By default the activity shows only one field - the Heart Rate value as a sample
        setContentView(R.layout.activity_feature_lora);
        appContext = this;
        setGUI();

        appFileStorage = this.getExternalFilesDir(null);
        Log.d(TAG, "Apps file directory is " + appFileStorage.getAbsolutePath());
        Log.d(TAG, "Apps file directory is " + appFileStorage.getPath());
    }

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
        lora_otaa.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                showOTAA();
            } else {
                showABP();
            }
        });
        lora_pub_net = findViewById(R.id.lora_pub_net);
        lora_duty_cycle = findViewById(R.id.lora_duty_cycle);
        lora_adr = findViewById(R.id.lora_adr);
        lora_join = findViewById(R.id.lora_join);
        lora_conf_msg = findViewById(R.id.lora_conf_msg);

        lora_datarate = findViewById(R.id.lora_datarate);
        lora_datarate.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress,
                                                  boolean fromUser) {
                        TextView pwrText = findViewById(R.id.lora_datarate_num);
                        dataRate = (byte) (progress);

                        pwrText.setText(String.valueOf(dataRate));
                    }
                }
        );
        lora_tx_pwr = findViewById(R.id.lora_tx_pwr);
        lora_tx_pwr.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress,
                                                  boolean fromUser) {
                        TextView pwrText = findViewById(R.id.lora_tx_pwr_num);
                        txPower = (byte) (progress);

                        pwrText.setText(String.valueOf(txPower));
                    }
                }
        );
        lora_class = findViewById(R.id.lora_class);
        lora_class.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress,
                                                  boolean fromUser) {
                        TextView pwrText = findViewById(R.id.lora_class_num);
                        loraClass = (byte) (progress);

                        switch (loraClass) {
                            case 0:
                                pwrText.setText("A");
                                break;
                            case 1:
                                pwrText.setText("B");
                                break;
                            case 2:
                                pwrText.setText("C");
                                break;
                        }
                    }
                }
        );
        lora_subband = findViewById(R.id.lora_subband);
        lora_subband.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress,
                                                  boolean fromUser) {
                        TextView pwrText = findViewById(R.id.lora_subband_num);
                        subBandChannel = (byte) (progress + 1);

                        pwrText.setText(String.valueOf(subBandChannel));
                    }
                }
        );
        lora_app_port = findViewById(R.id.lora_app_port);
        lora_app_port.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress,
                                                  boolean fromUser) {
                        TextView pwrText = findViewById(R.id.lora_app_port_num);
                        appPort = (byte) (progress);

                        pwrText.setText(String.valueOf(abs(appPort)));
                    }
                }
        );
        lora_nb_trials = findViewById(R.id.lora_nb_trials);
        lora_nb_trials.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress,
                                                  boolean fromUser) {
                        TextView pwrText = findViewById(R.id.lora_nb_trials_num);
                        nbTrials = (byte) (progress + 1);

                        pwrText.setText(String.valueOf(nbTrials));
                    }
                }
        );
        lora_send_repeat = findViewById(R.id.lora_send_repeat);
        lora_send_repeat.setMinValue(0);
        lora_send_repeat.setMaxValue(24);
        lora_send_repeat.setDisplayedValues(repeatValues);
        lora_send_repeat.setOnValueChangedListener((numberPicker, i, i1) -> sendRepeatTime = Integer.parseInt(repeatValues[i]) * 1000);

        int idx = 0;
        for (int start = 433100; start <= 433500; start += 100) {
            freq400Val[idx] = Integer.toString(start);
            idx++;
        }
        for (int start = 470300; start <= 489300; start += 100) {
            freq400Val[idx] = Integer.toString(start);
            idx++;
        }
        Log.d(TAG, "Wrote " + idx + "values");
        idx = 0;
        for (int start = 775500; start <= 779900; start += 100) {
            freq700Val[idx] = Integer.toString(start);
            idx++;
        }
        for (int start = 863000; start <= 870000; start += 100) {
            freq700Val[idx] = Integer.toString(start);
            idx++;
        }
        Log.d(TAG, "Wrote " + idx + "values");
        idx = 0;
        for (int start = 902000; start <= 928000; start += 100) {
            freq900Val[idx] = Integer.toString(start);
            idx++;
        }
        Log.d(TAG, "Wrote " + idx + "values");

        lora_p2p_freq4 = findViewById(R.id.lora_p2p_freq4);
        lora_p2p_freq7 = findViewById(R.id.lora_p2p_freq7);
        lora_p2p_freq9 = findViewById(R.id.lora_p2p_freq9);
        lora_p2p_freq4.setMinValue(0);
        lora_p2p_freq4.setMaxValue(195);
        lora_p2p_freq4.setDisplayedValues(freq400Val);
        lora_p2p_freq4.setOnValueChangedListener((numberPicker, i, i1) -> p2pFrequency = Integer.parseInt(freq400Val[i]) * 1000);
        lora_p2p_freq7.setMinValue(0);
        lora_p2p_freq7.setMaxValue(115);
        lora_p2p_freq7.setDisplayedValues(freq700Val);
        lora_p2p_freq7.setOnValueChangedListener((numberPicker, i, i1) -> p2pFrequency = Integer.parseInt(freq700Val[i]) * 1000);
        lora_p2p_freq9.setMinValue(0);
        lora_p2p_freq9.setMaxValue(260);
        lora_p2p_freq9.setDisplayedValues(freq900Val);
        lora_p2p_freq9.setOnValueChangedListener((numberPicker, i, i1) -> p2pFrequency = Integer.parseInt(freq900Val[i]) * 1000);

        lora_p2p_freq4_sel = findViewById(R.id.lora_p2p_freq4_sel);
        lora_p2p_freq7_sel = findViewById(R.id.lora_p2p_freq7_sel);
        lora_p2p_freq9_sel = findViewById(R.id.lora_p2p_freq9_sel);
        lora_p2p_freq9_sel.setChecked(true);
        lora_p2p_freq4_sel.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                setFreqView(0, false);
            }
        });
        lora_p2p_freq7_sel.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                setFreqView(1, false);
            }
        });
        lora_p2p_freq9_sel.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                setFreqView(2, false);
            }
        });

        lora_p2p_tx_power = findViewById(R.id.lora_p2p_tx_power);
        lora_p2p_tx_power.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress,
                                                  boolean fromUser) {
                        TextView pwrText = findViewById(R.id.tv_pwr_num);
                        p2pTxPower = (byte) (progress);

                        pwrText.setText(String.valueOf(p2pTxPower));
                    }
                }
        );
        lora_p2p_bw125 = findViewById(R.id.bw_125);
        lora_p2p_bw250 = findViewById(R.id.bw_250);
        lora_p2p_bw500 = findViewById(R.id.bw_500);
        lora_p2p_sf = findViewById(R.id.sf_sel);
        lora_p2p_sf.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress,
                                                  boolean fromUser) {
                        TextView sfText = findViewById(R.id.sf_sel_num);
                        p2pSF = (byte) (progress + 7);

                        sfText.setText(String.valueOf(p2pSF));
                    }
                }
        );
        lora_p2p_cr5 = findViewById(R.id.cr_5);
        lora_p2p_cr6 = findViewById(R.id.cr_6);
        lora_p2p_cr7 = findViewById(R.id.cr_7);
        lora_p2p_cr8 = findViewById(R.id.cr_8);

        lora_p2p_pre_len = findViewById(R.id.lora_p2p_pre_len);
        lora_p2p_pre_len.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress,
                                                  boolean fromUser) {
                        TextView prelenText = findViewById(R.id.tv_prelen_num);
                        p2pPreLen = (byte) (progress + 1);

                        prelenText.setText(String.valueOf(p2pPreLen));
                    }
                }
        );
        lora_p2p_sym_timeout = findViewById(R.id.lora_p2p_sym_timeout);

        lora_mode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                lora_lorap2p.setVisibility(GONE);
                lora_lorawan.setVisibility(VISIBLE);
            } else {
                lora_lorawan.setVisibility(GONE);
                lora_lorap2p.setVisibility(VISIBLE);
            }
            loraWanEna = isChecked;
        });
    }

    @Override
    protected void onInitialize(final Bundle savedInstanceState) {
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, makeIntentFilter());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

    @Override
    protected void setDefaultUI() {
        // TODO clear your UI
        lora_dev_eui.setText("");
        lora_app_eui.setText("");
        lora_app_key.setText("");
        lora_dev_addr.setText("");
        lora_nws_key.setText("");
        lora_apps_key.setText("");
    }

    void updateUI() {
        // Update UI with received settings
        lora_dev_eui.setText(nodeDeviceEUI);
        lora_app_eui.setText(nodeAppEUI);
        lora_app_key.setText(nodeAppKey);
        lora_dev_addr.setText(nodeDeviceAddr);
        lora_nws_key.setText(nodeNwsKey);
        lora_apps_key.setText(nodeAppsKey);
        lora_otaa.setChecked(otaaEna);
        if (otaaEna) {
            showOTAA();
        } else {
            showABP();
        }
        lora_adr.setChecked(adrEna);
        lora_pub_net.setChecked(publicNetwork);
        lora_duty_cycle.setChecked(dutyCycleEna);
        String repeatVal = Long.toString(sendRepeatTime / 1000);
        for (int idx = 0; idx < 25; idx++) {
            if (repeatValues[idx].equalsIgnoreCase(repeatVal)) {
                lora_send_repeat.setValue(idx);
                break;
            }
        }
        lora_nb_trials.setProgress(nbTrials - 1);
        lora_tx_pwr.setProgress(txPower);
        lora_datarate.setProgress(dataRate);
        lora_class.setProgress(loraClass);
        TextView pwrText = findViewById(R.id.lora_class_num);
        switch (loraClass) {
            case 0:
                pwrText.setText("A");
                break;
            case 1:
                pwrText.setText("B");
                break;
            case 2:
                pwrText.setText("C");
                break;
        }
        lora_subband.setProgress(subBandChannel - 1);
        pwrText = findViewById(R.id.lora_subband_num);
        pwrText.setText(String.valueOf(subBandChannel));
        lora_join.setChecked(autoJoin);
        lora_app_port.setProgress(appPort);
        lora_mode.setChecked(loraWanEna);
        if (p2pFrequency < 700000000) {
            setFreqView(0, true);
        } else if (p2pFrequency < 900000000) {
            setFreqView(1, true);
        } else {
            setFreqView(2, true);
        }
        lora_p2p_tx_power.setProgress(p2pTxPower);
        switch (p2pBW) {
            case 0:
                lora_p2p_bw125.setChecked(true);
                break;
            case 1:
                lora_p2p_bw250.setChecked(true);
                break;
            case 2:
                lora_p2p_bw500.setChecked(true);
                break;
        }
        lora_p2p_sf.setProgress(p2pSF - 7);
        switch (p2pCR) {
            case 5:
                lora_p2p_cr5.setChecked(true);
                break;
            case 6:
                lora_p2p_cr6.setChecked(true);
                break;
            case 7:
                lora_p2p_cr7.setChecked(true);
                break;
            case 8:
                lora_p2p_cr8.setChecked(true);
                break;
        }
        lora_p2p_pre_len.setProgress(p2pPreLen - 1);
        lora_p2p_sym_timeout.setText(String.valueOf(p2pSymTimeout));
        if (loraWanEna) {
            lora_lorap2p.setVisibility(GONE);
            lora_lorawan.setVisibility(VISIBLE);
        } else {
            lora_lorawan.setVisibility(GONE);
            lora_lorap2p.setVisibility(VISIBLE);
        }
    }

    @Override
    protected int getLoggerProfileTitle() {
        return R.string.lora_feature_title;
    }

    @Override
    protected int getAboutTextId() {
        return R.string.lora_lorawan_about_text;
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.lora_menu, menu);
        thisMenu = menu;
        SharedPreferences checkPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Map<String, ?> allEntries = checkPreferences.getAll();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            Log.d(TAG, entry.getKey() + ": " + entry.getValue().toString());
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        } else if (item.getItemId() == R.id.action_about) {
            final AppHelpFragment fragment = AppHelpFragment.getInstance(R.string.lora_about_text);
            fragment.show(getSupportFragmentManager(), "help_fragment");
        } else if (item.getItemId() == R.id.connect) {
            onMenuConnectClicked();
        } else if (item.getItemId() == R.id.load) {
            if (readConfigFile()) {
                updateUI();
                updatePreferences();
            } else {
                reportSettingsMismatch(getString(R.string.lora_read_config_fail));
            }
        } else if (item.getItemId() == R.id.save) {
            if (saveConfigFile()) {
                updatePreferences();
            } else {
                reportSettingsMismatch(getString(R.string.lora_save_config_fail));
            }
        } else if (item.getItemId() == R.id.get) {
            SharedPreferences checkPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            Map<String, ?> allEntries = checkPreferences.getAll();
            for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                Log.d(TAG, entry.getKey() + ": " + entry.getValue().toString());
            }

            getPreferences();
            checkPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            allEntries = checkPreferences.getAll();
            for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                Log.d(TAG, entry.getKey() + ": " + entry.getValue().toString());
            }

            updateUI();
        }
        return true;
    }

    @Override
    protected int getDefaultDeviceName() {
        return R.string.template_default_name;
    }

    @Override
    protected UUID getFilterUUID() {
        // TODO this method may return the UUID of the service that is required to be in the advertisement packet of a device in order to be listed on the Scanner dialog.
        // If null is returned no filtering is done.
        return LoRaManager.LORA_SERVICE_UUID;
    }

    @Override
    protected Class<? extends BleProfileService> getServiceClass() {
        return LoRaService.class;
    }

    @Override
    protected void onServiceBound(final LoRaService.TemplateBinder binder) {
        // not used
    }

    @Override
    protected void onServiceUnbound() {
        // not used
    }

    @Override
    public void onServicesDiscovered(@NonNull final BluetoothDevice device, final boolean optionalServicesFound) {
        thisMenu.findItem(R.id.connect).setTitle("");
        thisMenu.findItem(R.id.connect).setIcon(android.R.drawable.ic_delete);
        Button enaBt = findViewById(R.id.lora_readBT);
        enaBt.setEnabled(true);
        enaBt = findViewById(R.id.lora_writeBT);
        enaBt.setEnabled(true);
        enaBt = findViewById(R.id.lora_resetBT);
        enaBt.setEnabled(true);
        enaBt = findViewById(R.id.lora_qrDevEuiBt);
        enaBt.setEnabled(true);
    }

    @Override
    public void onDeviceReady(@NonNull final BluetoothDevice device) {
        // Enable notifications
        getService().enableNotifications(LoRaManager.requiredCharacteristic);
    }

    @Override
    public void onDeviceDisconnected(@NonNull final BluetoothDevice device) {
        super.onDeviceDisconnected(device);
        thisMenu.findItem(R.id.connect).setIcon(R.drawable.ic_action_bluetooth);
        thisMenu.findItem(R.id.connect).setTitle(getString(R.string.action_connect));
        Objects.requireNonNull(getSupportActionBar()).setTitle(getString(R.string.lora_feature_title));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        Button enaBt = findViewById(R.id.lora_readBT);
        enaBt.setEnabled(false);
        enaBt = findViewById(R.id.lora_writeBT);
        enaBt.setEnabled(false);
        enaBt = findViewById(R.id.lora_resetBT);
        enaBt.setEnabled(false);
        enaBt = findViewById(R.id.lora_qrDevEuiBt);
        enaBt.setEnabled(false);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        // Disconnect from the device
        onMenuConnectClicked();
    }

    // Handling updates from the device
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {

            final String action = intent.getAction();
            if (action.equalsIgnoreCase(BROADCAST_DATA_RECVD)) {
                // Update UI with received settings
                updateUI();
            }
        }
    };

    void getPreferences() {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        loraWanEna = preferences.getBoolean("lora_mode", true);
        nodeAppEUI = preferences.getString("app_eui", "");
        nodeAppKey = preferences.getString("app_key", "");
        nodeNwsKey = preferences.getString("nws_key", "");
        nodeAppsKey = preferences.getString("apps_key", "");
        otaaEna = preferences.getBoolean("otaa_ena", true);
        publicNetwork = preferences.getBoolean("pub_net", true);
        dutyCycleEna = preferences.getBoolean("dc_ena", true);
        adrEna = preferences.getBoolean("adr_ena", true);
        confirmedEna = preferences.getBoolean("conf_ena", true);
        dataRate = (byte) preferences.getInt("dr_min", 0);
        txPower = (byte) preferences.getInt("tx_power", 0);
        loraClass = (byte) preferences.getInt("lora_class", 0);
        subBandChannel = (byte) preferences.getInt("sub_chan", 0);
        appPort = (byte) preferences.getInt("app_port", 0);
        nbTrials = (byte) preferences.getInt("nb_trials", 0);
        p2pFrequency = Long.parseLong(preferences.getString("p2p_freq", "923300000"));
        p2pTxPower = (byte) preferences.getInt("p2p_txpower", 22);
        p2pBW = (byte) preferences.getInt("p2p_bw", 0);
        p2pSF = (byte) preferences.getInt("p2p_sf", 7);
        p2pCR = (byte) preferences.getInt("p2p_cr", 0);
        p2pPreLen = (byte) preferences.getInt("p2p_pre", 8);
        p2pSymTimeout = Integer.parseInt(preferences.getString("p2p_timeout", "0"));
    }

    @SuppressLint("ApplySharedPref")
    void updatePreferences() {
        final SharedPreferences.Editor preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
        preferences.putBoolean("lora_mode", loraWanEna);
        preferences.putString("app_eui", nodeAppEUI);
        preferences.putString("app_key", nodeAppKey);
        preferences.putString("nws_key", nodeNwsKey);
        preferences.putString("apps_key", nodeAppsKey);
        preferences.putBoolean("otaa_ena", otaaEna);
        preferences.putBoolean("pub_net", publicNetwork);
        preferences.putBoolean("dc_ena", dutyCycleEna);
        preferences.putBoolean("adr_ena", adrEna);
        preferences.putBoolean("conf_ena", confirmedEna);
        preferences.putInt("dr_min", dataRate);
        preferences.putInt("tx_power", txPower);
        preferences.putInt("lora_class", loraClass);
        preferences.putInt("sub_chan", subBandChannel);
        preferences.putInt("app_port", appPort);
        preferences.putInt("nb_trials", nbTrials);
        preferences.putString("p2p_freq", p2pFrequency + "");
        preferences.putInt("p2p_txpower", p2pTxPower);
        preferences.putInt("p2p_bw", p2pBW);
        preferences.putInt("p2p_sf", p2pSF);
        preferences.putInt("p2p_cr", p2pCR);
        preferences.putInt("p2p_pre", p2pPreLen);
        preferences.putString("p2p_timeout", p2pSymTimeout + "");
        preferences.commit();
    }

    void showOTAA() {
        TextView title;
        CheckBox checkb;
        RelativeLayout.LayoutParams params;
        title = findViewById(R.id.tv_deveui);
        title.setVisibility(VISIBLE);
        title = findViewById(R.id.lora_dev_eui);
        title.setVisibility(VISIBLE);
        title = findViewById(R.id.tv_appeui);
        title.setVisibility(VISIBLE);
        title = findViewById(R.id.lora_app_eui);
        title.setVisibility(VISIBLE);
        title = findViewById(R.id.tv_appkey);
        title.setVisibility(VISIBLE);
        title = findViewById(R.id.lora_app_key);
        title.setVisibility(VISIBLE);
        title = findViewById(R.id.lora_qrDevEuiBt);
        title.setVisibility(VISIBLE);
        checkb = findViewById(R.id.lora_otaa);
        params = (RelativeLayout.LayoutParams) checkb.getLayoutParams();
        params.addRule(RelativeLayout.BELOW, R.id.lora_app_key);
        checkb = findViewById(R.id.lora_pub_net);
        params = (RelativeLayout.LayoutParams) checkb.getLayoutParams();
        params.addRule(RelativeLayout.BELOW, R.id.lora_app_key);
        checkb = findViewById(R.id.lora_duty_cycle);
        params = (RelativeLayout.LayoutParams) checkb.getLayoutParams();
        params.addRule(RelativeLayout.BELOW, R.id.lora_app_key);
        title = findViewById(R.id.tv_devaddr);
        title.setVisibility(GONE);
        title = findViewById(R.id.lora_dev_addr);
        title.setVisibility(GONE);
        title = findViewById(R.id.tv_nws_key);
        title.setVisibility(GONE);
        title = findViewById(R.id.lora_nws_key);
        title.setVisibility(GONE);
        title = findViewById(R.id.tv_apps_key);
        title.setVisibility(GONE);
        title = findViewById(R.id.lora_apps_key);
        title.setVisibility(GONE);
    }

    void showABP() {
        TextView title;
        CheckBox checkb;
        RelativeLayout.LayoutParams params;
        title = findViewById(R.id.tv_deveui);
        title.setVisibility(GONE);
        title = findViewById(R.id.lora_dev_eui);
        title.setVisibility(GONE);
        title = findViewById(R.id.tv_appeui);
        title.setVisibility(GONE);
        title = findViewById(R.id.lora_app_eui);
        title.setVisibility(GONE);
        title = findViewById(R.id.tv_appkey);
        title.setVisibility(GONE);
        title = findViewById(R.id.lora_app_key);
        title.setVisibility(GONE);
        title = findViewById(R.id.lora_qrDevEuiBt);
        title.setVisibility(GONE);
        checkb = findViewById(R.id.lora_otaa);
        params = (RelativeLayout.LayoutParams) checkb.getLayoutParams();
        params.addRule(RelativeLayout.BELOW, R.id.lora_apps_key);
        checkb = findViewById(R.id.lora_pub_net);
        params = (RelativeLayout.LayoutParams) checkb.getLayoutParams();
        params.addRule(RelativeLayout.BELOW, R.id.lora_apps_key);
        checkb = findViewById(R.id.lora_duty_cycle);
        params = (RelativeLayout.LayoutParams) checkb.getLayoutParams();
        params.addRule(RelativeLayout.BELOW, R.id.lora_apps_key);
        title = findViewById(R.id.tv_devaddr);
        title.setVisibility(VISIBLE);
        title = findViewById(R.id.lora_dev_addr);
        title.setVisibility(VISIBLE);
        title = findViewById(R.id.tv_nws_key);
        title.setVisibility(VISIBLE);
        title = findViewById(R.id.lora_nws_key);
        title.setVisibility(VISIBLE);
        title = findViewById(R.id.tv_apps_key);
        title.setVisibility(VISIBLE);
        title = findViewById(R.id.lora_apps_key);
        title.setVisibility(VISIBLE);
    }

    void setFreqView(int selection, boolean setChecked) {
        switch (selection) {
            case 0:
                lora_p2p_freq4.setVisibility(VISIBLE);
                lora_p2p_freq7.setVisibility(View.INVISIBLE);
                lora_p2p_freq9.setVisibility(View.INVISIBLE);
                if (setChecked) {
                    lora_p2p_freq4_sel.setChecked(true);
                    String repeatVal = Long.toString(p2pFrequency / 1000);
                    for (int idx = 0; idx < 196; idx++) {
                        if (freq400Val[idx].equalsIgnoreCase(repeatVal)) {
                            lora_p2p_freq4.setValue(idx);
                            break;
                        }
                    }
                }
                break;
            case 1:
                lora_p2p_freq4.setVisibility(View.INVISIBLE);
                lora_p2p_freq7.setVisibility(VISIBLE);
                lora_p2p_freq9.setVisibility(View.INVISIBLE);
                if (setChecked) {
                    lora_p2p_freq7_sel.setChecked(true);
                    String repeatVal = Long.toString(p2pFrequency / 1000);
                    for (int idx = 0; idx < 116; idx++) {
                        if (freq700Val[idx].equalsIgnoreCase(repeatVal)) {
                            lora_p2p_freq7.setValue(idx);
                            break;
                        }
                    }
                }
                break;
            default:
                lora_p2p_freq4.setVisibility(View.INVISIBLE);
                lora_p2p_freq7.setVisibility(View.INVISIBLE);
                lora_p2p_freq9.setVisibility(VISIBLE);
                if (setChecked) {
                    lora_p2p_freq9_sel.setChecked(true);
                    String repeatVal = Long.toString(p2pFrequency / 1000);
                    for (int idx = 0; idx < 261; idx++) {
                        if (freq900Val[idx].equalsIgnoreCase(repeatVal)) {
                            lora_p2p_freq9.setValue(idx);
                            break;
                        }
                    }
                }
                break;
        }
    }

    private static IntentFilter makeIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BROADCAST_DATA_RECVD);
        intentFilter.addAction(SETTINGS_CLOSE);
        return intentFilter;
    }

    public void onClickLoRaWrite(View v) {
        // Send settings to device

        byte[] newSettings = prepareSettings(false);

        Log.d(TAG, "Sending: " + new Data(newSettings));
        getService().writeSettings(new Data(newSettings));
    }

    public void onClickLoRaRead(View v) {
        getService().readSettings();
    }

    public void onClickLoRaReset(View v) {
        // Send reset command to LoRa node
        byte[] newSettings = prepareSettings(true);
        if (newSettings[0] != -86) {
            // Settings error. Do not send
            return;
        }
        Log.d(TAG, "Sending: " + new Data(newSettings));
        getService().writeSettings(new Data(newSettings));
    }

    public void onClickQrRead(View v) {
        // Read Device EUI from QR code
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setOrientationLocked(false);
        integrator.setPrompt("Scan a barcode");
        integrator.setCameraId(0);  // Use a specific camera of the device
        integrator.setBeepEnabled(true);
        integrator.setBarcodeImageEnabled(false);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

        Log.d(TAG, "requestCode: " + requestCode);

        if (result != null) {

            if (result.getContents() == null) {
                //cancel
                Log.e(TAG, "QR scan failed");
            } else {
                //Scanned successfully
                String scanResultString = result.getContents();
                lora_dev_eui.setText(scanResultString);
            }

        }
    }

    public static boolean readConfigFile() {
        File file = null;
        FileInputStream configStream = null;

        if (appFileStorage != null) {
            file = new File(appFileStorage, "lora_config.txt");
            if (file != null) {
                try {
                    configStream = new FileInputStream(file);
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
            }
        }
        byte[] data = new byte[(int) file.length()];
        try {
            if (configStream.read(data) != file.length()) {
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        jsonConfig = new String(data);
        Log.d(TAG, "File content: " + jsonConfig);

        try {
            JSONObject readConfigJson = new JSONObject(jsonConfig);
            if (readConfigJson.has("lora_mode")) {
                loraWanEna = readConfigJson.getBoolean("lora_mode");
            }
            if (readConfigJson.has("join")) {
                autoJoin = readConfigJson.getBoolean("join");
            }
            if (readConfigJson.has("app_eui")) {
                nodeAppEUI = readConfigJson.getString("app_eui");
            }
            if (readConfigJson.has("app_key")) {
                nodeAppKey = readConfigJson.getString("app_key");
            }
            if (readConfigJson.has("nws_key")) {
                nodeNwsKey = readConfigJson.getString("nws_key");
            }
            if (readConfigJson.has("apps_key")) {
                nodeAppsKey = readConfigJson.getString("apps_key");
            }
            if (readConfigJson.has("otaa_ena")) {
                otaaEna = readConfigJson.getBoolean("otaa_ena");
            }
            if (readConfigJson.has("pub_net")) {
                publicNetwork = readConfigJson.getBoolean("pub_net");
            }
            if (readConfigJson.has("dc_ena")) {
                dutyCycleEna = readConfigJson.getBoolean("dc_ena");
            }
            if (readConfigJson.has("adr_ena")) {
                adrEna = readConfigJson.getBoolean("adr_ena");
            }
            if (readConfigJson.has("conf_ena")) {
                confirmedEna = readConfigJson.getBoolean("conf_ena");
            }
            if (readConfigJson.has("dr_min")) {
                dataRate = (byte) readConfigJson.getInt("dr_min");
            }
            if (readConfigJson.has("tx_power")) {
                txPower = (byte) readConfigJson.getInt("tx_power");
            }
            if (readConfigJson.has("lora_class")) {
                loraClass = (byte) readConfigJson.getInt("lora_class");
            }
            if (readConfigJson.has("sub_chan")) {
                subBandChannel = (byte) readConfigJson.getInt("sub_chan");
            }
            if (readConfigJson.has("app_port")) {
                appPort = (byte) readConfigJson.getInt("app_port");
            }
            if (readConfigJson.has("nb_trials")) {
                nbTrials = (byte) readConfigJson.getInt("nb_trials");
            }
            if (readConfigJson.has("repeat")) {
                sendRepeatTime = readConfigJson.getInt("repeat");
            }
            if (readConfigJson.has("region")) {
                region = (byte) readConfigJson.getInt("region");
            }
            if (readConfigJson.has("p2p_freq")) {
                p2pFrequency = readConfigJson.getLong("p2p_freq");
            }
            if (readConfigJson.has("p2p_txpower")) {
                p2pTxPower = (byte) readConfigJson.getInt("p2p_txpower");
            }
            if (readConfigJson.has("p2p_bw")) {
                p2pBW = (byte) readConfigJson.getInt("p2p_bw");
            }
            if (readConfigJson.has("p2p_sf")) {
                p2pSF = (byte) readConfigJson.getInt("p2p_sf");
            }
            if (readConfigJson.has("p2p_cr")) {
                p2pCR = (byte) readConfigJson.getInt("p2p_cr");
            }
            if (readConfigJson.has("p2p_pre")) {
                p2pPreLen = (byte) readConfigJson.getInt("p2p_pre");
            }
            if (readConfigJson.has("p2p_timeout")) {
                p2pSymTimeout = readConfigJson.getInt("p2p_timeout");
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean saveConfigFile() {
        File file;
        FileOutputStream configStream = null;

        // Delete existing file
        if (appFileStorage != null) {
            file = new File(appFileStorage, "lora_config.txt");
            if (file != null) {
                if (file.exists()) {
                    if (!file.delete()) {
                        return false;
                    }
                }
            }
        }

        if (appFileStorage != null) {
            file = new File(appFileStorage, "lora_config.txt");
            if (file != null) {
                try {
                    configStream = new FileOutputStream(file);
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
            }
        }
        JSONObject saveConfigJson = new JSONObject();
        try {
            saveConfigJson.put("lora_mode", loraWanEna);
            saveConfigJson.put("join", autoJoin);
            saveConfigJson.put("app_eui", nodeAppEUI);
            saveConfigJson.put("app_key", nodeAppKey);
            saveConfigJson.put("nws_key", nodeNwsKey);
            saveConfigJson.put("apps_key", nodeAppsKey);
            saveConfigJson.put("otaa_ena", otaaEna);
            saveConfigJson.put("pub_net", publicNetwork);
            saveConfigJson.put("dc_ena", dutyCycleEna);
            saveConfigJson.put("adr_ena", adrEna);
            saveConfigJson.put("conf_ena", confirmedEna);
            saveConfigJson.put("dr_min", dataRate);
            saveConfigJson.put("tx_power", txPower);
            saveConfigJson.put("lora_class", loraClass);
            saveConfigJson.put("sub_chan", subBandChannel);
            saveConfigJson.put("app_port", appPort);
            saveConfigJson.put("nb_trials", nbTrials);
            saveConfigJson.put("repeat", sendRepeatTime);
            saveConfigJson.put("region", region);
            saveConfigJson.put("p2p_freq", p2pFrequency);
            saveConfigJson.put("p2p_txpower", p2pTxPower);
            saveConfigJson.put("p2p_bw", p2pBW);
            saveConfigJson.put("p2p_sf", p2pSF);
            saveConfigJson.put("p2p_cr", p2pCR);
            saveConfigJson.put("p2p_pre", p2pPreLen);
            saveConfigJson.put("p2p_timeout", p2pSymTimeout);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        byte[] data = saveConfigJson.toString().getBytes();
        try {
            configStream.write(data);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        jsonConfig = new String(data);
        Log.d(TAG, "File content: " + jsonConfig);
        return true;
    }

    byte[] prepareSettings(boolean reqReset) {
        byte[] newSettings = new byte[108];

        newSettings[0] = -86;
        newSettings[1] = 0x55;
        nodeDeviceEUI = lora_dev_eui.getText().toString();
        if (nodeDeviceEUI.length() != 16) {
            reportSettingsMismatch("Device EUI is too short. Please enter correct Device EUI");
            newSettings[0] = 0;
            return newSettings;
        }
        newSettings[2] = (byte) Integer.parseInt(nodeDeviceEUI.substring(0, 2), 16);
        newSettings[3] = (byte) Integer.parseInt(nodeDeviceEUI.substring(2, 4), 16);
        newSettings[4] = (byte) Integer.parseInt(nodeDeviceEUI.substring(4, 6), 16);
        newSettings[5] = (byte) Integer.parseInt(nodeDeviceEUI.substring(6, 8), 16);
        newSettings[6] = (byte) Integer.parseInt(nodeDeviceEUI.substring(8, 10), 16);
        newSettings[7] = (byte) Integer.parseInt(nodeDeviceEUI.substring(10, 12), 16);
        newSettings[8] = (byte) Integer.parseInt(nodeDeviceEUI.substring(12, 14), 16);
        newSettings[9] = (byte) Integer.parseInt(nodeDeviceEUI.substring(14), 16);

        nodeAppEUI = lora_app_eui.getText().toString();
        if (nodeAppEUI.length() != 16) {
            reportSettingsMismatch("App EUI is too short. Please enter correct App EUI");
            newSettings[0] = 0;
            return newSettings;
        }
        newSettings[10] = (byte) Integer.parseInt(nodeAppEUI.substring(0, 2), 16);
        newSettings[11] = (byte) Integer.parseInt(nodeAppEUI.substring(2, 4), 16);
        newSettings[12] = (byte) Integer.parseInt(nodeAppEUI.substring(4, 6), 16);
        newSettings[13] = (byte) Integer.parseInt(nodeAppEUI.substring(6, 8), 16);
        newSettings[14] = (byte) Integer.parseInt(nodeAppEUI.substring(8, 10), 16);
        newSettings[15] = (byte) Integer.parseInt(nodeAppEUI.substring(10, 12), 16);
        newSettings[16] = (byte) Integer.parseInt(nodeAppEUI.substring(12, 14), 16);
        newSettings[17] = (byte) Integer.parseInt(nodeAppEUI.substring(14), 16);

        nodeAppKey = lora_app_key.getText().toString();
        if (nodeAppKey.length() != 32) {
            reportSettingsMismatch("App Key is too short. Please enter correct App Key");
            newSettings[0] = 0;
            return newSettings;
        }
        newSettings[18] = (byte) Integer.parseInt(nodeAppKey.substring(0, 2), 16);
        newSettings[19] = (byte) Integer.parseInt(nodeAppKey.substring(2, 4), 16);
        newSettings[20] = (byte) Integer.parseInt(nodeAppKey.substring(4, 6), 16);
        newSettings[21] = (byte) Integer.parseInt(nodeAppKey.substring(6, 8), 16);
        newSettings[22] = (byte) Integer.parseInt(nodeAppKey.substring(8, 10), 16);
        newSettings[23] = (byte) Integer.parseInt(nodeAppKey.substring(10, 12), 16);
        newSettings[24] = (byte) Integer.parseInt(nodeAppKey.substring(12, 14), 16);
        newSettings[25] = (byte) Integer.parseInt(nodeAppKey.substring(14, 16), 16);
        newSettings[26] = (byte) Integer.parseInt(nodeAppKey.substring(16, 18), 16);
        newSettings[27] = (byte) Integer.parseInt(nodeAppKey.substring(18, 20), 16);
        newSettings[28] = (byte) Integer.parseInt(nodeAppKey.substring(20, 22), 16);
        newSettings[29] = (byte) Integer.parseInt(nodeAppKey.substring(22, 24), 16);
        newSettings[30] = (byte) Integer.parseInt(nodeAppKey.substring(24, 26), 16);
        newSettings[31] = (byte) Integer.parseInt(nodeAppKey.substring(26, 28), 16);
        newSettings[32] = (byte) Integer.parseInt(nodeAppKey.substring(28, 30), 16);
        newSettings[33] = (byte) Integer.parseInt(nodeAppKey.substring(30), 16);

        newSettings[34] = 0;
        newSettings[35] = 0;

        nodeDeviceAddr = lora_dev_addr.getText().toString();
        if (nodeDeviceAddr.length() != 8) {
            reportSettingsMismatch("Device address is too short. Please enter correct Device address");
            newSettings[0] = 0;
            return newSettings;
        }
        newSettings[39] = (byte) Integer.parseInt(nodeDeviceAddr.substring(0, 2), 16);
        newSettings[38] = (byte) Integer.parseInt(nodeDeviceAddr.substring(2, 4), 16);
        newSettings[37] = (byte) Integer.parseInt(nodeDeviceAddr.substring(4, 6), 16);
        newSettings[36] = (byte) Integer.parseInt(nodeDeviceAddr.substring(6, 8), 16);

        nodeNwsKey = lora_nws_key.getText().toString();
        if (nodeNwsKey.length() != 32) {
            reportSettingsMismatch("Network Session Key is too short. Please enter correct Network Session Key");
            newSettings[0] = 0;
            return newSettings;
        }
        newSettings[40] = (byte) Integer.parseInt(nodeNwsKey.substring(0, 2), 16);
        newSettings[41] = (byte) Integer.parseInt(nodeNwsKey.substring(2, 4), 16);
        newSettings[42] = (byte) Integer.parseInt(nodeNwsKey.substring(4, 6), 16);
        newSettings[43] = (byte) Integer.parseInt(nodeNwsKey.substring(6, 8), 16);
        newSettings[44] = (byte) Integer.parseInt(nodeNwsKey.substring(8, 10), 16);
        newSettings[45] = (byte) Integer.parseInt(nodeNwsKey.substring(10, 12), 16);
        newSettings[46] = (byte) Integer.parseInt(nodeNwsKey.substring(12, 14), 16);
        newSettings[47] = (byte) Integer.parseInt(nodeNwsKey.substring(14, 16), 16);
        newSettings[48] = (byte) Integer.parseInt(nodeNwsKey.substring(16, 18), 16);
        newSettings[49] = (byte) Integer.parseInt(nodeNwsKey.substring(18, 20), 16);
        newSettings[50] = (byte) Integer.parseInt(nodeNwsKey.substring(20, 22), 16);
        newSettings[51] = (byte) Integer.parseInt(nodeNwsKey.substring(22, 24), 16);
        newSettings[52] = (byte) Integer.parseInt(nodeNwsKey.substring(24, 26), 16);
        newSettings[53] = (byte) Integer.parseInt(nodeNwsKey.substring(26, 28), 16);
        newSettings[54] = (byte) Integer.parseInt(nodeNwsKey.substring(28, 30), 16);
        newSettings[55] = (byte) Integer.parseInt(nodeNwsKey.substring(30), 16);

        nodeAppsKey = lora_apps_key.getText().toString();
        if (nodeAppsKey.length() != 32) {
            reportSettingsMismatch("Application Session Key is too short. Please enter correct Application Session Key");
            newSettings[0] = 0;
            return newSettings;
        }
        newSettings[56] = (byte) Integer.parseInt(nodeAppsKey.substring(0, 2), 16);
        newSettings[57] = (byte) Integer.parseInt(nodeAppsKey.substring(2, 4), 16);
        newSettings[58] = (byte) Integer.parseInt(nodeAppsKey.substring(4, 6), 16);
        newSettings[59] = (byte) Integer.parseInt(nodeAppsKey.substring(6, 8), 16);
        newSettings[60] = (byte) Integer.parseInt(nodeAppsKey.substring(8, 10), 16);
        newSettings[61] = (byte) Integer.parseInt(nodeAppsKey.substring(10, 12), 16);
        newSettings[62] = (byte) Integer.parseInt(nodeAppsKey.substring(12, 14), 16);
        newSettings[63] = (byte) Integer.parseInt(nodeAppsKey.substring(14, 16), 16);
        newSettings[64] = (byte) Integer.parseInt(nodeAppsKey.substring(16, 18), 16);
        newSettings[65] = (byte) Integer.parseInt(nodeAppsKey.substring(18, 20), 16);
        newSettings[66] = (byte) Integer.parseInt(nodeAppsKey.substring(20, 22), 16);
        newSettings[67] = (byte) Integer.parseInt(nodeAppsKey.substring(22, 24), 16);
        newSettings[68] = (byte) Integer.parseInt(nodeAppsKey.substring(24, 26), 16);
        newSettings[69] = (byte) Integer.parseInt(nodeAppsKey.substring(26, 28), 16);
        newSettings[70] = (byte) Integer.parseInt(nodeAppsKey.substring(28, 30), 16);
        newSettings[71] = (byte) Integer.parseInt(nodeAppsKey.substring(30), 16);

        newSettings[72] = lora_otaa.isChecked() ? (byte) 1 : (byte) 0;
        newSettings[73] = lora_adr.isChecked() ? (byte) 1 : (byte) 0;
        newSettings[74] = lora_pub_net.isChecked() ? (byte) 1 : (byte) 0;
        newSettings[75] = lora_duty_cycle.isChecked() ? (byte) 1 : (byte) 0;

        String value = String.format("%08X", sendRepeatTime);
        newSettings[79] = (byte) Integer.parseInt(value.substring(0, 2), 16);
        newSettings[78] = (byte) Integer.parseInt(value.substring(2, 4), 16);
        newSettings[77] = (byte) Integer.parseInt(value.substring(4, 6), 16);
        newSettings[76] = (byte) Integer.parseInt(value.substring(6, 8), 16);

        newSettings[80] = nbTrials;
        newSettings[81] = txPower;
        newSettings[82] = dataRate;
        newSettings[83] = loraClass;
        newSettings[84] = subBandChannel;

        newSettings[85] = lora_join.isChecked() ? (byte) 1 : (byte) 0;

        newSettings[86] = appPort;

        confirmedEna = lora_conf_msg.isChecked();
        newSettings[87] = confirmedEna ? (byte) 1 : (byte) 0;

        newSettings[88] = 1;

        loraWanEna = lora_mode.isChecked();
        newSettings[89] = loraWanEna ? (byte) 1 : (byte) 0;

        newSettings[90] = 0;
        newSettings[91] = 0;

        value = String.format("%08X", p2pFrequency);
        newSettings[95] = (byte) Integer.parseInt(value.substring(0, 2), 16);
        newSettings[94] = (byte) Integer.parseInt(value.substring(2, 4), 16);
        newSettings[93] = (byte) Integer.parseInt(value.substring(4, 6), 16);
        newSettings[92] = (byte) Integer.parseInt(value.substring(6, 8), 16);

        newSettings[96] = p2pTxPower;
        RadioGroup buttonGroup = findViewById(R.id.bw_sel);
        int index = buttonGroup.indexOfChild(findViewById(buttonGroup.getCheckedRadioButtonId()));
        newSettings[97] = (byte) index;
        newSettings[98] = p2pSF;
        buttonGroup = findViewById(R.id.cr_sel);
        index = buttonGroup.indexOfChild(findViewById(buttonGroup.getCheckedRadioButtonId()));
        newSettings[99] = (byte) index;
        newSettings[100] = p2pPreLen;

        newSettings[101] = 0;

        p2pSymTimeout = Integer.parseInt(lora_p2p_sym_timeout.getText().toString());
        value = String.format("%04X", p2pSymTimeout);
        newSettings[103] = (byte) Integer.parseInt(value.substring(0, 2), 16);
        newSettings[102] = (byte) Integer.parseInt(value.substring(2, 4), 16);

        newSettings[104] = reqReset ? (byte) 1 : (byte) 0;
        newSettings[105] = 0;
        newSettings[106] = 0;
        newSettings[107] = 0;
        return newSettings;
    }

    void reportSettingsMismatch(String errorMsg) {
        Snackbar successMsg = Snackbar.make(findViewById(android.R.id.content), errorMsg,
                Snackbar.LENGTH_INDEFINITE);
        successMsg.setAction("CLOSE", view -> {
        });
        successMsg.setActionTextColor(getResources().getColor(android.R.color.holo_red_dark));
        View sbView = successMsg.getView();
        sbView.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
        TextView textView = sbView.findViewById(com.google.android.material.R.id.snackbar_text);
        textView.setTextColor(getResources().getColor(android.R.color.black));

        successMsg.show();
    }

}
