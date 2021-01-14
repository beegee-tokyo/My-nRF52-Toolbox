package tk.giesecke.my_nrf52_tb.esp32_setup.callback;

import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;
import no.nordicsemi.android.ble.callback.profile.ProfileDataCallback;
import no.nordicsemi.android.ble.data.Data;

/**
 * This is a sample data callback, that's based on Heart Rate Measurement characteristic.
 * It parses the HR value and ignores other optional data for simplicity.
 * Check {@link no.nordicsemi.android.ble.common.callback.hr.HeartRateMeasurementDataCallback}
 * for full implementation.
 *
 * TODO Modify the content to parse your data.
 */
public abstract class ESP32_Settings_DataCallback implements ProfileDataCallback, ESP32_Settings_CharacteristicCallback {

	@Override
	public void onDataReceived(@NonNull final BluetoothDevice device, @NonNull final Data data) {
		// Report the parsed value(s)
		onSampleValueReceived(device, data);
	}
}
