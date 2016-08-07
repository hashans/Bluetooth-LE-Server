package com.hashan.bluetoothleserver.Peripheral;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;

public class BLEPeripheral {

    BluetoothManager mBManager;
    BluetoothAdapter mBAdapter;

    BluetoothLeAdvertiser mLeAdvertiser;

    AdvertiseSettings.Builder settingsBuilder;
    AdvertiseData.Builder dataBuilder;

    BluetoothGattServer mGattServer;
    private HashSet<BluetoothDevice> mBluetoothDevices;

    Handler mHandler;


    public BLEPeripheral(Context context, Handler handler) {

        this.mHandler = handler;

        mBManager = (BluetoothManager)context.getSystemService(Context.BLUETOOTH_SERVICE);

        mBAdapter = mBManager.getAdapter();

        mBluetoothDevices = new HashSet<>();

        settingsBuilder = new AdvertiseSettings.Builder();
        settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);
        settingsBuilder.setConnectable(true);
        settingsBuilder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);

        dataBuilder = new AdvertiseData.Builder();
        mBAdapter.setName("SimplePeripheral");
        dataBuilder.setIncludeDeviceName(true);


        mGattServer = mBManager.openGattServer(context, new BluetoothGattServerCallback() {
            @Override
            public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (newState == BluetoothGatt.STATE_CONNECTED) {
                        mBluetoothDevices.add(device);
                        Log.v("GattServer", "Connected to device: " + device.getAddress());
                        Log.d("GattServer", "Gatt server connection state changed, new state ");

                    }
                    else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                        mBluetoothDevices.remove(device);
                        Log.v("GattServer", "Disconnected from device");
                    }
                }
                else{
                    mBluetoothDevices.remove(device);
                }


                super.onConnectionStateChange(device, status, newState);
            }

            @Override
            public void onServiceAdded(int status, BluetoothGattService service) {
                Log.d("GattServer", "Gatt server service was added.");

                super.onServiceAdded(status, service);
            }

            @Override
            public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
                Log.d("GantServer", "Device tried to read characteristic: " + characteristic.getUuid());
                Log.d("GantServer", "Value: " + Arrays.toString(characteristic.getValue()));

                mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset,
                        characteristic.getValue());


                super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            }

            @Override
            public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
                Log.d("GattServer", "We have received a write request for one of our hosted characteristics");

                mHandler.obtainMessage(CustomConstants.WRITE,value.length,-1,value).sendToTarget();

                mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset,
                        characteristic.getValue());

                super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
            }

            @Override
            public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
                Log.d("GattServer", "Gatt server descriptor was read.");
                super.onDescriptorReadRequest(device, requestId, offset, descriptor);
            }

            @Override
            public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
                Log.d("GattServer", "Gatt server descriptor was written.");
                super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
            }

            @Override
            public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
                Log.d("GattServer", "Gatt server on execute write.");
                super.onExecuteWrite(device, requestId, execute);
            }

            @Override
            public void onNotificationSent(BluetoothDevice device, int status) {
                Log.d("GattServer", "Status: "+ status + " Notification Sent to "+device.getName()+" : "+ device.getAddress());
                super.onNotificationSent(device, status);
            }
        });



    }


    public static String getAddress(){
        return BluetoothAdapter.getDefaultAdapter().getAddress();
    }

    private AdvertiseCallback mAdvCallback = new AdvertiseCallback() {

        @Override
        public void onStartFailure(int errorCode){
            Log.d("advertise","onStartFailure" + errorCode);
        }

        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect){
            Log.d("advertise","onStartSuccess");
        };
    };

    public BluetoothGattCharacteristic getNotifyCharacteristic(){
        return mGattServer.getService(UUID.fromString(CustomConstants.SERVICE_A))
                .getCharacteristic(UUID.fromString(CustomConstants.CHAR_READ2));
    }

    public void notifyValue(BluetoothGattCharacteristic characteristic){

        if(!mBluetoothDevices.isEmpty()){
            for (BluetoothDevice device: mBluetoothDevices) {
                mGattServer.notifyCharacteristicChanged(device,characteristic,false);
            }
        }
    }



    public void setService(String read1Data, String read2Data ){
        if(mGattServer == null){
            return;
        }

        stopAdvertise();



        BluetoothGattService previousService = mGattServer.getService(UUID.fromString(CustomConstants.SERVICE_A));


        if(previousService != null){
            mGattServer.removeService(previousService);
        }

        BluetoothGattCharacteristic read1Characteristic = new BluetoothGattCharacteristic(
                UUID.fromString(CustomConstants.CHAR_READ1),
                BluetoothGattCharacteristic.PROPERTY_READ  | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE
        );

        BluetoothGattCharacteristic read2Characteristic = new BluetoothGattCharacteristic(
                UUID.fromString(CustomConstants.CHAR_READ2),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ
        );





        read1Characteristic.setValue(read1Data.getBytes());
        read2Characteristic.setValue(read2Data.getBytes());



        BluetoothGattService AService = new BluetoothGattService(
                UUID.fromString(CustomConstants.SERVICE_A),
                BluetoothGattService.SERVICE_TYPE_PRIMARY);


        AService.addCharacteristic(read1Characteristic);
        AService.addCharacteristic(read2Characteristic);

        mGattServer.addService(AService);

    }

    public void startAdvertise()
    {
        if(mBAdapter == null)
            return;

        if (mLeAdvertiser == null)
            mLeAdvertiser = mBAdapter.getBluetoothLeAdvertiser();

        if(mLeAdvertiser == null)
            return;

        Log.d("Advertising","Advertising is called!");
        mLeAdvertiser.startAdvertising(settingsBuilder.build(),
                dataBuilder.build(), mAdvCallback);
    }

    public void stopAdvertise()
    {
        if(mLeAdvertiser != null)
            mLeAdvertiser.stopAdvertising(mAdvCallback);

        mLeAdvertiser = null;
    }


}