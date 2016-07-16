package com.hashan.bluetoothleserver;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.hashan.bluetoothleserver.Peripheral.BLEPeripheral;
import com.hashan.bluetoothleserver.Peripheral.CustomConstants;

public class MainActivity extends AppCompatActivity {

    BluetoothAdapter mBluetoothAdapter;
    private BLEPeripheral blePeri;
    private static final long SCAN_PERIOD = 10000;
    TextView value1Text;
    TextView value2Text;
    CheckBox advertiseCheckbox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        value1Text = (TextView)findViewById(R.id.value1Text);
        value2Text = (TextView)findViewById(R.id.value2Text);

        Button plusButton = (Button)findViewById(R.id.plusButton);
        Button minusButton = (Button)findViewById(R.id.minusButton);
        plusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String value = value2Text.getText().toString();
                int val = Integer.parseInt(value);
                val++;
                value2Text.setText(String.valueOf(val));
                changeValue();

            }
        });

        minusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String value = value2Text.getText().toString();
                int val = Integer.parseInt(value);
                val--;
                value2Text.setText(String.valueOf(val));
                changeValue();
            }
        });


        advertiseCheckbox = (CheckBox)findViewById(R.id.advertise_checkBox);


        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Bluetooth LE not supported", Toast.LENGTH_SHORT).show();
            finish();
        }



        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if(!mBluetoothAdapter.isEnabled()){
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }

        blePeri = new BLEPeripheral(this,mHandler);

        advertiseCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    TextView statusTextView = (TextView)findViewById(R.id.status_textView);
                    statusTextView.setText("advertising...");

                    blePeri.setService(value1Text.getText().toString(), value2Text.getText().toString());

                    blePeri.startAdvertise();

                }
                else{
                    TextView textView = (TextView)findViewById(R.id.status_textView);
                    textView.setText("disabled");
                    blePeri.stopAdvertise();
                }
            }
        });

    }


    private void changeValue(){
        BluetoothGattCharacteristic characteristic = blePeri.getNotifyCharacteristic();
        characteristic.setValue(value2Text.getText().toString().getBytes());
        blePeri.notifyValue(characteristic);
    }

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what){
                case CustomConstants.WRITE:
                    byte[] bytes = (byte[])msg.obj;
                    String readMessage = new String(bytes, 0, msg.arg1);
                    value1Text.setText(readMessage);
                    break;

            }
        }
    };


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }

        return super.onOptionsItemSelected(item);
    }
}
