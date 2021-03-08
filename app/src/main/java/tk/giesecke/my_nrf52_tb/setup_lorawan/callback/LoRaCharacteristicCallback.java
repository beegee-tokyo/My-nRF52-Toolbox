package tk.giesecke.my_nrf52_tb.setup_lorawan.callback;

import android.bluetooth.BluetoothDevice;
import androidx.annotation.NonNull;

import no.nordicsemi.android.ble.data.Data;

/**
 * This class defines your characteristic API.
 * In this example (that is the HRM characteristic, which the template is based on), is notifying
 * with a value (Heart Rate). The single method just returns the value and ignores other
 * optional data from Heart Rate Measurement characteristic for simplicity.
 */
public interface LoRaCharacteristicCallback {

	/**
	 * Called when a value is received.
	 *
	 * @param device a device from which the value was obtained.
	 * @param value  the new value.
	 */
	void onSampleValueReceived(@NonNull final BluetoothDevice device, Data value);
}
