package com.example.beagleq;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import java.net.Socket;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class MainActivity extends AppCompatActivity {

    private Thread Thread1 = null;
    private Button btnStart, btnConnect;
    private TextView output, tvPitTemp, tvFanInfo, lblSetPitTemp;
    private OkHttpClient client;
    private TextView tvConnStatus;
    private EditText etIP, etPort;
    private Switch switchFan;
    SeekBar barPitTemp;

    //Web Socket
    private WebSocket ws;

    String SERVER_IP;
    int SERVER_PORT;

    private final class EchoWebSocketListener extends WebSocketListener {
        private static final int NORMAL_CLOSURE_STATUS = 1000;

        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            try {
                JSONObject json = new JSONObject();
                json.put("topic", "pidToggle");
                json.put("data", "0");
                webSocket.send(json.toString());

            } catch (Exception ex){
                tvConnStatus.setText(ex.toString());
            }
        }

        @Override
        public void onMessage(WebSocket webSocket, String receivedPayload) {
            output(receivedPayload);
        }

        @Override
        public void onMessage(WebSocket webSocket, ByteString bytes) {
            output("Receiving bytes : " + bytes.hex());
        }

        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            webSocket.close(NORMAL_CLOSURE_STATUS, null);
            output("Closing : " + code + " / " + reason);
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            output("Error : " + t.getMessage());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnConnect = findViewById(R.id.btnConnect);
        tvConnStatus = findViewById(R.id.tvConnStatus);
        tvPitTemp = findViewById(R.id.tvPitTemp);
        tvFanInfo = findViewById(R.id.tvFanInfo);
        lblSetPitTemp = findViewById(R.id.lblSetPitTemp);
        switchFan = findViewById(R.id.switchFan);
        output = (TextView) findViewById(R.id.tvConnStatus);
        etIP = findViewById(R.id.etIP);
        etPort = findViewById(R.id.etPort);
        barPitTemp = findViewById(R.id.barPitTemp);
        client = new OkHttpClient();

        Button btnConnect = findViewById(R.id.btnConnect);
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    tvConnStatus.setText("");
                    SERVER_IP = etIP.getText().toString().trim();
                    SERVER_PORT = Integer.parseInt(etPort.getText().toString().trim());

                    if (SERVER_IP.length() == 0 || SERVER_PORT == 0) {
                        tvConnStatus.setText("Must set the Server IP and Port");
                    } else {
                        Thread1 = new Thread(new Thread1());
                        Thread1.start();
                    }
                } catch (Exception ex){
                    tvConnStatus.setText("Must set the Server IP and Port");
                }
            }
        });

        switchFan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                JSONObject json = new JSONObject();

                try {
                    json.put("topic", "pidToggle");
                    if (switchFan.isChecked()) {
                        json.put("data", "1");
                    } else {
                        json.put("data", "0");
                    }
                    ws.send(json.toString());
                } catch (Exception e) {
                    tvConnStatus.setText("Fan error, you're not connected!");
                }

            }
        });

        barPitTemp.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progressChangedValue = 0;

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressChangedValue = progress;
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            public void onStopTrackingTouch(SeekBar seekBar) {

                String pitSet = String.format("%s F", progressChangedValue);


                Toast.makeText(MainActivity.this, pitSet, Toast.LENGTH_SHORT).show();

                JSONObject json = new JSONObject();

                try {
                    json.put("topic", "pitSL");
                    json.put("data", progressChangedValue);

                    ws.send(json.toString());
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Error Setting Pit Temp!", Toast.LENGTH_SHORT).show();
                }


            }
        });

        /*btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connect();
            }
        });*/
    }


    private void updateTemps(JSONObject message) throws JSONException {
        String PIT = String.format("%s F", message.getString("pitTemp"));
        tvPitTemp.setText(PIT);

        lblSetPitTemp.setText(String.format("Set Pit Temp: %s F", message.getString("pitSet")));
        barPitTemp.setProgress(Integer.parseInt(message.getString("pitSet")));
    }

    private void updateFanStatus(JSONObject message) throws JSONException {
        if (message.getString("state").equals("1")){
            tvFanInfo.setText("Running");
        } else {
            tvFanInfo.setText("Off");
        }
    }

    private void output(final String receivedPayload) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    // server message payload for data topics
                    JSONObject message = new JSONObject(receivedPayload);
                    String topic = message.getString("topic");
                    switch (topic){
                        case "info":
                            //updateInfo(message);
                            break;
                        case "temperatures":
                            updateTemps(message);
                            break;
                        case "beagleInfo":
                            //updateBeagleInfo(message);
                            break;
                        case "fanStatus":
                            updateFanStatus(message);
                            break;
                        case "pitAlert":
                            //updatePitAlert(message);
                            break;
                        default:
                            //Logging Here
                    }
                } catch (Exception ex){
                     tvConnStatus.setText("Fatal Blow! Server Down");
                }
            }
        });
    }

    class Thread1 implements Runnable {
        public void run() {
            Socket socket;
            try {
                Request request = new Request.Builder().url("ws://" + SERVER_IP + ":" + SERVER_PORT + "").build();
                EchoWebSocketListener listener = new EchoWebSocketListener();
                ws = client.newWebSocket(request, listener);
                client.dispatcher().executorService().shutdown();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvConnStatus.setText("Connected");
                    }
                });
                //new Thread(new Thread2()).start();
            } catch (Exception e) {
                tvConnStatus.setText("Unable to connect. Verify IP and Port");
                e.printStackTrace();
            }
        }
    }
}