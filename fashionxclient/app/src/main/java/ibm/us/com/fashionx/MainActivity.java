package ibm.us.com.fashionx;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioRecord;
import android.media.Image;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.net.Uri;
import com.badoo.mobile.util.WeakHandler;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.internal.notification.GameNotification;
import com.ibm.caas.CAASContentItem;
import com.ibm.caas.CAASContentItemsList;
import com.ibm.caas.CAASAssetRequest;
import com.ibm.caas.CAASRequestResult;
import com.ibm.caas.CAASDataCallback;
import com.ibm.caas.CAASErrorResult;
import com.ibm.caas.CAASService;

import com.ibm.cio.watsonsdk.SpeechToText;
import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPPush;

import com.ibm.watson.developer_cloud.android.speech_to_text.v1.dto.SpeechConfiguration;

import com.ibm.watson.developer_cloud.alchemy.v1.AlchemyLanguage;
import com.ibm.watson.developer_cloud.alchemy.v1.model.DocumentSentiment;
import com.ibm.watson.developer_cloud.alchemy.v1.model.Sentiment;
import com.ibm.watson.developer_cloud.




import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private ImageView imgSuggest;
    private LocationManager gps;
    private LocationListener locationMonitor;
    private WeakHandler handler;


    private MobileFirst mobile;
    private MobileContent content;
    private MobileFirstWeather currentWeather;
    private CAASService caasService;
    private String suggestImgURL;
    private AlchemyLanguage alchemyService;

    protected GenericCache genericCache;

    protected MediaRecorder mediaRecorder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // User interface
        imgSuggest = (ImageView) findViewById(R.id.image_suggest);

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

        mobile = new MobileFirst(getApplicationContext());
        currentWeather = new MobileFirstWeather();

        content = new MobileContent(
                getApplicationContext().getString(R.string.macm_server),
                getApplicationContext().getString(R.string.macm_context),
                getApplicationContext().getString(R.string.macm_instance),
                getApplicationContext().getString(R.string.macm_api_id),
                getApplicationContext().getString(R.string.macm_api_password)
        );

        caasService = content.getService();

        GenericCache.getInstance().put("caasService", caasService);

        final CAASDataCallback<byte[]> CAASImgcallback = new CAASDataCallback<byte[]>() {
            @Override
            public void onSuccess(CAASRequestResult<byte[]> requestResult) {
                byte[] bytes = requestResult.getResult();

                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                BitmapDrawable drawable = new BitmapDrawable(
                        getApplicationContext().getResources(),
                        bitmap
                );

                imgSuggest.setImageDrawable(drawable);
                Log.d("Asset", "Image success:");
            }

            @Override
            public void onError(CAASErrorResult error) {
                Log.e("Asset", "Image failed: " + error.getMessage());
            }
        };

        final CAASDataCallback CAASContentCallback = new CAASDataCallback<CAASContentItemsList>() {
            @Override
            public void onSuccess(CAASRequestResult<CAASContentItemsList> requestResult) {
                currentWeather = mobile.getWeather();
                List<CAASContentItem> CAASConentItemList = requestResult.getResult().getContentItems();
                for (CAASContentItem tempItem : CAASConentItemList) {

                    if (tempItem.getTitle().equals(currentWeather.phrase)) {
                        suggestImgURL = tempItem.getElement("Image");
                        Log.d("CONTENT", "OnSuccess: " + suggestImgURL);

                        CAASAssetRequest assetRequest = new CAASAssetRequest(suggestImgURL, CAASImgcallback);
                        caasService.executeRequest(assetRequest);

                        return;
                    }
                }


            }

            @Override
            public void onError(CAASErrorResult caasErrorResult) {
                Log.e("CONTENT", "onError" + caasErrorResult.getMessage());
            }
        };

        GenericCache.getInstance().put("caasContentCallback", CAASContentCallback);


        locationMonitor = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                // Just once
                //gps.removeUpdates(this);

                float latitude = (float) location.getLatitude();
                float longitude = (float) location.getLongitude();

                // Store on device
                SharedPreferences history = getPreferences(Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = history.edit();
                editor.putFloat("latitude", latitude);
                editor.putFloat("longitude", longitude);
                editor.apply();

                // Request the current weather
                mobile.currentWeather(latitude, longitude);

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                ;
            }

            @Override
            public void onProviderEnabled(String provider) {
                //
                //
            }

            @Override
            public void onProviderDisabled(String provider) {
                ;
            }
        };

        gps = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

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
        

        ImageView recordView = (ImageView) findViewById(R.id.image_record);
        recordView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                try{
                    URI uri = new URI("wss://stream.watsonplatform.net/speech-to-text/api/v1/recognize");



                    SpeechConfiguration sConfig = new SpeechConfiguration(SpeechConfiguration.AUDIO_FORMAT_OGGOPUS);

                    SpeechToText.sharedInstance().initWithContext(uri,getApplicationContext());

                }
                catch (URISyntaxException e){
                    Log.e("SpeechToText", e.getMessage() );
                }



            }
        });


        ImageView imgNavigate = (ImageView) findViewById(R.id.image_navigate);
        imgNavigate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("onClick", "onClick Fired");
                /**
                 Bundle bundle = new Bundle();
                 Message message = new Message();
                 bundle.putString("action", "weather");
                 bundle.putParcelable("weather", currentWeather);
                 message.setData(bundle);
                 handler.sendMessage(message);
                 Log.d("onClick", "handler.sendMessage(currentWeather)" );
                 **/

            }
        });

        // Catalog
        LinearLayout layCatalog = (LinearLayout) findViewById(R.id.layout_catalog);
        layCatalog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CatalogActivity.class);
                startActivity(intent);

                AlchemySentimentTask alchemyTask = new AlchemySentimentTask();
                alchemyTask.execute("I feel good today");


            }
        });

    }


    @Override
    protected void onResume() {
        super.onResume();
        MFPPush push = mobile.getPush();
        if (push != null) {
            push.listen(mobile.getNotificationListener());
        }

        if (ContextCompat.checkSelfPermission(getApplicationContext(), "android.permission.ACCESS_COARSE_LOCATION") == PackageManager.PERMISSION_GRANTED) {
            gps.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationMonitor);
            gps.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,1000,0, locationMonitor);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        MFPPush push = mobile.getPush();
        if (push != null) {
            push.hold();
        }
    }


    class AlchemySentimentTask extends AsyncTask<String, Void, Sentiment> {

        DocumentSentiment docSentiment;

        @Override
        protected Sentiment doInBackground(String... params) {
            alchemyService = new AlchemyLanguage();
            alchemyService.setApiKey(getApplicationContext().getString(R.string.AlchemyLanguageAPIKey));
            //GenericCache.getInstance().put("AlchemySerivce", alchemyService);
            Map<String, Object> text = new HashMap<String, Object>();
            text.put(AlchemyLanguage.TEXT, params);
            docSentiment = alchemyService.getSentiment(text);
            return docSentiment.getSentiment();
        }

        @Override
        protected void onPostExecute(Sentiment Sentiment) {
            GenericCache.getInstance().put("Sentiment",docSentiment.getSentiment());
            Log.d("Sentiment", docSentiment.getSentiment().getType() + " " +docSentiment.getSentiment().getScore());
        }

    }

}
