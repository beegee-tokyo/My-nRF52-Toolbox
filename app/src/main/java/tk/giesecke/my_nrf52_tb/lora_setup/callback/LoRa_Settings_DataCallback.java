package tk.giesecke.my_nrf52_tb.lora_setup.callback;

import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;

import no.nordicsemi.android.ble.callback.profile.ProfileDataCallback;
import no.nordicsemi.android.ble.data.Data;

public abstract class LoRa_Settings_DataCallback implements ProfileDataCallback, LoRa_Settings_CharacteristicCallback {
    @Override
    public void onDataReceived(@NonNull final BluetoothDevice device, @NonNull final Data data) {
        // Report the parsed value(s)
        onSampleValueReceived(device, data);
    }
}
