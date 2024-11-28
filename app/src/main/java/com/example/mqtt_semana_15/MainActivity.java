package com.example.mqtt_semana_15;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private MqttAndroidClient client;
    private Button btnPublicar, btnSuscribir;
    private EditText txtTema, txtMensaje;
    private TextView txtV_mensajesRecibidos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        monitorNetworkChanges();

        btnPublicar = findViewById(R.id.btnPublicar);
        btnSuscribir = findViewById(R.id.btnSuscribir);
        txtMensaje = findViewById(R.id.txtMensaje);
        txtTema = findViewById(R.id.txtTema);
        txtV_mensajesRecibidos = findViewById(R.id.txtV_mensajesRecibidos);

        String clientId = MqttClient.generateClientId();
        client = new MqttAndroidClient(this.getApplicationContext(), "tcp://test.mosquitto.org:1883", clientId);

        try {
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            client.connect(options, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // Successfully connected
                    Toast.makeText(MainActivity.this, "Conectado al servidor MQTT", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // Failed to connect
                    Toast.makeText(MainActivity.this, "Error al conectar al servidor MQTT", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();

        }

        btnSuscribir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String topic = txtTema.getText().toString().trim();
                if (!topic.isEmpty()) {
                    subscribeToTopic(topic);
                } else {
                    Toast.makeText(MainActivity.this, "Por favor, ingresa un tema para suscribirte", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnPublicar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String topic = txtTema.getText().toString().trim();
                String message = txtMensaje.getText().toString().trim();
                if (!topic.isEmpty() && !message.isEmpty()) {
                    publishMessage(topic, message);
                } else {
                    Toast.makeText(MainActivity.this, "Por favor, completa los campos de tema y mensaje", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void subscribeToTopic(String topic) {
        try {
            client.subscribe(topic, 0);
            Toast.makeText(this, "Suscrito al tema: " + topic, Toast.LENGTH_SHORT).show();
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    Toast.makeText(MainActivity.this, "Conexión perdida con el servidor MQTT", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    String mensajeRecibido = message.toString();
                    runOnUiThread(() -> txtV_mensajesRecibidos.append("[" + topic + "]: " + mensajeRecibido + "\n"));
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // Message delivered
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void publishMessage(String topic, String message) {
        try {
            MqttMessage mqttMensaje = new MqttMessage(message.getBytes());
            mqttMensaje.setQos(0);
            client.publish(topic, mqttMensaje);
            Toast.makeText(this, "Mensaje publicado: " + message, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al publicar el mensaje", Toast.LENGTH_SHORT).show();
        }
    }

    private void monitorNetworkChanges() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        connectivityManager.registerNetworkCallback(networkRequest, new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                super.onAvailable(network);
                // Reconectar al servidor MQTT si es necesario
                if (!client.isConnected()) {
                    try {
                        client.connect();
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onLost(Network network) {
                super.onLost(network);
                // Manejar pérdida de conexión si es necesario
            }
        });
    }
}