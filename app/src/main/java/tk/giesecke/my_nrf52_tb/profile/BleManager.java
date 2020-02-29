package tk.giesecke.my_nrf52_tb.profile;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import no.nordicsemi.android.ble.BleManagerCallbacks;

/**
 * The manager without logging to nRF Logger.
 *
 * @param <T> the callbacks class.
 */
public abstract class BleManager<T extends BleManagerCallbacks> extends no.nordicsemi.android.ble.BleManager<T> {

    /**
     * The manager constructor.
     * <p>
     * After constructing the manager, the callbacks object must be set with
     * {@link #setGattCallbacks(BleManagerCallbacks)}.
     *
     * @param context the context.
     */
    public BleManager(@NonNull final Context context) {
        super(context);
    }

}
