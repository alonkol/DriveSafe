package com.drivesafe.drivesafe;

import android.bluetooth.BluetoothAdapter;
import android.os.AsyncTask;
import android.util.Log;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.UserConsent;
import com.microsoft.band.sensors.BandRRIntervalEvent;
import com.microsoft.band.sensors.BandRRIntervalEventListener;
import com.microsoft.band.sensors.HeartRateConsentListener;
import com.drivesafe.drivesafe.Auxiliary.*;

class RRIntervalTaker extends Thread {


    private static final String TAG = "RRInterval";
    private MainActivity mainActivity;

    // band
    private BandClient client = null;

    public RRIntervalTaker(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    public void run() {
        try {
            if (getConnectedBandClient()) {
                int hardwareVersion = Integer.parseInt(client.getHardwareVersion().await());
                if (hardwareVersion >= 20) {

                    if (client.getSensorManager().getCurrentHeartRateConsent() != UserConsent.GRANTED) {
                        client.getSensorManager().requestHeartRateConsent(mainActivity.mainActivityReference, new HeartRateConsentListener() {

                            @Override
                            public void userAccepted(boolean b) {

                            }
                        });
                    }

                    client.getSensorManager().registerRRIntervalEventListener(mRRIntervalEventListener);
                } else {
                    // appendToUI("The RR Interval sensor is not supported with your Band version. Microsoft Band 2 is required.\n");
                }
            } else {
                //appendToUI("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
            }
        } catch (BandException e) {
            String exceptionMessage = "";
            switch (e.getErrorType()) {
                case UNSUPPORTED_SDK_VERSION_ERROR:
                    exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.\n";
                    break;
                case SERVICE_ERROR:
                    exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.\n";
                    break;
                default:
                    exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
                    break;
            }
            // TODO: handle exception
            Log.e(this.TAG, exceptionMessage);

        } catch (Exception e) {

            // TODO: handle exception
            e.printStackTrace();
        }
    }

    private boolean getConnectedBandClient() throws InterruptedException, BandException {
        // check if bluetooth turned on
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            return false;
        } else {
            if (!mBluetoothAdapter.isEnabled()) {
                // TODO: Bluetooth is not enable
            }
        }

        if (client == null) {
            BandInfo[] devices = BandClientManager.getInstance().getPairedBands();
            if (devices.length == 0) {
                return false;
            }
            client = BandClientManager.getInstance().create(mainActivity.getBaseContext(), devices[0]);
        } else if (ConnectionState.CONNECTED == client.getConnectionState()) {
            return true;
        }

        return ConnectionState.CONNECTED == client.connect().await();
    }

    private BandRRIntervalEventListener mRRIntervalEventListener = new BandRRIntervalEventListener() {
        @Override
        public void onBandRRIntervalChanged(final BandRRIntervalEvent event) {
            try {
                if (event != null) {
                    //notify listener that band was detected
                    if (mainActivity.initBandDetectionListener != null)
                        mainActivity.initBandDetectionListener.onBandDetection();

                    //check if we can start now
                    //TODO: change 2 for an enum
                    if (mainActivity.bandIsReady && mainActivity.faceIsReady) {
                        if (mainActivity.initDetectionCompletion != null) {
                            mainActivity.initDetectionCompletion.onCompletion();
                        }
                    }
                    if (mainActivity.STATE == AppState.Active) {
                        Log.i(TAG, "Adding RR interval to history");
                        IntervalHistory.add(event.getInterval(), mainActivity.alertManager);
                    }

                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    };
}
