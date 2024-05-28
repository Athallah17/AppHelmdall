package com.example.apphelmdall;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import com.google.android.material.snackbar.Snackbar;
import androidx.appcompat.app.AppCompatActivity;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import android.os.Handler;
import android.util.Log;
import android.view.View;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.example.apphelmdall.databinding.ActivityMainBinding;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;


import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private GraphView graph;
    private LineGraphSeries<DataPoint> series;
    private List<BlinkData> blinkDataList;
    private boolean isSimulating = true;
    private int totalBlinks;
    private Handler handler = new Handler();
    private Runnable runnable;
    private int elapsedTime = 0;
    private MqttAndroidClient mqttAndroidClient;
    private TextView statusTextView;
    private EditText inputEditText;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.landing_page); // Change to the landing page layout

        // Find the button
        Button getStartedButton = findViewById(R.id.getstarted);

        // Set click listener for the button
        getStartedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to the next screen
                homepage();
            }
        });
    }

    private void homepage() {  //Dashboard Graphic Drowsy
        setContentView(R.layout.homepage);

        // Find the ImageView
        ImageView drowsyImageView = findViewById(R.id.drowsy);

        // Set click listener for the ImageView
        drowsyImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSimulation(); // Panggil metode startSimulation saat ImageView diklik
            }
        });

        // Find the GraphView
        graph = findViewById(R.id.graph);

        // Clear existing data from graph
        graph.removeAllSeries();

    }

    private void startSimulation() {
        // Set isSimulating to true
        isSimulating = true;

        // Clear existing data from graph, if any
        graph.removeAllSeries();

        // Initialize data list
        blinkDataList = new ArrayList<>();

        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                // Generate new data for the graph every second
                if (isSimulating) {
                    // Generate new dummy data (either 1 or 0)
                    int newBlinkCount = new Random().nextBoolean() ? 1 : 0;

                    // Add new data point to the list
                    blinkDataList.add(new BlinkData(elapsedTime, newBlinkCount));

                    // Remove oldest data if the total number of data points exceeds 30
                    if (blinkDataList.size() > 30) {
                        blinkDataList.remove(0); // Remove the oldest data point
                    }

                    // Plot the data on the graph
                    plotData();

                    // Update the image based on the total blinks
                    updateImageView();

                    // Increment elapsed time
                    elapsedTime++;
                } else {
                    // If not simulating, stop the handler
                    handler.removeCallbacks(this);
                }

                // Schedule the next update
                handler.postDelayed(this, 1000); // Update every second
            }
        };
        handler.post(runnable);
    }

    private void updateImageView() {
        // Calculate the total blinks in the last 30 seconds
        int totalBlinksIn30Seconds = 0;
        for (BlinkData blinkData : blinkDataList) {
            totalBlinksIn30Seconds += blinkData.getBlinkCount();
        }

        // Log the total blinks in 30 seconds
        Log.d("data", "Total blinks in 30 seconds: " + totalBlinksIn30Seconds);

        // Update the image based on the total blinks
        ImageView drowsyImageView = findViewById(R.id.drowsy);
        if (totalBlinksIn30Seconds <= 5) {
            drowsyImageView.setImageResource(R.drawable.drowsyg);
        } else if (totalBlinksIn30Seconds > 5 && totalBlinksIn30Seconds <= 10) {
            drowsyImageView.setImageResource(R.drawable.drowsyc);
        } else if (totalBlinksIn30Seconds > 10) {
            drowsyImageView.setImageResource(R.drawable.drowsyr);
        }
    }

    private void plotData() {
        // Clear existing series from the graph
        graph.removeAllSeries();

        // Calculate the start time for the 30-second window
        long startTime = Math.max(0, elapsedTime - 30); // Ensure startTime is not negative

        // Calculate the end time (current time or 30 seconds, whichever is smaller)
        long endTime = Math.min(elapsedTime, startTime + 30);

        // Create a new series for the 30-second window
        LineGraphSeries<DataPoint> series = new LineGraphSeries<>(getDataPoints(startTime, endTime));

        // Add the new series to the graph
        graph.addSeries(series);

        // Customize graph viewport, labels, etc.
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(startTime); // Set min X to start time
        graph.getViewport().setMaxX(startTime + 30); // Set max X to start time + 30 seconds
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(0);
        graph.getViewport().setMaxY(1);
    }




    private DataPoint[] getDataPoints(long startTime, long endTime) {
        List<DataPoint> dataPoints = new ArrayList<>();

        // Iterate through blink data and add data points within the specified time window
        for (BlinkData data : blinkDataList) {
            if (data.getSecond() >= startTime && data.getSecond() <= endTime) {
                DataPoint dataPoint = new DataPoint(data.getSecond(), data.getBlinkCount());
                dataPoints.add(dataPoint);
                // Log the second and blink data for each data point
                Log.d("DataPoints", "Second: " + data.getSecond() + ", Blink count: " + data.getBlinkCount());
            }
        }

        return dataPoints.toArray(new DataPoint[0]);
    }


    private void initializeMQTT() {
        setContentView(R.layout.activity_main);

        statusTextView = findViewById(R.id.streaming_status);
        inputEditText = findViewById(R.id.streaming_textinput);

        Button connectButton = findViewById(R.id.streaming_connectBroker);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connect(MainActivity.this);
            }
        });

        Button publishButton = findViewById(R.id.streaming_publishtext);
        publishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //publishText();
            }
        });
    }

    public void connect(Context context) {
        // Initialize MQTT client with the provided application context
        mqttAndroidClient = new MqttAndroidClient(
                context.getApplicationContext(),  // Use applicationContext instead of context.applicationContext
                "tcp://broker.hivemq.com",       // Replace with your MQTT broker address
                "tester123"                  // Replace with your client ID
        );

        class MyMqttCallback implements MqttCallback {
            @Override
            public void connectionLost(Throwable cause) {
                Log.d("Connection", "connection lost: " + cause.getMessage());
            }

            @Override
            public void messageArrived
            (String topic, MqttMessage message) throws Exception {
                Log.d("Message", "incoming: " + message.toString());

            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                Log.d("Message", "complete: " + token.toString());
            }
        }

        MqttCallback callback = new MyMqttCallback();
        mqttAndroidClient.setCallback(callback);

        Button publishButton = findViewById(R.id.streaming_publishtext);

        try {
            // Attempt to connect to the MQTT broker
            IMqttToken token = mqttAndroidClient.connect();

            // Set up callback for connection success and failure
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // Connection success callback
                    Log.i("Connection", "Successful Connection");

                    Subscribe subscribe = new Subscribe(mqttAndroidClient);
                    subscribe.subscribe("helmdall/led");

                    publishButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // Get the text from the inputEditText
                            String messageData = inputEditText.getText().toString();

                            // Create an instance of the Publish class
                            Publish publish = new Publish(mqttAndroidClient);

                            // Define the topic to which you want to publish the message
                            String topic = "helmdall/control";

                            // Call the publish method on the Publish instance
                            publish.publish(topic, messageData);
                        }
                    });
                }


                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // Connection failure callback
                    Log.i("Connection", "Failure    Connection");
                    // Give your callback on connection failure here
                    exception.printStackTrace();
                }
            });
        } catch (MqttException e) {
            // Connection exception callback
            Log.e("Connection", "Exception while connecting", e);
            // Give your callback on connection failure here
            e.printStackTrace();
        }
    }

    public class Subscribe {

        private MqttAndroidClient mqttAndroidClient;

        public Subscribe(MqttAndroidClient client) {
            this.mqttAndroidClient = client;
        }

        public void subscribe(String topic) {
            int qos = 2; // Mention your qos value
            try {
                mqttAndroidClient.subscribe(topic, qos, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        // Give your callback on Subscription here
                        Log.d("Subscribe", "Subscribed to " + topic);
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        // Give your subscription failure callback here
                        Log.d("Subscribe", "Failed to Subscribe");
                    }
                });
            } catch (MqttException e) {
                // Give your subscription failure callback here
                e.printStackTrace();
            }
        }
    }

    public class Publish {

        private MqttAndroidClient mqttAndroidClient;

        public Publish(MqttAndroidClient client) {
            this.mqttAndroidClient = client;
        }

        public void publish(String topic, String data) {
            try {
                // Log the message before publishing
                Log.d("Publish", "Publishing message: " + data);

                byte[] encodedPayload = data.getBytes("UTF-8");
                MqttMessage message = new MqttMessage(encodedPayload);
                message.setQos(2);
                message.setRetained(false);
                mqttAndroidClient.publish(topic, message);
            } catch (Exception e) {
                // Handle any general exception, including MqttException
                e.printStackTrace();
            }
        }

    }
}

