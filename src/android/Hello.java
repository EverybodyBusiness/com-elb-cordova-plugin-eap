package com.example.plugin;

import static android.content.ContentValues.TAG;

import org.apache.cordova.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import jpos.POSPrinterConst;
import com.bxl.config.editor.BXLConfigLoader;

import com.acs.smartcard.Features;
import com.acs.smartcard.PinModify;
import com.acs.smartcard.PinProperties;
import com.acs.smartcard.PinVerify;
import com.acs.smartcard.ReadKeyOption;
import com.acs.smartcard.Reader;
import com.acs.smartcard.Reader.OnStateChangeListener;
import com.acs.smartcard.TlvProperties;

public class Hello extends CordovaPlugin {
    private CallbackContext onPrinterResultCallbackContext = null;
    private CallbackContext onNfcResultCallbackContext = null;
    private static final String LOG_TAG = "Cordova Hello -kalen";
    private static BixolonPrinter bxlPrinter = null;

  private Reader mReader;
  private UsbManager mManager;
  private PendingIntent mPermissionIntent;
  private TransmitTask mTransmitTask=null;
  private static final String[] stateStrings = {"Unknown", "Absent",
          "Present", "Swallowed", "Powered", "Negotiable", "Specific"};
  private String command_with_aid = "00 A4 04 00 07 F0 01 02 03 04 05 06";
  private static final String ACTION_USB_PERMISSION = "com.elb.bookingtong_kiosk.USB_PERMISSION";

  private ArrayAdapter<String> mReaderAdapter;

  // private ArrayAdapter<String> mSlotAdapter;

  private Features mFeatures = new Features();
  private PinVerify mPinVerify = new PinVerify();
  private PinModify mPinModify = new PinModify();
  private ReadKeyOption mReadKeyOption = new ReadKeyOption();

  @Override
  public boolean execute(String action, JSONArray data, CallbackContext callbackContext) throws JSONException {
    Log.e("kalen ","execute action "+action.toString());
    if (action.equals("printerInit")) {
      this.onPrinterResultCallbackContext = callbackContext;
      try {
        this.bxlPrinter = new BixolonPrinter(this.cordova.getActivity().getApplicationContext());
        cordova.getThreadPool().execute(new Runnable() {
          @Override
          public void run() {
            // Log.e("kalen ","call printerOpen "+action.toString());
            if (bxlPrinter.printerOpen(BXLConfigLoader.DEVICE_BUS_USB, "BK3-3", "", true)) {
              // Log.e("kalen ","printerOpen success");
              PluginResult result = new PluginResult(PluginResult.Status.OK);
              onPrinterResultCallbackContext.sendPluginResult(result);
              // Log.e("kalen ","printerOpen return success");
            } else {
              PluginResult result = new PluginResult(PluginResult.Status.ERROR, "printerOpen error");
              onPrinterResultCallbackContext.sendPluginResult(result);
            }
          }
        });
        return true;
      } catch (Exception e) {
        PluginResult result = new PluginResult(PluginResult.Status.ERROR, e.getMessage());
        onPrinterResultCallbackContext.sendPluginResult(result);
        return false; // false 인가? true 인가?
      }
    }else if(action.equals("print")){
      // Log.e("kalen ","print comes");
      this.onPrinterResultCallbackContext = callbackContext; // 여기 저기서 동시에 불러서는 안된다!
      JSONObject obj = data.getJSONObject(0);
      JSONArray printData = obj.getJSONArray("data");
      // Log.e("kalen ","print data len "+printData.length());
      if(printData!=null){ // 주문서 출력
        cordova.getThreadPool().execute(new Runnable() {
          @Override
          public void run() {
            if(print(printData)){
              PluginResult result = new PluginResult(PluginResult.Status.OK);
              onPrinterResultCallbackContext.sendPluginResult(result);
            }else{
              PluginResult result = new PluginResult(PluginResult.Status.ERROR, "print error");
              onPrinterResultCallbackContext.sendPluginResult(result);
            }
          }
        });
      }
      return true;
    }else if(action.equals("nfcRead")){
      onNfcResultCallbackContext=callbackContext;
      Log.e("kalen ","기존 task 가 존재할까? " + (mTransmitTask!=null));
      if(mTransmitTask!=null){
        Log.e("kalen ","기존 task 가 존재함.");
        try {
          mTransmitTask.cancel(true);
        }catch(Exception ex){
          Log.e("kalen task cancel error in nfcRead" , ex.getMessage());
        }
      }
      cordova.getThreadPool().execute(new Runnable() {
        @Override
        public void run() {
          nfcRead();
        }
      });
      return true;
    }else if(action.equals("nfcClose")){
           closeNfcReader();
           return true;
         }
    return false;
  }

  private void closeNfcReader() {
        // Close reader
        mReader.close();
        this.cordova.getActivity().getApplicationContext().unregisterReceiver(mReceiver);
  }

  private boolean print(JSONArray data) {
    try {
      bxlPrinter.beginTransactionPrint();

      if (!bxlPrinter.posPrinter.getDeviceEnabled()) {
        return false;
      }

      for (int i = 0; i < data.length(); i++) {
        JSONObject tmp = data.optJSONObject(i);
        Log.d("ELB", "---- 루프 ----");
        Log.d("ELB", String.valueOf(tmp));

        String type = tmp.optString("type");
        Integer value = tmp.optInt("value");

        String strOption = EscapeSequence.getString(0);
        String str = "";

        switch (type) {
          case "0":       // 줄바꿈
            for (int idx = 0; idx < value; idx++) {
              str = str + "\n";
            }

            bxlPrinter.posPrinter.printNormal(POSPrinterConst.PTR_S_RECEIPT, strOption + str);

            break;

          case "1":       // 텍스트 출력
            Integer scale = tmp.optInt("scale");
            String align_text = tmp.optString("align");
            String text = tmp.optString("text");

            switch (align_text) {       // 긁자 정렬
              case "left":
                strOption += EscapeSequence.getString(4);
                break;
              case "center":
                strOption += EscapeSequence.getString(5);
                break;
              case "right":
                strOption += EscapeSequence.getString(6);
                break;
            }

            if (tmp.optBoolean("height")) {
              strOption += EscapeSequence.getString(16);
            }

            if (tmp.optBoolean("bold")) {       // 글자 굵게
              strOption += EscapeSequence.getString(7);
            }

            if (tmp.optBoolean("underline")) {      // 글자 밑줄
              strOption += EscapeSequence.getString(9);
            }

            switch (scale) {        // 글자 크기
              case 1:
                strOption += EscapeSequence.getString(17);
                strOption += EscapeSequence.getString(25);
                break;
              case 2:
                strOption += EscapeSequence.getString(18);
                strOption += EscapeSequence.getString(26);
                break;
              case 3:
                strOption += EscapeSequence.getString(19);
                strOption += EscapeSequence.getString(27);
                break;
              case 4:
                strOption += EscapeSequence.getString(20);
                strOption += EscapeSequence.getString(28);
                break;
              case 5:
                strOption += EscapeSequence.getString(21);
                strOption += EscapeSequence.getString(29);
                break;
              case 6:
                strOption += EscapeSequence.getString(22);
                strOption += EscapeSequence.getString(30);
                break;
              case 7:
                strOption += EscapeSequence.getString(23);
                strOption += EscapeSequence.getString(31);
                break;
              case 8:
                strOption += EscapeSequence.getString(24);
                strOption += EscapeSequence.getString(32);
                break;
              default:
                strOption += EscapeSequence.getString(17);
                strOption += EscapeSequence.getString(25);
                break;
            }

            str = text;

            Log.d("ELB", "strOption");
            Log.d("ELB", strOption);
            bxlPrinter.posPrinter.printNormal(POSPrinterConst.PTR_S_RECEIPT, strOption + str);
            break;

          case "2":       // 바코드
            int barcode_type;
            switch (tmp.optString("barcode_type")) {
              case "qr":      // 글자수 제한 X
                barcode_type = BixolonPrinter.BARCODE_TYPE_QRCODE;
                break;

              case "code128":     // 글자수 제한 O (42자)
                barcode_type = BixolonPrinter.BARCODE_TYPE_Code128;
                break;

              default:
                barcode_type = BixolonPrinter.BARCODE_TYPE_QRCODE;
                break;
            }

            String align_barcode = tmp.optString("align");
            int align;

            switch (align_barcode) {       // 바코드 정렬
              case "left":
                align = BixolonPrinter.ALIGNMENT_LEFT;
                break;
              case "center":
                align = BixolonPrinter.ALIGNMENT_CENTER;
                break;
              case "right":
                align = BixolonPrinter.ALIGNMENT_RIGHT;
                break;
              default:
                align = BixolonPrinter.ALIGNMENT_CENTER;
                break;
            }

            int hri;
            switch (tmp.optString("hri")) {       // 바코드 글자 출력 위치
              case "top":
                hri = BixolonPrinter.BARCODE_HRI_ABOVE;
                break;
              case "bottom":
                hri = BixolonPrinter.BARCODE_HRI_BELOW;
                break;
              case "none":
              default:
                hri = BixolonPrinter.BARCODE_HRI_NONE;
                break;
            }

            bxlPrinter.printBarcode(tmp.optString("text"), barcode_type, tmp.optInt("width"), tmp.optInt("height"), align, hri);
            break;

//                    case "3":       // image
//                        break;

          default:        // 잘못된 타입
            bxlPrinter.posPrinter.printNormal(POSPrinterConst.PTR_S_RECEIPT, strOption + "Not defined code type: " + type + "\n");
        }
      }

      bxlPrinter.cutPaper();

      bxlPrinter.endTransactionPrint();

      return true;

    } catch (Exception e) {
      return false;
    }
  }

  private class OpenTask extends AsyncTask<UsbDevice, Void, Exception> {

    @Override
    protected Exception doInBackground(UsbDevice... params) {

      Log.d(TAG, "OpenTask doInBackground: ");

      Exception result = null;

      try {

        mReader.open(params[0]);

      } catch (Exception e) {

        result = e;
      }

      return result;
    }

    @Override
    protected void onPostExecute(Exception result) {
      Log.d(TAG, "OpenTask onPostExecute: ");

      if (result != null) {

      } else {

        int numSlots = mReader.getNumSlots();
//                logMsg("Number of slots: " + numSlots);

        // Add slot items
        // mSlotAdapter.clear();
        // for (int i = 0; i < numSlots; i++) {
        //   mSlotAdapter.add(Integer.toString(i));
        // }

        // Remove all control codes
        mFeatures.clear();
      }
    }
  }

  private class CloseTask extends AsyncTask<Void, Void, Void> {

    @Override
    protected Void doInBackground(Void... params) {
      Log.d(TAG, "CloseTask doInBackground: ");
      mReader.close();
      return null;
    }

    @Override
    protected void onPostExecute(Void result) {
      Log.d(TAG, "CloseTask onPostExecute: ");
    }

  }
  private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

    public void onReceive(Context context, Intent intent) {

      Log.d(TAG, "BroadcastReceiver onReceive: ");

      String action = intent.getAction();

      if (ACTION_USB_PERMISSION.equals(action)) {

        synchronized (this) {

          UsbDevice device = (UsbDevice) intent
                  .getParcelableExtra(UsbManager.EXTRA_DEVICE);

          if (intent.getBooleanExtra(
                  UsbManager.EXTRA_PERMISSION_GRANTED, false)) {

            if (device != null) {
              // Open reader
              new OpenTask().execute(device);
            }
          } else {
          }
        }

      } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {

        synchronized (this) {

          // Update reader list
          mReaderAdapter.clear();
          for (UsbDevice device : mManager.getDeviceList().values()) {
            if (mReader.isSupported(device)) {
              mReaderAdapter.add(device.getDeviceName());
            }
          }

          UsbDevice device = (UsbDevice) intent
                  .getParcelableExtra(UsbManager.EXTRA_DEVICE);

          if (device != null && device.equals(mReader.getDevice())) {

            // Clear slot items
            // mSlotAdapter.clear();

            // Close reader
            new CloseTask().execute();
          }
        }
      }
    }
  };

  private boolean nfcRead(){
    mManager = (UsbManager) this.cordova.getActivity().getApplicationContext().getSystemService(Context.USB_SERVICE);
    // Initialize reader`
    mReader = new Reader(mManager);
    mReader.setOnStateChangeListener(new OnStateChangeListener() {

      @Override
      public void onStateChange(int slotNum, int prevState, int currState) {
        Log.d(TAG, "onCreate setOnStateChangeListener mReader onStateChange:");

        if (prevState < Reader.CARD_UNKNOWN
                || prevState > Reader.CARD_SPECIFIC) {
          prevState = Reader.CARD_UNKNOWN;
        }

        if (currState < Reader.CARD_UNKNOWN
                || currState > Reader.CARD_SPECIFIC) {
          currState = Reader.CARD_UNKNOWN;
        }

        // Create output string
        final String outputString = "Slot " + slotNum + ": "
                + stateStrings[prevState] + " -> "
                + stateStrings[currState];
        Log.d(TAG, "onStateChange: " + outputString);
        Log.d(TAG, "slotNum: " + slotNum);

        Log.d(TAG, "connect or disconnect 하고 대기: " + stateStrings[currState]);

        String now_present = "Present";

        if (now_present.equals(stateStrings[currState])) {
          // 명령어 전송
          sendCommand();
        }
      }
    });
    ///////////////////////////////////////////////
    // 아래부분 정리가 필요하다!
    mPermissionIntent = PendingIntent.getBroadcast(this.cordova.getActivity().getApplicationContext(), 0, new Intent(
            ACTION_USB_PERMISSION), 0);
    IntentFilter filter = new IntentFilter();
    filter.addAction(ACTION_USB_PERMISSION);
    filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
    this.cordova.getActivity().getApplicationContext().registerReceiver(mReceiver, filter);
    usbOpen();
    return true;
  }

  private void usbOpen() {
        // kalen 2024.01.20 nfc장비는 하나라고 가정한다.
        for (UsbDevice device : mManager.getDeviceList().values()) {
          Log.d(TAG, "device.getDeviceName() : " + device.getDeviceName());
          // If device name is found
          if (mReader.isSupported(device)) {
            // Request permission
            Log.d(TAG, "nfc device : " + device.getDeviceName());
            mManager.requestPermission(device,
                    mPermissionIntent);
            break;
          }
    }
  }

  private void sendCommand() {
    // Get slot number
//        int slotNum = mSlotSpinner.getSelectedItemPosition();
    int slotNum = 0;
    Log.d(TAG, "slotNum " + slotNum);
    Log.d(TAG, "Spinner.INVALID_POSITION " + Spinner.INVALID_POSITION);

    // Set parameters
    TransmitParams params = new TransmitParams();
    params.slotNum = slotNum;
    params.controlCode = Reader.IOCTL_CCID_ESCAPE;
    params.commandString = command_with_aid;
    Log.d(TAG, "params.commandString " + params.commandString);

    mTransmitTask = new TransmitTask();
    Log.e("kalen","mTransmitTask "+ mTransmitTask.toString());
    mTransmitTask.execute(params);
  }
  private class TransmitParams {

    public int slotNum;
    public int controlCode;
    public String commandString;
  }

  private class TransmitProgress {

    public int controlCode;
    public byte[] command;
    public int commandLength;
    public byte[] response;
    public int responseLength;
    public Exception e;
  }

  private class TransmitTask extends
          AsyncTask<TransmitParams, TransmitProgress, Void> {
    private byte[] toByteArray(String hexString) {

      int hexStringLength = hexString.length();
      byte[] byteArray = null;
      int count = 0;
      char c;
      int i;

      // Count number of hex characters
      for (i = 0; i < hexStringLength; i++) {

        c = hexString.charAt(i);
        if (c >= '0' && c <= '9' || c >= 'A' && c <= 'F' || c >= 'a'
                && c <= 'f') {
          count++;
        }
      }

      byteArray = new byte[(count + 1) / 2];
      boolean first = true;
      int len = 0;
      int value;
      for (i = 0; i < hexStringLength; i++) {

        c = hexString.charAt(i);
        if (c >= '0' && c <= '9') {
          value = c - '0';
        } else if (c >= 'A' && c <= 'F') {
          value = c - 'A' + 10;
        } else if (c >= 'a' && c <= 'f') {
          value = c - 'a' + 10;
        } else {
          value = -1;
        }

        if (value >= 0) {

          if (first) {

            byteArray[len] = (byte) (value << 4);

          } else {

            byteArray[len] |= value;
            len++;
          }

          first = !first;
        }
      }

      return byteArray;
    }

    @Override
    protected Void doInBackground(TransmitParams... params) {
      Log.d(TAG, "TransmitTask doInBackground: ");

      TransmitProgress progress = null;

      byte[] command = null;
      byte[] response = null;
      int responseLength = 0;
      int foundIndex = 0;
      int startIndex = 0;

      do {

        // Find carriage return
        foundIndex = params[0].commandString.indexOf('\n', startIndex);
        if (foundIndex >= 0) {
          Log.d(TAG, "TransmitTask doInBackground findIndex: " + foundIndex);
          command = toByteArray(params[0].commandString.substring(
                  startIndex, foundIndex));
        } else {
          Log.d(TAG, "TransmitTask doInBackground findIndex: " + foundIndex);
          command = toByteArray(params[0].commandString
                  .substring(startIndex));
        }


        // Set next start index
        startIndex = foundIndex + 1;
        Log.d(TAG, "TransmitTask doInBackground startIndex: " + startIndex);

        response = new byte[300];
        progress = new TransmitProgress();
        progress.controlCode = params[0].controlCode;
        try {

          if (params[0].controlCode < 0) {

            Log.d(TAG, "TransmitTask doInBackground Transmit APDU: " + progress.controlCode);
            // Transmit APDU
            responseLength = mReader.transmit(params[0].slotNum,
                    command, command.length, response,
                    response.length);

          } else {

            Log.d(TAG, "TransmitTask doInBackground Transmit control command: " + progress.controlCode);
            // Transmit control command
            responseLength = mReader.control(params[0].slotNum,
                    params[0].controlCode, command, command.length,
                    response, response.length);
          }

          progress.command = command;
          progress.commandLength = command.length;
          progress.response = response;
          progress.responseLength = responseLength;
          progress.e = null;

          Log.d(TAG, "TransmitTask doInBackground end try: ");

        } catch (Exception e) {
          Log.d(TAG, "TransmitTask doInBackground catch: " + e.toString());

          progress.command = null;
          progress.commandLength = 0;
          progress.response = null;
          progress.responseLength = 0;
          progress.e = e;
        }


        publishProgress(progress);

      } while (foundIndex >= 0 && !isCancelled());

      return null;
    }

    @Override
    protected void onProgressUpdate(TransmitProgress... progress) {
      Log.d(TAG, "TransmitTask onProgressUpdate: ");

      if (progress[0].e != null) {
        Log.d(TAG, "ERROR :" + progress[0].e);

//                if (!String.valueOf(progress[0].e).contains("CCID Error: -1")) {
//                    int RESULT_FAILED = 3;
//                    returnResultToKiosk(false, progress[0].e.toString(), "", RESULT_FAILED);
//                }
        // 부킹통 으로 전송 하고 종료

      } else {
        String final_command = toHexStringWithLength(progress[0].command, progress[0].commandLength);
        String final_response = toHexStringWithLength(progress[0].response, progress[0].responseLength);
        Log.d(TAG, "onProgressUpdate progress[0].command: " + final_command);
        Log.d(TAG, "onProgressUpdate progress[0].response: " + final_response);
        if (progress[0].response != null
                && progress[0].responseLength > 0) {

          int controlCode;
          int i;

          // Show control codes for IOCTL_GET_FEATURE_REQUEST
          if (progress[0].controlCode == Reader.IOCTL_GET_FEATURE_REQUEST) {

            mFeatures.fromByteArray(progress[0].response,
                    progress[0].responseLength);

            for (i = Features.FEATURE_VERIFY_PIN_START; i <= Features.FEATURE_CCID_ESC_COMMAND; i++) {

              controlCode = mFeatures.getControlCode(i);
              if (controlCode >= 0) {
//                                logMsg("Control Code: " + controlCode + " ("
//                                        + featureStrings[i] + ")");
              }
            }
          }


          controlCode = mFeatures
                  .getControlCode(Features.FEATURE_IFD_PIN_PROPERTIES);

          if (controlCode >= 0
                  && progress[0].controlCode == controlCode) {

            PinProperties pinProperties = new PinProperties(
                    progress[0].response,
                    progress[0].responseLength);
          }

          controlCode = mFeatures
                  .getControlCode(Features.FEATURE_GET_TLV_PROPERTIES);

          if (controlCode >= 0
                  && progress[0].controlCode == controlCode) {

            TlvProperties readerProperties = new TlvProperties(
                    progress[0].response,
                    progress[0].responseLength);

            Object property;
//                        logMsg("TLV Properties:");
            for (i = TlvProperties.PROPERTY_wLcdLayout; i <= TlvProperties.PROPERTY_wIdProduct; i++) {

              property = readerProperties.getProperty(i);
              if (property instanceof Integer) {
//                                logMsg(propertyStrings[i] + ": "
//                                        + toHexString((Integer) property));
              } else if (property instanceof String) {
//                                logMsg(propertyStrings[i] + ": " + property);
              }
            }
          }

          // 부킹통 으로 전송 하고 종료
          Log.d(TAG, "final_response: " + final_response);
          if (!final_response.equals("6A 82 ") && final_command.equals("00 A4 04 00 07 F0 01 02 03 04 05 06 ")) {
            JSONObject obj=new JSONObject();
            try {
              obj.put("value", final_response);
            } catch (JSONException e) {
              e.printStackTrace();
            }
             PluginResult result = new PluginResult(PluginResult.Status.OK,obj);
            onNfcResultCallbackContext.sendPluginResult(result);
            new CloseTask().execute();
            mTransmitTask=null;
            // kalen plugin 응답으로 수정함.
            // returnResultToKiosk(true, "", final_response, RESULT_OK);
          }
        }
      }
    }

    private String toHexStringWithLength(byte[] buffer, int bufferLength) {

      String bufferString = "";

      for (int i = 0; i < bufferLength; i++) {

        String hexChar = Integer.toHexString(buffer[i] & 0xFF);
        if (hexChar.length() == 1) {
          hexChar = "0" + hexChar;
        }

        bufferString += hexChar.toUpperCase() + " ";
      }

      return bufferString;
    }


  }
}
