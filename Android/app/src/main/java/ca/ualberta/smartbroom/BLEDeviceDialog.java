package ca.ualberta.smartbroom;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.bluecreation.melodysmart.MelodySmartDevice;

import java.util.ArrayList;
import java.util.List;

import ca.ualberta.smartbroom.conn.BLEDeviceConnection;

/**
 * Created by Jared on 2015-01-31.
 */
public class BLEDeviceDialog extends DialogFragment {

    List<BLEDevice> deviceList;
    BLEDeviceListAdapter adapter;
    ListView listView;

    private MelodySmartDevice device;
    private boolean scanning;

    public static final String TAG = "BLEDeviceDialog";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        deviceList = new ArrayList<BLEDevice>();
        adapter = new BLEDeviceListAdapter(getActivity(), R.layout.ble_device_list_item, deviceList);
        listView = new ListView(getActivity());
        listView.setOnItemClickListener(itemClickListener);
        listView.setAdapter(adapter);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setView(listView)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        BLEDeviceDialog.this.getDialog().cancel();
                    }
                });

        device = MelodySmartDevice.getInstance();
        device.init(getActivity().getApplicationContext());

        Dialog dlg = builder.create();
        scanLeDevice(true);

        return dlg;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        scanLeDevice(false);
    }

    private synchronized void scanLeDevice(final boolean enable) {
        scanning = enable;
        if (enable) {
            device.startLeScan(mLeScanCallback);
        } else {
            device.stopLeScan(mLeScanCallback);
        }
    }

    // Device scan callback.
    private final BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            final BLEDevice bleDevice = new BLEDevice(device.getName(), device.getAddress());
            Log.d(TAG, "Found device " + bleDevice);
            if (!deviceList.contains(bleDevice)) {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    public void run() {
                        deviceList.add(bleDevice);
                        adapter.notifyDataSetChanged();
                    }
                });
            }
        }
    };

    private AdapterView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            scanLeDevice(false);

            Log.d(TAG, "Selected device " + adapter.getItem(i));
            BLEDeviceConnection.getInstance().connect(adapter.getItem(i), getActivity().getApplicationContext());
            BLEDeviceDialog.this.dismiss();
        }
    };
}
