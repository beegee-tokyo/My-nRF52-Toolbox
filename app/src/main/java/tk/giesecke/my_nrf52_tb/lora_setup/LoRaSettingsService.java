package tk.giesecke.my_nrf52_tb.lora_setup;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import no.nordicsemi.android.ble.data.Data;
import tk.giesecke.my_nrf52_tb.FeaturesActivity;
import tk.giesecke.my_nrf52_tb.R;
import tk.giesecke.my_nrf52_tb.Toolbox;
import tk.giesecke.my_nrf52_tb.lora_setup.callback.LoRa_Settings_ManagerCallbacks;
import tk.giesecke.my_nrf52_tb.profile.BleManager;
import tk.giesecke.my_nrf52_tb.profile.BleProfileService;

public class LoRaSettingsService extends BleProfileService implements LoRa_Settings_ManagerCallbacks {
    public static final String BROADCAST_DATA_RECVD = "no.nordicsemi.android.nrftoolbox.COLL.BROADCAST_MEASUREMENT";
    public static final String EXTRA_DATA = "no.nordicsemi.android.nrftoolbox.COLL.EXTRA_DATA";

    private final static String ACTION_DISCONNECT = "no.nordicsemi.android.nrftoolbox.COLL.ACTION_DISCONNECT";

    private final static int NOTIFICATION_ID = 864;
    private final static int OPEN_ACTIVITY_REQ = 0;
    private final static int DISCONNECT_REQ = 1;

    private LoRa_Settings_Manager mManager;

    private final LocalBinder mBinder = new TemplateBinder();

    /**
     * This local binder is an interface for the bound activity to operate with the sensor.
     */
    class TemplateBinder extends LocalBinder {

        /**
         * Send parameters to the device
         * @param parameter
         * 			Parameters as String
         */
        void writeSettings(final String parameter) {
            mManager.writeLoRaSettings(parameter);
        }

        /**
         * Send parameters to the device
         * @param parameter
         * 			Parameters as String
         */
        void writeSettings(final Data parameter) {
            mManager.writeLoRaSettings(parameter);
        }
        /**
         * Read parameters received from device
         */
        void readSettings() {
            mManager.readLoRaSettings();
        }
    }

    @Override
    protected LocalBinder getBinder() {
        return mBinder;
    }

    @Override
    protected BleManager<LoRa_Settings_ManagerCallbacks> initializeManager() {
        return mManager = new LoRa_Settings_Manager(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        final IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_DISCONNECT);
        registerReceiver(mDisconnectActionBroadcastReceiver, filter);
    }

    @Override
    public void onDestroy() {
        // when user has disconnected from the sensor, we have to cancel the notification that we've created some milliseconds before using unbindService
        cancelNotification();
        unregisterReceiver(mDisconnectActionBroadcastReceiver);

        super.onDestroy();
    }

    @Override
    protected void onRebind() {
        // when the activity rebinds to the service, remove the notification
        cancelNotification();
    }

    @Override
    protected void onUnbind() {
        // when the activity closes we need to show the notification that user is connected to the sensor
        createNotification();
    }

    @Override
    public void onSampleValueReceived(@NonNull final BluetoothDevice device, final Data data) {
        final Intent broadcast = new Intent(BROADCAST_DATA_RECVD);
        broadcast.putExtra(EXTRA_DEVICE, getBluetoothDevice());
        broadcast.putExtra(EXTRA_DATA, data.getValue());
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
        // TODO do we need this for the collector connection?
    }

    /**
     * Creates the notification.
     *
     */
    private void createNotification() {
        final Intent parentIntent = new Intent(this, FeaturesActivity.class);
        parentIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        final Intent targetIntent = new Intent(this, LoRa_Settings_Activity.class);

        final Intent disconnect = new Intent(ACTION_DISCONNECT);
        final PendingIntent disconnectAction = PendingIntent.getBroadcast(this, DISCONNECT_REQ, disconnect, PendingIntent.FLAG_UPDATE_CURRENT);

        // both activities above have launchMode="singleTask" in the AndroidManifest.xml file, so if the task is already running, it will be resumed
        final PendingIntent pendingIntent = PendingIntent.getActivities(this, OPEN_ACTIVITY_REQ, new Intent[]{parentIntent, targetIntent}, PendingIntent.FLAG_UPDATE_CURRENT);
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this, Toolbox.CONNECTED_DEVICE_CHANNEL);
        builder.setContentIntent(pendingIntent);
        builder.setContentTitle(getString(R.string.app_name)).setContentText(getString(R.string.uart_notification_connected_message, getDeviceName()));
        builder.setSmallIcon(R.drawable.ic_stat_notify_ppg);
        builder.setShowWhen(false).setDefaults(0).setAutoCancel(true).setOngoing(true);
        builder.addAction(new NotificationCompat.Action(R.drawable.ic_action_bluetooth, getString(R.string.uart_notification_action_disconnect), disconnectAction));

        final Notification notification = builder.build();
        final NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Objects.requireNonNull(nm).notify(NOTIFICATION_ID, notification);
    }

    /**
     * Cancels the existing notification. If there is no active notification this method does nothing
     */
    private void cancelNotification() {
        final NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Objects.requireNonNull(nm).cancel(NOTIFICATION_ID);
    }

    /**
     * This broadcast receiver listens for {@link #ACTION_DISCONNECT} that may be fired by pressing Disconnect action button on the notification.
     */
    private final BroadcastReceiver mDisconnectActionBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
//			Logger.i(getLogSession(), "[Notification] Disconnect action pressed");
            if (isConnected())
                getBinder().disconnect();
            else
                stopSelf();
        }
    };
}
