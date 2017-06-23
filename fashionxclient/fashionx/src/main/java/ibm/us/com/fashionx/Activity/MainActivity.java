package ibm.us.com.fashionx.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import com.badoo.mobile.util.WeakHandler;

import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPPush;

import com.ibm.watson.developer_cloud.alchemy.v1.AlchemyLanguage;
import com.ibm.watson.developer_cloud.alchemy.v1.model.DocumentSentiment;
import com.ibm.watson.developer_cloud.alchemy.v1.model.Sentiment;

import com.ibm.watson.developer_cloud.android.speech_to_text.v1.*;
import com.ibm.watson.developer_cloud.android.speech_to_text.v1.SpeechToText;
import com.ibm.watson.developer_cloud.android.speech_to_text.v1.dto.SpeechConfiguration;

import ibm.us.com.fashionx.R;
import ibm.us.com.fashionx.model.MobileFirst;
import ibm.us.com.fashionx.model.MobileFirstWeather;
import ibm.us.com.fashionx.util.GenericCache;


public class MainActivity extends AppCompatActivity {

    private LocationManager gps;
    private LocationListener locationMonitor;
    private WeakHandler handler;

    private MobileFirst mobileFirst;

    private AlchemyLanguage alchemyService;
    private String mRecognitionResults;
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(ibm.us.com.fashionx.R.layout.activity_main);

        // Weak handler
        // Chris: Draw currentweather based on location to the UI
        handler = new WeakHandler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message message) {
                Bundle bundle;
                String action;
                String concat;

                bundle = message.getData();
                action = bundle.getString("action");

                switch (action) {
                    case "weather":
                        // Results
                        MobileFirstWeather weather = bundle.getParcelable("weather");

                        // Temperature
                        concat = weather.temperature + "°C";
                        TextView txtTemperature = (TextView) findViewById(R.id.text_temperature);
                        txtTemperature.setText(concat);

                        // Icon
                        ImageView imgPhrase = (ImageView) findViewById(R.id.image_phrase);
                        //DownloadTask    task = new DownloadTask(imgPhrase);
                        //task.execute(weather.path);

                        // Phrase
                        TextView txtPhrase = (TextView) findViewById(R.id.text_phrase);
                        txtPhrase.setText(weather.phrase);

                        // Maximum
                        if (weather.maximum == 9999) {
                            concat = "--";
                        } else {
                            concat = weather.maximum + "°C";
                        }

                        TextView txtMaximum = (TextView) findViewById(R.id.text_maximum);
                        txtMaximum.setText(concat);

                        // Minimum
                        concat = weather.minimum + "°C";
                        TextView txtMinimum = (TextView) findViewById(R.id.text_minimum);
                        txtMinimum.setText(concat);
                        Log.d("onLocationChanged", "handler" + weather.temperature + " " + weather.latitude + " " + weather.longitude);
                        break;
                }

                return false;
            }
        });

        GenericCache.getInstance().put("handler", handler);

        mobileFirst = new MobileFirst(getApplicationContext(), (ImageView) findViewById(R.id.image_suggest));

        //Request voice capture
        initSTT();
        final TextView textRecord = (TextView)findViewById(R.id.text_record);
        mHandler = new Handler();
        GenericCache.getInstance().put("mHandler", mHandler);
        LinearLayout layout = (LinearLayout) findViewById(R.id.layout_record);
        layout.setOnClickListener(new View.OnClickListener() {
            boolean mStartRecording = true;

            @Override
            public void onClick(View v) {
                if (mStartRecording){
                    mRecognitionResults = "";
                    displayResult(mRecognitionResults);
                    textRecord.setText("Stop recording");

                    RecognizeTask task = new RecognizeTask();
                    task.execute();
                }
                else   {
                    SpeechToText.sharedInstance().stopRecognition();
                    textRecord.setText("Record");
                    Log.d("STT", "Stop Recording");

                    AlchemySentimentTask sentimentTask = new AlchemySentimentTask();
                    String str = GenericCache.getInstance().get("RecognitionResults");
                    if (str != "" && str != null){
                        sentimentTask.execute(str);
                    }
                }
                mStartRecording = !mStartRecording;
            }

        });

        //Request location update
        gps = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationMonitor = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                Log.d("onLocationChanged" , location.getLongitude() + " " + location.getAltitude());

                if (ContextCompat.checkSelfPermission(getApplicationContext(), "android.permission.ACCESS_COARSE_LOCATION") == PackageManager.PERMISSION_GRANTED) {
                    //Just once
                    gps.removeUpdates(this);
                }

                float latitude = (float) location.getLatitude();
                float longitude = (float) location.getLongitude();

                // Store on device
                SharedPreferences history = getPreferences(Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = history.edit();
                editor.putFloat("latitude", latitude);
                editor.putFloat("longitude", longitude);
                editor.apply();

                // Request the current weather
                mobileFirst.currentWeather(latitude, longitude);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}

            @Override
            public void onProviderEnabled(String provider) {}

            @Override
            public void onProviderDisabled(String provider) {}
        };
        /**
         // Location history
         SharedPreferences history = getPreferences(Context.MODE_PRIVATE);

         if (history.contains("latitude")) {
         float latitude = history.getFloat("latitude", 0);
         float longitude = history.getFloat("longitude", 0);

         // Weather from cached location
         mobile.currentWeather(latitude, longitude);

         }
         **/
        ImageView imgNavigate = (ImageView) findViewById(R.id.image_navigate);
        imgNavigate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("onClick", "imgNavigate Fired");
                if (ContextCompat.checkSelfPermission(getApplicationContext(), "android.permission.ACCESS_COARSE_LOCATION") == PackageManager.PERMISSION_GRANTED) {
                    gps.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 10000, locationMonitor);
                    gps.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,10000,10000, locationMonitor);
                }
            }
        });

        // Catalog
        LinearLayout layCatalog = (LinearLayout) findViewById(R.id.layout_catalog);
        layCatalog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GenericCache.getInstance().put("MobileFirst", mobileFirst);
                Intent intent = new Intent(getApplicationContext(), CatalogActivity.class);
                startActivity(intent);

            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        MFPPush push = mobileFirst.getPush();
        if (push != null) {
            push.listen(mobileFirst.getNotificationListener());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        MFPPush push = mobileFirst.getPush();
        if (push != null) {
            push.hold();
        }
    }

    //Initialize SpeechToText Service
    private void initSTT(){
        try{
            //Using Android SpeechToText wrapper
            //WARNING! There are classes in JAVA SpeechToText SDK have the same name
            URI uri = new URI(getApplicationContext().getString(R.string.SpeechToTextWebSocktURL));
            SpeechConfiguration sConfig = new SpeechConfiguration(SpeechConfiguration.AUDIO_FORMAT_OGGOPUS);

            SpeechToText.sharedInstance().initWithContext(uri,getApplicationContext(),sConfig);

        }
        catch (URISyntaxException e){
            Log.e("STT", e.getMessage() );
        }

        String username = getApplicationContext().getString(R.string.SpeechToTextUserName);
        String pwd = getApplicationContext().getString(R.string.SpeechToTextPwd);
        SpeechToText.sharedInstance().setCredentials(username, pwd);
        SpeechToText.sharedInstance().setModel("en-US_BroadbandModel");
        SpeechToText.sharedInstance().setDelegate(new SpeechDelegate());

        return;
    }

    // SpeechToText delegages implement onMessage() to parse JSON
    class SpeechDelegate implements ISpeechDelegate{
        String TAG = "STT";

        public void onOpen() {
            Log.d(TAG, "onOpen");
        }

        public void onError(String error) {
            Log.e(TAG, error);
        }

        public void onClose(int code, String reason, boolean remote) {
            Log.d(TAG, "onClose, code: " + code + " reason: " + reason);
        }

        public void onMessage(String message) {
            Log.d(TAG, "onMessage, message: " + message);
            try {
                JSONObject jObj = new JSONObject(message);
                // state message
                if(jObj.has("state")) {
                    Log.d(TAG, "Status message: " + jObj.getString("state"));
                }
                // results message
                else if (jObj.has("results")) {
                    //if has result
                    Log.d(TAG, "Results message: ");
                    JSONArray jArr = jObj.getJSONArray("results");
                    for (int i=0; i < jArr.length(); i++) {
                        JSONObject obj = jArr.getJSONObject(i);
                        JSONArray jArr1 = obj.getJSONArray("alternatives");
                        String str = jArr1.getJSONObject(0).getString("transcript");

                        //String model = SpeechToText.sharedInstance().getModels().getString("name");
                        //if (model.startsWith("ja-JP") || model.startsWith("zh-CN")) {
                        //    str = str.replaceAll("\\s+","");
                        //}
                        // remove whitespaces if the language requires it

                        String strFormatted = Character.toUpperCase(str.charAt(0)) + str.substring(1);
                        Log.d(TAG + "strFormatted: ", strFormatted);
                        if (obj.getString("final").equals("true")) {
                            //Long confidence = jArr1.getJSONObject(0).getLong("confidence");
                            mRecognitionResults += strFormatted.substring(0,strFormatted.length()-1);
                            displayResult(mRecognitionResults);
                            GenericCache.getInstance().put("RecognitionResults",mRecognitionResults);
                            Log.d(TAG, "GenericaCache: " + mRecognitionResults);
                        } else {
                            displayResult(mRecognitionResults + strFormatted);
                        }

                        break;
                    }
                } else {
                    Log.e(TAG, "unexpected data coming from stt server: \n" + message);
                }

            } catch (JSONException e) {
                Log.e(TAG, "Error parsing JSON");
                e.printStackTrace();
            }
        }

        public void onAmplitude(double amplitude, double volume) {
            //Logger.e(TAG, "amplitude=" + amplitude + ", volume=" + volume);
        }
    }

    class RecognizeTask extends AsyncTask <Void, Void, Void>{
        @Override
        protected Void doInBackground(Void... params) {
            SpeechToText.sharedInstance().recognize();
            Log.d("STT", "start recognize");
            return null;
        }
    }

    class AlchemySentimentTask extends AsyncTask<String, Void, Sentiment> {

        DocumentSentiment docSentiment;
        TextView mTextRecord = (TextView)findViewById(R.id.text_record);
        @Override
        protected Sentiment doInBackground(String... inputText) {
            alchemyService = new AlchemyLanguage();
            alchemyService.setApiKey(getApplicationContext().getString(R.string.AlchemyLanguageAPIKey));
            //GenericCache.getInstance().put("AlchemySerivce", alchemyService);
            Map<String, Object> text = new HashMap<String, Object>();
            text.put(AlchemyLanguage.TEXT, inputText);
            docSentiment = alchemyService.getSentiment(text);
            return docSentiment.getSentiment();
        }

        @Override
        protected void onPostExecute(Sentiment Sentiment) {
            GenericCache.getInstance().put("Sentiment",Sentiment);
            displayResult("\n Sentiment: " + Sentiment.getType() + " " +Sentiment.getScore());
            Log.d("Sentiment", Sentiment.getType() + " " +Sentiment.getScore());
            //mobileFirst.getSuggestImage(mobileFirst.getWeather(),Sentiment);
        }

    }

    void displayResult(final String result) {
        final Runnable runnableUi = new Runnable(){
            @Override
            public void run() {
                TextView textResult = (TextView)findViewById(R.id.speechResult);
                textResult.setText(result);
            }
        };

        new Thread(){
            public void run(){
                mHandler.post(runnableUi);
            }
        }.start();
    }

}
