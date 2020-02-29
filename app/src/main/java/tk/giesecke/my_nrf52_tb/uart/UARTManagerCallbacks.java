package tk.giesecke.my_nrf52_tb.uart;

import android.bluetooth.BluetoothDevice;

import no.nordicsemi.android.ble.BleManagerCallbacks;

public interface UARTManagerCallbacks extends BleManagerCallbacks {

	void onDataReceived(final BluetoothDevice device, final String data);

	void onDataSent(final BluetoothDevice device, final String data);
}
