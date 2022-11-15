package com.example.beagleq;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.spec.ECField;


import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class MainActivity extends AppCompatActivity {

    Thread Thread1 = null;
    private Button start;
    private TextView output;
    private OkHttpClient client;
    private TextView tvMessages;
    EditText etIP, etPort;

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
                tvMessages.setText(ex.toString());
            }

            //webSocket.send("Hello, it's SSaurel !");
            //webSocket.send("What's up ?");
           // webSocket.send(ByteString.decodeHex("deadbeef"));
            //webSocket.close(NORMAL_CLOSURE_STATUS, "Goodbye !");
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            output("Receiving : " + text);
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
        start = (Button) findViewById(R.id.btnSend);
        output = (TextView) findViewById(R.id.tvMessages);
        client = new OkHttpClient();



        Button btnConnect = findViewById(R.id.btnConnect);
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    tvMessages.setText("");
                    SERVER_IP = etIP.getText().toString().trim();
                    SERVER_PORT = Integer.parseInt(etPort.getText().toString().trim());

                    if (SERVER_IP.length() == 0 || SERVER_PORT == 0) {
                        tvMessages.setText("Must set the Server IP and Port");
                    } else {
                        Thread1 = new Thread(new Thread1());
                        Thread1.start();
                    }
                } catch (Exception ex){
                    tvMessages.setText("Must set the Server IP and Port");
                }
            }
        });

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                start();
            }
        });

    }

    private void start() {
        Request request = new Request.Builder().url("ws://10.69.8.179:8086").build();
        EchoWebSocketListener listener = new EchoWebSocketListener();
        WebSocket ws = client.newWebSocket(request, listener);

        client.dispatcher().executorService().shutdown();
    }

    private void output(final String txt) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                output.setText(output.getText().toString() + "\n\n" + txt);
            }
        });
    }



    /*

    Thread Thread1 = null;
    EditText etIP, etPort;
    TextView tvMessages;
    EditText etMessage;
    Button btnSend;
    String SERVER_IP;
    int SERVER_PORT;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        etIP = findViewById(R.id.etIP);
        etPort = findViewById(R.id.etPort);
        tvMessages = findViewById(R.id.tvMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        Button btnConnect = findViewById(R.id.btnConnect);
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    tvMessages.setText("");
                    SERVER_IP = etIP.getText().toString().trim();
                    SERVER_PORT = Integer.parseInt(etPort.getText().toString().trim());

                    if (SERVER_IP.length() == 0 || SERVER_PORT == 0) {
                        tvMessages.setText("Must set the Server IP and Port");
                    } else {
                        Thread1 = new Thread(new Thread1());
                        Thread1.start();
                    }
                } catch (Exception ex){
                    tvMessages.setText("Must set the Server IP and Port");
                }
            }
        });
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = "";
                try {
                    JSONObject json = new JSONObject();
                    json.put("topic", "pidToggle");
                    json.put("data", "1");
                    message = json.toString();
                } catch (Exception ex){
                    tvMessages.setText(ex.toString());
                }
                if (!message.isEmpty()) {
                    new Thread(new Thread3(message)).start();
                }
            }
        });
    }
    private PrintWriter output;
    private BufferedReader input;
    class Thread1 implements Runnable {
        public void run() {
            Socket socket;
            try {
                socket = new Socket(SERVER_IP, SERVER_PORT);
                output = new PrintWriter(socket.getOutputStream());
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvMessages.setText("Connected");
                    }
                });
                new Thread(new Thread2()).start();
            } catch (IOException e) {
                tvMessages.setText("Unable to connect. Verify IP and Port");
                e.printStackTrace();
            }
        }
    }
    class Thread2 implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    final String message = input.readLine();
                    if (message != null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tvMessages.append("server: " + message + " ");
                            }
                        });
                    } else {
                        Thread1 = new Thread(new Thread1());
                        Thread1.start();
                        return;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    class Thread3 implements Runnable {
        private String message;
        Thread3(String message) {
            this.message = message;
        }
        @Override
        public void run() {
            output.write(message);
            output.flush();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //tvMessages.append("client: " + message + " ");
                    tvMessages.append("client: " + message +  " ");
                    etMessage.setText("");
                }
            });
        }
    } */
}