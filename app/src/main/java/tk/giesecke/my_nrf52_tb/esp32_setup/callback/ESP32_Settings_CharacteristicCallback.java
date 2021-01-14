package tk.giesecke.my_nrf52_tb.esp32_setup.callback;

import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;
import no.nordicsemi.android.ble.data.Data;

/**
 * This class defines your characteristic API.
 * Unused
 */
public interface ESP32_Settings_CharacteristicCallback {

	/**
	 * Called when a value is received.
	 *
	 * @param device a device from which the value was obtained.
	 * @param value  the new value.
	 */
	void onSampleValueReceived(@NonNull final BluetoothDevice device, Data value);
}
