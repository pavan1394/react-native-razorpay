
package com.razorpay.rn;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.ReadableNativeMap;
import com.facebook.react.bridge.ReadableType;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.razorpay.ApplicationDetails;
import com.razorpay.PaymentData;
import com.razorpay.PaymentResultWithDataListener;
import com.razorpay.Razorpay;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Iterator;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;


public class RazorpayModule extends ReactContextBaseJavaModule implements ActivityEventListener  {


  public static final int RZP_REQUEST_CODE = 72967729;
  public static final String MAP_KEY_RZP_PAYMENT_ID = "razorpay_payment_id";
  public static final String MAP_KEY_PAYMENT_ID = "payment_id";
  public static final String MAP_KEY_ERROR_CODE = "code";
  public static final String MAP_KEY_ERROR_DESC = "description";
  public static final String MAP_KEY_PAYMENT_DETAILS = "details";
  public static final String MAP_KEY_WALLET_NAME="name";
  ReactApplicationContext reactContext;
  public RazorpayModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
    reactContext.addActivityEventListener(this);
  }

  @Override
  public String getName() {
    return "RazorpayCustomui";
  }

  @ReactMethod
  public void open(ReadableMap options) {
    Activity currentActivity = getCurrentActivity();
    try {
      JSONObject optionsJSON = Utils.readableMapToJson(options);
      Intent intent = new Intent(currentActivity, RazorpayPaymentActivity.class);
      intent.putExtra(Constants.OPTIONS, optionsJSON.toString());
      currentActivity.startActivityForResult(intent, RazorpayPaymentActivity.RZP_REQUEST_CODE);
    } catch (Exception e) {}
  }

  @ReactMethod
  public void isValidCardNumber(final String cardNumber, final Promise promise) {
    final Activity currentActivity = getCurrentActivity();
    try {
      if (currentActivity != null) {
        currentActivity.runOnUiThread(new Runnable() {
          @Override
          public void run() {
            final Razorpay razorpay = new Razorpay(currentActivity);
            boolean isValidCardNumber = razorpay.isValidCardNumber(cardNumber);
            String cardType = razorpay.getCardNetwork(cardNumber);
            WritableMap map = Arguments.createMap();
            map.putBoolean("isValidCardNumber", isValidCardNumber);
            map.putString("cardType", cardType);
            promise.resolve(map);
          }
        });
      }
    } catch (Exception e) {
      Log.d("TEST4", e.getMessage()+"TEST4");
    }
  }

  @ReactMethod
  public void openUpiApp(final String appName, final String packageName, final Promise promise) {
    final Activity currentActivity = getCurrentActivity();
    try {
      if (currentActivity != null) {
        currentActivity.runOnUiThread(new Runnable() {
          @Override
          public void run() {
            final Razorpay razorpay = new Razorpay(currentActivity);
            razorpay.openUpiApp(appName, packageName);
          }
        });
      }
    } catch (Exception e) {
      Log.d("TEST4", e.getMessage()+"TEST4");
    }
  }

  @ReactMethod
  public void callNativeIntent(final String name, final Promise promise) {
    final Activity currentActivity = getCurrentActivity();
    try {
      if (currentActivity != null) {
        currentActivity.runOnUiThread(new Runnable() {
          @Override
          public void run() {
            final Razorpay razorpay = new Razorpay(currentActivity);
            razorpay.callNativeIntent(name);
          }
        });
      }
    } catch (Exception e) {
      Log.d("TEST4", e.getMessage()+"TEST4");
    }
  }

  @ReactMethod
  public void getPaymentMethods(final ReadableMap options, final Promise promise) {
    final Activity currentActivity = getCurrentActivity();
    Log.d("TEST1", "TEST1");
    try {
      if (currentActivity != null) {
        Log.d("TEST2", "TEST2");
        currentActivity.runOnUiThread(new Runnable() {
          @Override
          public void run() {
            JSONObject optionsJSON = Utils.readableMapToJson(options);
            final Razorpay razorpay = new Razorpay(currentActivity);
            Razorpay.canShowUpiIntentMethod(currentActivity);

            razorpay.getPaymentMethods(new Razorpay.PaymentMethodsCallback() {
              @Override
              public void onPaymentMethodsReceived(String result) {
                try {
                  JSONObject paymentMethods = new JSONObject(result);
                  Log.d("paymentMethods", result);
                  WritableMap map = Arguments.createMap();
                  JSONObject banksJson = paymentMethods.getJSONObject("netbanking");
                  JSONObject walletsJson = paymentMethods.getJSONObject("wallet");
                  JSONArray keys = banksJson.names ();
                  WritableArray banks = new WritableNativeArray();
                  WritableArray wallets = new WritableNativeArray();
                  for (int i = 0; i < keys.length (); ++i) {
                    String key = keys.getString (i); // Here's your key
                    String value = banksJson.getString (key); // Here's your value
                    WritableMap bankObj = new WritableNativeMap();
                    bankObj.putInt("id", i);
                    bankObj.putString("slug", key);
                    bankObj.putString("name", value);
                    bankObj.putString("logo", razorpay.getBankLogoUrl(key));
                    banks.pushMap(bankObj);
                  }

                  JSONArray walletKeys = walletsJson.names ();

                  for (int i = 0; i < walletKeys.length (); ++i) {
                    String key = walletKeys.getString (i); // Here's your key
                    String value = walletsJson.getString (key); // Here's your value
                    WritableMap walletObj = new WritableNativeMap();
                    walletObj.putInt("id", i);
                    walletObj.putString("slug", key);
                    walletObj.putString("name", key.toUpperCase());
                    walletObj.putString("enabled", value);
                    walletObj.putString("logo", razorpay.getWalletLogoUrl(key));
                    wallets.pushMap(walletObj);
                  }

                  List<ApplicationDetails> list = Razorpay.getAppsWhichSupportUpi(currentActivity);
                  int i = wallets.size();
                  for (ApplicationDetails obj : list) {
                    Log.d("TEST", "TEST"+obj+":\n"+obj.getAppName()+":\n"+obj.getPackageName()+":\n"+obj.getIconBase64());
                    WritableMap walletObj = new WritableNativeMap();
                    walletObj.putInt("id", i);
                    walletObj.putString("slug", obj.getPackageName());
                    walletObj.putString("name", obj.getAppName());
                    walletObj.putBoolean("enabled", true);
                    walletObj.putString("packageName", obj.getPackageName());
                    walletObj.putString("logo", obj.getIconBase64());
                    wallets.pushMap(walletObj);
                    i++;
                  }

                  map.putArray("netbanking", banks);
                  map.putArray("wallets", wallets);
                  promise.resolve(map);
                } catch (JSONException e) {
                  e.printStackTrace();
                }
              }

              @Override
              public void onError(String error){
                Log.d("TEST3 onError", error+"TEST3");
              }
            });
          }
        });
      }
    } catch (Exception e) {
      Log.d("TEST4", e.getMessage()+"TEST4");
    }
  }

  public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
    if(requestCode == RazorpayPaymentActivity.RZP_REQUEST_CODE && resultCode == RazorpayPaymentActivity.RZP_RESULT_CODE){
      onActivityResult(requestCode, resultCode, data);
    }
  }

  public void onNewIntent(Intent intent) {}


  public void onActivityResult(int requestCode, int resultCode, Intent data){
    String paymentDataString = data.getStringExtra(Constants.PAYMENT_DATA);
    JSONObject paymentData = new JSONObject();
    try{
      paymentData = new JSONObject(paymentDataString);
    } catch(Exception e){
    }
    if(data.getBooleanExtra(Constants.IS_SUCCESS, false)){
      String payment_id = data.getStringExtra(Constants.PAYMENT_ID);
      onPaymentSuccess(payment_id, paymentData);
    } else {
      int errorCode = data.getIntExtra(Constants.ERROR_CODE, 0);
      String errorMessage = data.getStringExtra(Constants.ERROR_MESSAGE);
      onPaymentError(errorCode, errorMessage, paymentData);
    }
  }

  private void sendEvent(String eventName, WritableMap params) {
    reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit(eventName, params);
  }


  public void onPaymentSuccess(String razorpayPaymentId, JSONObject paymentData) {
    sendEvent("Razorpay::PAYMENT_SUCCESS", Utils.jsonToWritableMap(paymentData));
  }

  public void onPaymentError(int code, String description, JSONObject paymentDataJson) {
    WritableMap errorParams = Arguments.createMap();
    try{
      paymentDataJson.put(MAP_KEY_ERROR_CODE, code);
      paymentDataJson.put(MAP_KEY_ERROR_DESC, description);
    } catch(Exception e){
    }
    sendEvent("Razorpay::PAYMENT_ERROR", Utils.jsonToWritableMap(paymentDataJson));
  }

  public void onExternalWalletSelected(String walletName, JSONObject paymentData){
    sendEvent("Razorpay::EXTERNAL_WALLET_SELECTED", Utils.jsonToWritableMap(paymentData));
  }
}
