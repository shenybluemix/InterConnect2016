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
import android.media.Image;
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
import android.net.Uri;
import com.badoo.mobile.util.WeakHandler;
import com.ibm.caas.CAASContentItem;
import com.ibm.caas.CAASContentItemsList;
import com.ibm.caas.CAASAssetRequest;
import com.ibm.caas.CAASRequestResult;
import com.ibm.caas.CAASContentItemsRequest;
import com.ibm.caas.CAASDataCallback;
import com.ibm.caas.CAASErrorResult;
import com.ibm.caas.CAASService;
import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPPush;
import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPPushNotificationListener;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ImageView       imgSuggest;
    private LocationManager gps;
    private LocationListener locationMonitor;
    private WeakHandler     handler;


    private MobileFirst     mobile;
    private MobileContent   content;
    private MobileFirstWeather  currentWeather;
    private CAASService     caasService;
    private String suggestImgURL;



    //String rainImageURL = "https://macm.saas.ibmcloud.com/wps/wcm/myconnect/vp6517/c7a55647-577a-4ca2-b1a2-b199b51b40f5/rain.jpeg?MOD=AJPERES";


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

                switch(action) {
                    case "weather":
                        // Results
                        MobileFirstWeather weather = bundle.getParcelable("weather");

                        // Temperature
                        concat = weather.temperature + "°C";
                        TextView txtTemperature = (TextView) findViewById(R.id.text_temperature);
                        txtTemperature.setText(concat);

                        // Icon
                        ImageView       imgPhrase = (ImageView) findViewById(R.id.image_phrase);
                        //DownloadTask    task = new DownloadTask(imgPhrase);
                        //task.execute(weather.path);

                        // Phrase
                        TextView txtPhrase = (TextView) findViewById(R.id.text_phrase);
                        txtPhrase.setText(weather.phrase);

                        // Maximum
                        if(weather.maximum == 9999) {
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
                        Log.d("onLocationChanged","handler" + weather.temperature +" "+weather.latitude + " "+weather.longitude);
                        break;
                }

                return false;
            }
        });



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
                Log.d("Asset", "Image success:" );
            }

            @Override
            public void onError(CAASErrorResult error) {
                Log.e("Asset", "Image failed: " + error.getMessage());
            }
        };

        final CAASDataCallback CAASContentCallback =  new CAASDataCallback<CAASContentItemsList>() {
            @Override
            public void onSuccess(CAASRequestResult<CAASContentItemsList> requestResult) {
                List<CAASContentItem> CAASConentItemList = requestResult.getResult().getContentItems();
                for (CAASContentItem tempItem: CAASConentItemList){
                    if (tempItem.getTitle().equals(currentWeather.phrase)){
                        suggestImgURL = tempItem.getElement("Image");
                        Log.d("CONTENT", "OnSuccess: " + suggestImgURL );

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


        final CAASContentItemsRequest contentRequest = new CAASContentItemsRequest(CAASContentCallback);
        String path = getApplicationContext().getString(R.string.macm_path);
        contentRequest.setPath(path);
        contentRequest.addElements("Image");


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

                // Request weather
                mobile.currentWeather(latitude, longitude);
                currentWeather = mobile.getWeather();
                Log.d("onLocationChanged", latitude + " "+ longitude+ "" + location.getTime());

                Bundle bundle = new Bundle();
                Message message = new Message();
                bundle.putString("action", "weather");
                bundle.putParcelable("weather", currentWeather);
                message.setData(bundle);
                handler.sendMessage(message);

                caasService.executeRequest(contentRequest);
                Log.d("onLocationChanged", "caasService after execute" );
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                ;
            }

            @Override
            public void onProviderEnabled(String provider) {
                ;
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

        // Current location
        //if(ContextCompat.checkSelfPermission(getApplicationContext(), "android.permission.ACCESS_COARSE_LOCATION") == PackageManager.PERMISSION_GRANTED) {
        //    Log.d("MAIN", "Add location monitor.");
        //}


        ImageView imgNavigate = (ImageView) findViewById(R.id.image_navigate);
        imgNavigate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("onClick", "onClick Fired" );
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

        if(ContextCompat.checkSelfPermission(getApplicationContext(), "android.permission.ACCESS_COARSE_LOCATION") == PackageManager.PERMISSION_GRANTED) {
            gps.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationMonitor);
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

}
