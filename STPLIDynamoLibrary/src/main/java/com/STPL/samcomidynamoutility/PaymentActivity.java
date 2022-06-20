package com.STPL.samcomidynamoutility;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class PaymentActivity extends AppCompatActivity implements View.OnClickListener {
    IdynamoWorkerClass idynamoWorkerClass;
    private TextView textViewDeviceStatus, textViewMessage, textViewTryAgain;
    private Button buttonTap, buttonChip, buttonSwipe, buttonClear;
    private ImageView imageViewCardTypes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);
        initView();
        initIDynamo();
        setOnClickListener();
    }
    public void openCardSelectionDialog() {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                CardOptionSelectionDialog cardOptionSelectionDialog = CardOptionSelectionDialog.newInstance();
                cardOptionSelectionDialog.setCalBacks(types -> {
                    switch (types) {
                        case "TAP":
                            idynamoWorkerClass.startTransactionWithTap();
                            break;
                        case "SWIPE":
                            idynamoWorkerClass.startTransactionWithSwipe();
                            break;
                        case "CHIP":
                            idynamoWorkerClass.startTransactionWithChip();
                            break;
                    }
                });
                cardOptionSelectionDialog.show(getSupportFragmentManager(), "readerDialog");
            }
        }, 2000);

    }

    private void initView() {
        textViewDeviceStatus = findViewById(R.id.textViewDeviceStatus);
        textViewDeviceStatus.setText(getString(R.string.device_status, "" + "Disconnect"));
        buttonTap = findViewById(R.id.buttonTap);
        buttonChip = findViewById(R.id.buttonChip);
        buttonSwipe = findViewById(R.id.buttonSwipe);
        buttonClear = findViewById(R.id.buttonClear);
        textViewMessage = findViewById(R.id.textViewMessage);
        buttonClear = findViewById(R.id.buttonClear);
        textViewTryAgain = findViewById(R.id.textViewTryAgain);
        imageViewCardTypes = findViewById(R.id.imageViewCardTypes);
    }

    private void initIDynamo() {
        if (idynamoWorkerClass == null) {
            idynamoWorkerClass = new IdynamoWorkerClass(this, new IDynamoInterface() {
                @Override
                public void onDeviceNotFound() {
                    setDeviceStatus("");
                }
                @Override
                public void onDeviceConnectionStatus(String status) {
                    Toast.makeText(PaymentActivity.this, "" + status, Toast.LENGTH_SHORT).show();
                    setDeviceStatus(status);
                }
                @Override
                public void onSendToDisplay(String message) {
                }

                @Override
                public void onDeviceConnectionError(String error) {
                }

                @Override
                public void onTransactionMessage(String message) {
//                    textViewMessage.setText(message);
                    setDeviceStatus(message);
                }

                @Override
                public void onClearTransactionMessage() {
                    //  textViewMessage.setText("");
                }

                @Override
                public void onResponseDataFromARQC(String response) {

                }

                @Override
                public void removeDeviceCallback() {

                }
            });
        }
    }

    public void setDeviceStatus(String status) {
        if (status.equalsIgnoreCase(Const.Connected)) {
            textViewDeviceStatus.setText(getResources().getString(R.string.insert_tap_swipe));
            imageViewCardTypes.setVisibility(View.VISIBLE);
            imageViewCardTypes.setImageResource(R.drawable.ic_swipe_inser_tap);
         //   hideProgressDialog();
            openCardSelectionDialog();
        } else if (status.equalsIgnoreCase(Const.Disconnected) || status.equalsIgnoreCase("")) {
            textViewDeviceStatus.setText(getResources().getString(R.string.device_not_connected));
            textViewTryAgain.setVisibility(View.GONE);
            imageViewCardTypes.setVisibility(View.GONE);
        } else if (status.equalsIgnoreCase(Const.DECLINED) || status.equalsIgnoreCase(Const.TRANSACTION_TERMINATED)) {
            textViewDeviceStatus.setText(status);
            textViewTryAgain.setVisibility(View.VISIBLE);
        } else {
            textViewTryAgain.setVisibility(View.GONE);
            textViewDeviceStatus.setText(status);
        }
     //   callDecryptCardDetailsAPIAfterCardRead(textViewDeviceStatus.getText().toString(), mARQCData);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void setOnClickListener() {
        buttonTap.setOnClickListener(this);
        buttonChip.setOnClickListener(this);
        buttonSwipe.setOnClickListener(this);
        textViewTryAgain.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.buttonTap) {
            idynamoWorkerClass.startTransactionWithTap();
        } else if (id == R.id.buttonChip) {
            idynamoWorkerClass.startTransactionWithChip();
        } else if (id == R.id.buttonSwipe) {
            idynamoWorkerClass.startTransactionWithSwipe();
        } else if (id == R.id.textViewTryAgain) {
            openCardSelectionDialog();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (idynamoWorkerClass != null) {
            idynamoWorkerClass.closeDevice();
            idynamoWorkerClass.unRegisterReceiver();
        }
    }
}