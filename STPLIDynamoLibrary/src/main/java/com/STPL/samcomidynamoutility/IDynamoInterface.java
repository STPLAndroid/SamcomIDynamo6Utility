package com.STPL.samcomidynamoutility;

public interface IDynamoInterface {
    public void onDeviceNotFound();
    public void onDeviceConnectionStatus(String status);
    public void onSendToDisplay(String message);
    public void onDeviceConnectionError(String error);
    public void onTransactionMessage(String message);
    public void onClearTransactionMessage();
    public void onResponseDataFromARQC(String response);
    void removeDeviceCallback();
}
