package ro.pub.cs.systems.eim.lab03.practicaltest02v9;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class PracticalTest02MainActivityv9 extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String BROADCAST_ACTION = "ro.pub.cs.systems.eim.practicaltest02v9.SHOW_DATA";

    private EditText wordInput;
    private EditText minLengthInput;
    private TextView resultTextView;

    // Receiver care primește datele
    private final BroadcastReceiver dataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive CALLED!");
            if (intent != null && BROADCAST_ACTION.equals(intent.getAction())) {
                // Obținem string-ul cu cuvintele filtrate
                String filteredWords = intent.getStringExtra("filtered_words");
                // Afișăm în TextView
                if (filteredWords != null && !filteredWords.isEmpty()) {
                    resultTextView.setText(filteredWords);
                } else {
                    resultTextView.setText("No words found.");
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate() called");

        // Inițializări
        wordInput = findViewById(R.id.edit_text_word);
        minLengthInput = findViewById(R.id.edit_text_min_length);
        resultTextView = findViewById(R.id.text_view_meaning);
        Button searchButton = findViewById(R.id.button_request_word);

        // Înregistrăm broadcastReceiver (simplu, fără condiționare de versiune)
        IntentFilter filter = new IntentFilter(BROADCAST_ACTION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(dataReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        }

        // La apăsarea butonului "Search":
        searchButton.setOnClickListener(v -> {
            String word = wordInput.getText().toString().trim();
            String minLenStr = minLengthInput.getText().toString().trim();

            if (!word.isEmpty()) {
                int minLen = 3; // Valoare implicită
                try {
                    minLen = Integer.parseInt(minLenStr);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Min length parse error. Using default = 3", e);
                }
                // Pornim cererea în background
                fetchDataFromAPI(word, minLen);
            } else {
                resultTextView.setText("Please enter a word.");
            }
        });
    }

    private void fetchDataFromAPI(String word, int minLen) {
        new Thread(() -> {
            try {
                URL url = new URL("http://www.anagramica.com/all/:" + word);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    // Logăm răspunsul complet
                    Log.d(TAG, "Full Response: " + response);

                    // Parsăm JSON
                    JSONObject jsonObject = new JSONObject(response.toString());
                    JSONArray allWords = jsonObject.getJSONArray("all");

                    List<String> filteredWordsList = new ArrayList<>();
                    for (int i = 0; i < allWords.length(); i++) {
                        String currentWord = allWords.getString(i);
                        if (currentWord.length() >= minLen) {
                            filteredWordsList.add(currentWord);
                        }
                    }

                    // Facem un string din rezultatele filtrate
                    String result = String.join(", ", filteredWordsList);
                    Log.d(TAG, "Filtered Words: " + result);

                    // Trimitem broadcast
                    Intent broadcastIntent = new Intent(BROADCAST_ACTION);
                    broadcastIntent.putExtra("filtered_words", result);
                    sendBroadcast(broadcastIntent);

                } else {
                    Log.e(TAG, "Error: Response code " + responseCode);
                }

                connection.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Exception: " + e.getMessage(), e);
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Ne deregistrăm de la broadcast când se distruge activitatea
        unregisterReceiver(dataReceiver);
    }
}
