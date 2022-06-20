package com.STPL.samcomidynamoutility;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.magtek.mobile.android.mtlib.IMTCardData;
import com.magtek.mobile.android.mtlib.MTBankingEvent;
import com.magtek.mobile.android.mtlib.MTCardDataState;
import com.magtek.mobile.android.mtlib.MTConnectionState;
import com.magtek.mobile.android.mtlib.MTConnectionType;
import com.magtek.mobile.android.mtlib.MTDeviceConstants;
import com.magtek.mobile.android.mtlib.MTDeviceFeatures;
import com.magtek.mobile.android.mtlib.MTEMVDeviceConstants;
import com.magtek.mobile.android.mtlib.MTEMVEvent;
import com.magtek.mobile.android.mtlib.MTSCRA;
import com.magtek.mobile.android.mtlib.MTSCRAEvent;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;


public class IdynamoWorkerClass {
    private MTSCRA m_scra;
    private Handler mSelectionDialogController;
    private boolean[] mTypeChecked = new boolean[]{true, true, true};
    Activity activity;
    private boolean m_turnOffLEDPending;
    private int m_emvMessageFormat = 0;
    private String m_deviceAddress;
    private IDynamoInterface idynamoInterface;
    private static final String TAG = IdynamoWorkerClass.class.getSimpleName();
    private Handler m_scraHandler = new Handler(new SCRAHandlerCallback());
    private MTConnectionType m_connectionType = MTConnectionType.USB;
    private MTConnectionState m_connectionState = MTConnectionState.Disconnected;
    private boolean m_startTransactionActionPending;
    private Object m_syncEvent = null;
    private boolean m_emvMessageFormatRequestPending = false;
    private String m_syncData = "";
    private AlertDialog mSelectionDialog;
    private final HeadSetBroadCastReceiver m_headsetReceiver = new HeadSetBroadCastReceiver();
    private final NoisyAudioStreamReceiver m_noisyAudioStreamReceiver = new NoisyAudioStreamReceiver();

    public IdynamoWorkerClass(Activity activity, IDynamoInterface idynamoInterface) {
        this.activity = activity;
        registerUSBBroadCast();
        this.idynamoInterface = idynamoInterface;
        m_scra = new MTSCRA(activity, m_scraHandler);
        initDeviceConnection();
    }

    private void initDeviceConnection() {
        if (checkDeviceAttachORNot().size() > 0) {
            startDevice();
        } else {
            idynamoInterface.onDeviceNotFound();
        }
    }

    private void startDevice() {
        openDevice();
    }

    public void closeDevice() {
        if (m_scra.isDeviceConnected()) {
            Log.e("setDeviceStatus", "m_scra");
            m_scra.closeDevice();
        }
    }


    private void registerUSBBroadCast() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        activity.registerReceiver(mUsbReceiver, filter);
        activity.registerReceiver(m_headsetReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
        activity.registerReceiver(m_noisyAudioStreamReceiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
    }

    public void unRegisterReceiver() {
        Log.i(TAG, "*** App onDestroy");
        activity.unregisterReceiver(m_headsetReceiver);
        activity.unregisterReceiver(m_noisyAudioStreamReceiver);
        activity.unregisterReceiver(mUsbReceiver);
    }

    BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED == action) {
                Log.i(TAG, "BroadcastReceiver USB Connected");
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (checkDeviceAttachORNot().size() > 0) {
                    openDevice();
                }
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED == action) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null) {
                    Log.i(TAG, "BroadcastReceiver USB Disconnected");
                    idynamoInterface.removeDeviceCallback();
                }
            }
        }
    };

    public class NoisyAudioStreamReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            /* If the device is unplugged, this will immediately detect that action,
             * and close the device.
             */
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                if (m_connectionType == MTConnectionType.Audio) {
                    if (m_scra.isDeviceConnected()) {
                        closeDevice();
                    }
                }
            }
        }
    }

    public void startTransactionWithLED() {
        Log.e("startTransactionWithLED", "startTransactionWithLED");
        m_startTransactionActionPending = true;
        setLED(true);
    }

    public class HeadSetBroadCastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            try {
                String action = intent.getAction();

                if ((action.compareTo(Intent.ACTION_HEADSET_PLUG)) == 0)   //if the action match a headset one
                {
                    int headSetState = intent.getIntExtra("state", 0);      //get the headset state property
                    int hasMicrophone = intent.getIntExtra("microphone", 0);//get the headset microphone property

                    if ((headSetState == 1) && (hasMicrophone == 1))        //headset was unplugged & has no microphone
                    {
                    } else {
                        if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                            if (m_connectionType == MTConnectionType.Audio) {
                                if (m_scra.isDeviceConnected()) {
                                    closeDevice();
                                }
                            }
                        }
                    }

                }

            } catch (Exception ex) {

            }
        }
    }

    protected void OnDeviceResponse(String data) {
        idynamoInterface.onSendToDisplay("[Failed to connect to the device]");
        idynamoInterface.onSendToDisplay(data);
        notifySyncData(data);

        if (m_emvMessageFormatRequestPending) {
            m_emvMessageFormatRequestPending = false;

            byte[] emvMessageFormatResponseByteArray = TLVParser.getByteArrayFromHexString(data);

            if (emvMessageFormatResponseByteArray.length == 3) {
                if ((emvMessageFormatResponseByteArray[0] == 0) && (emvMessageFormatResponseByteArray[1] == 1)) {
                    m_emvMessageFormat = emvMessageFormatResponseByteArray[2];
                }
            }
        } else if (m_startTransactionActionPending) {
            m_startTransactionActionPending = false;

            startTransaction();
        }
    }

    public void startTransaction() {
        byte type = 0;

        if (mTypeChecked[0]) {
            type |= (byte) 0x01;
        }

        if (mTypeChecked[1]) {
            type |= (byte) 0x02;
        }

        if (mTypeChecked[2]) {
            type |= (byte) 0x04;
        }
        startTransactionWithOptions(type);
    }

    public void startTransactionWithOptions(byte cardType) {
        if (m_scra != null) {
            byte timeLimit = 0x3C;
            //byte cardType = 0x02;  // Chip Only
            //byte cardType = 0x03;  // MSR + Chip
            byte option = (isQuickChipEnabled() ? (byte) 0x80 : 00);
            byte[] amount = new byte[]{0x00, 0x00, 0x00, 0x00, 0x15, 0x00};
            byte transactionType = 0x00; // Purchase
            byte[] cashBack = new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
            byte[] currencyCode = new byte[]{0x08, 0x40};
            byte reportingOption = 0x02;  // All Status Changes
            String s = new String(amount);
            Log.e("amount", "total::" + s);
            idynamoInterface.onClearTransactionMessage();
            int result = m_scra.startTransaction(timeLimit, cardType, option, amount, transactionType, cashBack, currencyCode, reportingOption);
            idynamoInterface.onSendToDisplay("[Start Transaction] (Result=" + result + ")");
        }
    }

    protected boolean isQuickChipEnabled() {
        boolean enabled = false;
        return enabled;
    }

    public void setLED(boolean on) {
        if (m_scra != null) {
            if (on) {
                m_scra.sendCommandToDevice(MTDeviceConstants.SCRA_DEVICE_COMMAND_STRING_SET_LED_ON);
            } else {
                m_scra.sendCommandToDevice(MTDeviceConstants.SCRA_DEVICE_COMMAND_STRING_SET_LED_OFF);
            }
        }
    }

    protected void notifySyncData(String data) {
        if (m_syncEvent != null) {
            synchronized (m_syncEvent) {
                m_syncData = data;
                m_syncEvent.notifyAll();
            }
        }
    }

    public long openDevice() {
        Log.e("step 11", "00");
        m_deviceAddress = "";
        long result = -1;
        if (m_scra != null) {
            m_scra.closeDevice();
            Log.e("step 11", "11");
            Log.e("set Address", "address" + m_deviceAddress);
            m_scra.setConnectionType(MTConnectionType.USB);
            m_scra.setAddress(m_deviceAddress);
            m_scra.openDevice();
            Log.e("step 11", "22");
            result = 0;
            if (result != 0) {
                idynamoInterface.onSendToDisplay("[Failed to connect to the device]");
                // sendToDisplay("[Failed to connect to the device]");
                Log.e("step 11", "33");
            }
        }

        return result;
    }

    private HashMap<String, UsbDevice> checkDeviceAttachORNot() {
        ArrayList<String> selectionList = new ArrayList<String>();
        HashMap<String, UsbDevice> deviceList = new HashMap<>();
        UsbManager usbManager = (UsbManager) activity.getSystemService(Context.USB_SERVICE);
        if (usbManager != null) {
            deviceList = usbManager.getDeviceList();
        }
        return deviceList;
    }

    public void startTransactionWithTap() {
//        sendSetDateTimeCommand();
        mTypeChecked = new boolean[]{false, false, true};
        startTransactionWithLED();
    }

    public void startTransactionWithSwipe() {
//        sendSetDateTimeCommand();
        mTypeChecked = new boolean[]{true, false, false};
        startTransactionWithLED();
    }

    private void sendSetDateTimeCommand() {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Calendar now = Calendar.getInstance();
                int month = now.get(Calendar.MONTH) + 1;
                int day = now.get(Calendar.DAY_OF_MONTH);
                int hour = now.get(Calendar.HOUR_OF_DAY);
                int minute = now.get(Calendar.MINUTE);
                int second = now.get(Calendar.SECOND);
                int year = now.get(Calendar.YEAR) - 2008;
                Log.e("deviceTime", "times :::>" + now.getTime());
                String dateTimeString = String.format("%1$02x%2$02x%3$02x%4$02x%5$02x00%6$02x", month, day, hour, minute, second, year);
                Log.e("dateTimeString", "dateTimeString" + dateTimeString);
                String command = "49220000030C001C0000000000000000000000000000000000" + dateTimeString + "00000000";
                sendCommand(command);
            }
        });

    }

    public void startTransactionWithChip() {
//        sendSetDateTimeCommand();
        mTypeChecked = new boolean[]{false, true, false};
        startTransactionWithLED();
    }

    public void startTransactionWithAll() {
//        sendSetDateTimeCommand();
        mTypeChecked = new boolean[]{true, true, true};
        startTransactionWithLED();
    }

    private void displayMessage(final String message) {
        if (message != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    idynamoInterface.onTransactionMessage(message);
                }
            });
        }
    }

    protected void OnDisplayMessageRequest(byte[] data) {
        idynamoInterface.onSendToDisplay("[Display Message Request]");
        String message = TLVParser.getTextString(data, 0);
        idynamoInterface.onSendToDisplay(message);
        displayMessage(message);
    }


    private class SCRAHandlerCallback implements Handler.Callback {
        public boolean handleMessage(Message msg) {
            try {
                Log.i(TAG, "*** Callback " + msg.what);
                switch (msg.what) {
                    case MTSCRAEvent.OnDeviceConnectionStateChanged:
                        OnDeviceConnectionStateChanged((MTConnectionState) msg.obj);
                        break;
                    case MTSCRAEvent.OnCardDataStateChanged:
                        OnCardDataStateChanged((MTCardDataState) msg.obj);
                        break;
                    case MTSCRAEvent.OnDataReceived:
                        OnCardDataReceived((IMTCardData) msg.obj);
                        break;
                    case MTSCRAEvent.OnDeviceResponse:
                        OnDeviceResponse((String) msg.obj);
                        break;

                    case MTEMVEvent.OnTransactionStatus:
                        OnTransactionStatus((byte[]) msg.obj);
                        break;
                    case MTEMVEvent.OnDisplayMessageRequest:
                        OnDisplayMessageRequest((byte[]) msg.obj);
                        break;
                    case MTEMVEvent.OnUserSelectionRequest:
                        OnUserSelectionRequest((byte[]) msg.obj);
                        break;
                    case MTEMVEvent.OnARQCReceived:
                        OnARQCReceived((byte[]) msg.obj);
                        break;
                    case MTEMVEvent.OnTransactionResult:
                        OnTransactionResult((byte[]) msg.obj);
                        break;

                    case MTEMVEvent.OnEMVCommandResult:
                        OnEMVCommandResult((byte[]) msg.obj);
                        break;
                    case MTEMVEvent.OnDeviceExtendedResponse:
                        OnDeviceExtendedResponse((String) msg.obj);
                        break;
                   /* case MTBankingEvent.OnDeviceState:
                        OnDeviceState((byte[]) msg.obj);
                        break;
                    case MTBankingEvent.OnCardStatus:
                        OnCardStatus((byte[]) msg.obj);
                        break;
                    case MTBankingEvent.OnCardData:
                        OnCardData((byte[]) msg.obj);
                        break;
                    case MTBankingEvent.OnPINResponse:
                        OnPINResponse((byte[]) msg.obj);
                        break;
                    case MTBankingEvent.OnSignatureState:
                        OnSignatureState((byte[]) msg.obj);
                        break;
                    case MTBankingEvent.OnSignature:
                        OnSignature((byte[]) msg.obj);
                        break;
                    case MTBankingEvent.OnEncryptedDataState:
                        OnEncryptedDataState((byte[]) msg.obj);
                        break;
                    case MTBankingEvent.OnEncryptedData:
                        OnEncryptedData((byte[]) msg.obj);
                        break;*/
                }
            } catch (Exception ex) {
                Log.e("Exception", "card" + ex.getMessage());
            }

            return true;
        }
    }

    protected void OnCardStatus(byte[] data) {
        idynamoInterface.onSendToDisplay("[OnCardStatus]");
        idynamoInterface.onSendToDisplay(TLVParser.getHexString(data));
    }


    protected void OnEMVCommandResult(byte[] data) {

        idynamoInterface.onSendToDisplay("[EMV Command Result]");
        idynamoInterface.onSendToDisplay(TLVParser.getHexString(data));


        if (m_turnOffLEDPending) {
            m_turnOffLEDPending = false;

            setLED(false);
        }
    }

    protected void OnCardData(byte[] data) {
        idynamoInterface.onSendToDisplay("[OnCardData]");
        idynamoInterface.onSendToDisplay(TLVParser.getHexString(data));
    }

    protected void OnPINResponse(byte[] data) {
        idynamoInterface.onSendToDisplay("[OnPINResponse]");
        idynamoInterface.onSendToDisplay(TLVParser.getHexString(data));
    }

    protected void OnSignatureState(byte[] data) {

        idynamoInterface.onSendToDisplay("[OnSignatureState]");
        idynamoInterface.onSendToDisplay(TLVParser.getHexString(data));
    }

    protected void OnSignature(byte[] data) {
        idynamoInterface.onSendToDisplay("[OnSignature]");
        idynamoInterface.onSendToDisplay(TLVParser.getHexString(data));
    }

    protected void OnEncryptedDataState(byte[] data) {
        idynamoInterface.onSendToDisplay("[OnEncryptedDataState]");
        idynamoInterface.onSendToDisplay(TLVParser.getHexString(data));
    }

    protected void OnEncryptedData(byte[] data) {
        idynamoInterface.onSendToDisplay("[OnEncryptedData]");
        idynamoInterface.onSendToDisplay(TLVParser.getHexString(data));
    }

    protected void OnDeviceExtendedResponse(String data) {
        idynamoInterface.onSendToDisplay("[Device Extended Response]");
        idynamoInterface.onSendToDisplay(data);
    }


    protected void OnCardDataStateChanged(MTCardDataState cardDataState) {
        switch (cardDataState) {
            case DataNotReady:
                idynamoInterface.onSendToDisplay("[Card Data Not Ready]");
                break;
            case DataReady:
                idynamoInterface.onSendToDisplay("[Card Data Ready]");
                break;
            case DataError:
                idynamoInterface.onSendToDisplay("[Card Data Error]");
                break;
        }

    }

    protected void OnDeviceState(byte[] data) {
        idynamoInterface.onSendToDisplay("[OnDeviceState]");
        idynamoInterface.onSendToDisplay(TLVParser.getHexString(data));

    }

    protected void OnTransactionStatus(byte[] data) {
        idynamoInterface.onSendToDisplay("--------------------");
        idynamoInterface.onSendToDisplay("[Transaction Status]");
        idynamoInterface.onSendToDisplay(TLVParser.getHexString(data));
        Log.e("OnTransactionStatus", "TLVParser.getHexString(data)" + TLVParser.getHexString(data));
    }

    protected void OnCardDataReceived(IMTCardData cardData) {
        //clearDisplay();
        idynamoInterface.onSendToDisplay("[Raw Data]");
        idynamoInterface.onSendToDisplay(m_scra.getResponseData());
        idynamoInterface.onSendToDisplay("[Card Data]");
        idynamoInterface.onSendToDisplay(IDynamoUtils.getCardInfo(m_scra));
        idynamoInterface.onSendToDisplay("[TLV Payload]");
        idynamoInterface.onSendToDisplay(cardData.getTLVPayload());
        Log.e("on card Data Receiver", "on card ::: " + cardData);
    }

    protected void OnUserSelectionRequest(byte[] data) {
        idynamoInterface.onSendToDisplay("[User Selection Request]");
        idynamoInterface.onSendToDisplay(TLVParser.getHexString(data));
        processSelectionRequest(data);
    }

    protected ArrayList<String> getSelectionList(byte[] data, int offset) {
        ArrayList<String> selectionList = new ArrayList<String>();

        if (data != null) {
            int dataLen = data.length;

            if (dataLen >= offset) {
                int start = offset;

                for (int i = offset; i < dataLen; i++) {
                    if (data[i] == 0x00) {
                        int len = i - start;

                        if (len >= 0) {
                            selectionList.add(new String(data, start, len));
                        }
                        start = i + 1;
                    }
                }
            }
        }

        return selectionList;
    }

    protected void processSelectionRequest(byte[] data) {
        if (data != null) {
            int dataLen = data.length;

            if (dataLen > 2) {
                byte selectionType = data[0];
                long timeout = ((long) (data[1] & 0xFF) * 1000);

                ArrayList<String> selectionList = getSelectionList(data, 2);

                String selectionTitle = selectionList.get(0);

                selectionList.remove(0);

                int nSelections = selectionList.size();

                if (nSelections > 0) {
                    if (selectionType == MTEMVDeviceConstants.SELECTION_TYPE_LANGUAGE) {
                        for (int i = 0; i < nSelections; i++) {
                            byte[] code = selectionList.get(i).getBytes();
                            EMVLanguage language = EMVLanguage.GetLanguage(code);

                            if (language != null) {
                                selectionList.set(i, language.getName());
                            }
                        }
                    }
                    String[] selectionArray = selectionList.toArray(new String[selectionList.size()]);
                    mSelectionDialogController = new Handler();
                    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
                    dialogBuilder.setTitle(selectionTitle);
                    dialogBuilder.setNegativeButton(R.string.value_cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    mSelectionDialogController.removeCallbacksAndMessages(null);
                                    mSelectionDialogController = null;
                                    dialog.dismiss();
                                    setUserSelectionResult(MTEMVDeviceConstants.SELECTION_STATUS_CANCELLED, (byte) 0);
                                }
                            });

                    dialogBuilder.setItems(selectionArray,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    mSelectionDialogController.removeCallbacksAndMessages(null);
                                    mSelectionDialogController = null;
                                    setUserSelectionResult(MTEMVDeviceConstants.SELECTION_STATUS_COMPLETED, (byte) (which));
                                }
                            });

                    mSelectionDialog = dialogBuilder.show();
                    mSelectionDialogController.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mSelectionDialog.dismiss();
                            setUserSelectionResult(MTEMVDeviceConstants.SELECTION_STATUS_TIMED_OUT, (byte) 0);
                        }
                    }, timeout);
                }
            }
        }
    }

    public void setUserSelectionResult(byte status, byte selection) {
        if (m_scra != null) {
            idynamoInterface.onSendToDisplay("[Sending Selection Result] Status=" + status + " Selection=" + selection);
            m_scra.setUserSelectionResult(status, selection);
        }
    }


    protected void OnDeviceConnectionStateChanged(MTConnectionState deviceState) {
        setState(deviceState);
        //  updateDisplay();
        switch (deviceState) {
            case Disconnected:
                if (m_connectionType == MTConnectionType.Audio) {

                }
                break;
            case Connected:
                displayDeviceFeatures();
                if ((m_connectionType == MTConnectionType.USB) && (m_scra.isDeviceEMV() == false)) {
                    sendGetSecurityLevelCommand();    // Wake up swipe output channel for BulleT and Audio readers
                }
                Log.e("sendSetDateTimeCommand", "sendSetDateTimeCommand");
                sendSetDateTimeCommand();
                break;
            case Error:
                idynamoInterface.onDeviceConnectionError("[Device Connection State Error]");
                break;
            case Connecting:
                break;
            case Disconnecting:
                break;
        }
    }


    public void cancelTransaction() {
        if (m_scra != null) {
            m_scra.cancelTransaction();
        }
    }

    private void setState(MTConnectionState deviceState) {
        m_connectionState = deviceState;
        updateDisplay();
    }

    private void updateConnectionState(String resourceId) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                idynamoInterface.onDeviceConnectionStatus(resourceId);
            }
        });
    }

    private void updateDisplay() {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (m_connectionState == MTConnectionState.Connected) {
                    updateConnectionState(activity.getResources().getString(R.string.connected));
                } else if (m_connectionState == MTConnectionState.Connecting) {
                    updateConnectionState(activity.getResources().getString(R.string.connecting));
                } else if (m_connectionState == MTConnectionState.Disconnecting) {
                    updateConnectionState(activity.getResources().getString(R.string.disconnecting));
                } else if (m_connectionState == MTConnectionState.Disconnected) {
                    updateConnectionState(activity.getResources().getString(R.string.disconnected));
                }
            }
        });
    }

    private void displayDeviceFeatures() {
        if (m_scra != null) {

            MTDeviceFeatures features = m_scra.getDeviceFeatures();
            if (features != null) {
                StringBuilder infoSB = new StringBuilder();
                infoSB.append("[Device Features]\n");
                infoSB.append("Supported Types: " + (features.MSR ? "(MSR) " : "") + (features.Contact ? "(Contact) " : "") + (features.Contactless ? "(Contactless) " : "") + "\n");
                infoSB.append("MSR Power Saver: " + (features.MSRPowerSaver ? "Yes" : "No") + "\n");
                infoSB.append("Battery Backed Clock: " + (features.BatteryBackedClock ? "Yes" : "No"));
                idynamoInterface.onSendToDisplay(infoSB.toString());
            }
        }
    }

    private void sendGetSecurityLevelCommand() {
        Handler delayHandler = new Handler();
        delayHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                int status = sendCommand("1500");

            }
        }, 1000);
    }

    public int sendCommand(String command) {
        int result = MTSCRA.SEND_COMMAND_ERROR;
        if (m_scra != null) {
            idynamoInterface.onSendToDisplay("[Sending Command]");
            idynamoInterface.onSendToDisplay(command);
            result = m_scra.sendCommandToDevice(command);
        }
        return result;
    }

    protected void OnARQCReceived(byte[] data) {
        idynamoInterface.onSendToDisplay("[ARQC Received]");
        idynamoInterface.onSendToDisplay(TLVParser.getHexString(data));
        if (isQuickChipEnabled()) {
            return;
        }

        List<HashMap<String, String>> parsedTLVList = TLVParser.parseEMVData(data, true, "");
        if (parsedTLVList != null) {

//            get macKSNString DFDF54
            String macKSNString = TLVParser.getTagValue(parsedTLVList, "DFDF54");
            byte[] macKSN = TLVParser.getByteArrayFromHexString(macKSNString);


            String macEncryptionTypeString = TLVParser.getTagValue(parsedTLVList, "DFDF55");
            byte[] macEncryptionType = TLVParser.getByteArrayFromHexString(macEncryptionTypeString);


            String deviceSNString = TLVParser.getTagValue(parsedTLVList, "DFDF25");
            byte[] deviceSN = TLVParser.getByteArrayFromHexString(deviceSNString);
            idynamoInterface.onSendToDisplay("SN Bytes=" + deviceSNString);
            idynamoInterface.onSendToDisplay("SN String=" + TLVParser.getTextString(deviceSN, 2));
            //   String cardType = IDynamoUtils.getCardType(TLVParser.getTagValue(parsedTLVList, "DFDF52"));
            boolean approved = isTransactionApproved(data);
            byte[] response = null;
            if (m_emvMessageFormat == 0) {
                response = IDynamoUtils.buildAcquirerResponseFormat0(deviceSN, approved);
            } else if (m_emvMessageFormat == 1) {
                response = IDynamoUtils.buildAcquirerResponseFormat1(macKSN, macEncryptionType, deviceSN, approved);
            }

            idynamoInterface.onResponseDataFromARQC(TLVParser.getHexString(data));
            setAcquirerResponse(response);
        }
    }

    public void setAcquirerResponse(byte[] response) {
        if ((m_scra != null) && (response != null)) {
            idynamoInterface.onSendToDisplay("[Sending Acquirer Response]\n" + TLVParser.getHexString(response));
            m_scra.setAcquirerResponse(response);
        }
    }

    protected boolean isTransactionApproved(byte[] data) {
        boolean approved = true;
        return approved;
    }

    protected void OnTransactionResult(byte[] data) {
        idynamoInterface.onSendToDisplay("[Transaction Result]");
        idynamoInterface.onSendToDisplay(TLVParser.getHexString(data));

        if (data != null) {
            if (data.length > 0) {
                boolean signatureRequired = (data[0] != 0);

                int lenBatchData = data.length - 3;
                if (lenBatchData > 0) {
                    byte[] batchData = new byte[lenBatchData];

                    System.arraycopy(data, 3, batchData, 0, lenBatchData);
                    idynamoInterface.onSendToDisplay("(Parsed Batch Data)");
                    List<HashMap<String, String>> parsedTLVList = TLVParser.parseEMVData(batchData, false, "");
                    displayParsedTLV(parsedTLVList);

                    String cidString = TLVParser.getTagValue(parsedTLVList, "9F27");
                    byte[] cidValue = TLVParser.getByteArrayFromHexString(cidString);

                    boolean approved = false;

                    if (cidValue != null) {
                        if (cidValue.length > 0) {
                            if ((cidValue[0] & (byte) 0x40) != 0) {
                                approved = true;
                            }
                        }
                    }

                    if (approved) {
                        if (signatureRequired) {
                            //   displayMessage2("( Signature Required )");
                        } else {
                            //    displayMessage2("( No Signature Required )");
                        }
                    }
                }
            }
        }

        setLED(false);
    }

    private void displayParsedTLV(List<HashMap<String, String>> parsedTLVList) {
        if (parsedTLVList != null) {
            ListIterator<HashMap<String, String>> it = parsedTLVList.listIterator();

            while (it.hasNext()) {
                HashMap<String, String> map = it.next();

                String tagString = map.get("tag");
                //String lenString = map.get("len");
                String valueString = map.get("value");
                idynamoInterface.onSendToDisplay(" Tags:: " + tagString + "=" + valueString);
            }
        }
    }
}