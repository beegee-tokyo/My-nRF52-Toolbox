package tk.giesecke.my_nrf52_tb.uart;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import java.util.UUID;

import no.nordicsemi.android.ble.WriteRequest;
import no.nordicsemi.android.log.LogContract;
import tk.giesecke.my_nrf52_tb.profile.BleManager;

public class UARTManager extends BleManager<UARTManagerCallbacks> {
	/** Nordic UART Service UUID */
	private final static UUID UART_SERVICE_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
	/** RX characteristic UUID */
	private final static UUID UART_RX_CHARACTERISTIC_UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E");
	/** TX characteristic UUID */
	private final static UUID UART_TX_CHARACTERISTIC_UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E");

	private BluetoothGattCharacteristic mRXCharacteristic, mTXCharacteristic;
	/**
	 * A flag indicating whether Long Write can be used. It's set to false if the UART RX
	 * characteristic has only PROPERTY_WRITE_NO_RESPONSE property and no PROPERTY_WRITE.
	 * If you set it to false here, it will never use Long Write.
	 *
	 * TODO change this flag if you don't want to use Long Write even with Write Request.
	 */
	private boolean mUseLongWrite = true;

	UARTManager(final Context context) {
		super(context);
	}

	@NonNull
	@Override
	protected no.nordicsemi.android.ble.BleManager.BleManagerGattCallback getGattCallback() {
		return mGattCallback;
	}

	/**
	 * BluetoothGatt callbacks for connection/disconnection, service discovery,
	 * receiving indication, etc.
	 */
	private final no.nordicsemi.android.ble.BleManager.BleManagerGattCallback mGattCallback = new no.nordicsemi.android.ble.BleManager.BleManagerGattCallback() {

		@Override
		protected void initialize() {
			setNotificationCallback(mTXCharacteristic)
					.with((device, data) -> {
						final String text = data.getStringValue(0);
						log(LogContract.Log.Level.APPLICATION, "\"" + text + "\" received");
						mCallbacks.onDataReceived(device, text);
					});
			requestMtu(260).enqueue();
			enableNotifications(mTXCharacteristic).enqueue();
		}

		@Override
		public boolean isRequiredServiceSupported(@NonNull final BluetoothGatt gatt) {
			final BluetoothGattService service = gatt.getService(UART_SERVICE_UUID);
			if (service != null) {
				mRXCharacteristic = service.getCharacteristic(UART_RX_CHARACTERISTIC_UUID);
				mTXCharacteristic = service.getCharacteristic(UART_TX_CHARACTERISTIC_UUID);
			}

			boolean writeRequest = false;
			boolean writeCommand = false;
			if (mRXCharacteristic != null) {
				final int rxProperties = mRXCharacteristic.getProperties();
				writeRequest = (rxProperties & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0;
				writeCommand = (rxProperties & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) > 0;

				// Set the WRITE REQUEST type when the characteristic supports it.
				// This will allow to send long write (also if the characteristic support it).
				// In case there is no WRITE REQUEST property, this manager will divide texts
				// longer then MTU-3 bytes into up to MTU-3 bytes chunks.
				if (writeRequest)
					mRXCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
				else
					mUseLongWrite = false;
			}

			return mRXCharacteristic != null && mTXCharacteristic != null && (writeRequest || writeCommand);
		}

		@Override
		protected void onDeviceDisconnected() {
			mRXCharacteristic = null;
			mTXCharacteristic = null;
			mUseLongWrite = true;
		}
	};

	// This has been moved to the service in BleManager v2.0.
	/*@Override
	protected boolean shouldAutoConnect() {
		// We want the connection to be kept
		return true;
	}*/

	/**
	 * Sends the given text to RX characteristic.
	 * @param text the text to be sent
	 */
	public void send(final String text) {
		// Are we connected?
		if (mRXCharacteristic == null)
			return;

		if (!TextUtils.isEmpty(text)) {
			final WriteRequest request = writeCharacteristic(mRXCharacteristic, text.getBytes())
					.with((device, data) -> log(LogContract.Log.Level.APPLICATION,
							"\"" + data.getStringValue(0) + "\" sent"));
			if (!mUseLongWrite) {
				// This will automatically split the long data into MTU-3-byte long packets.
				request.split();
			}
			request.enqueue();
		}
	}
}
