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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ImageView       imgSuggest;
    private LocationManager gps;
    private MobileContent   content;
    private MobileFirst     mobile;
    private WeakHandler     handler;
    private MobileFirstWeather  currentWeather;


    private CAASService     caasService;
    private List <CAASContentItem> suggestContentList;
    private MobileContentItem suggestContent;
    String suggestImgURL;
    BitmapDrawable suggestdrawable;
    String rainImageURL = "https://macm.saas.ibmcloud.com/wps/wcm/myconnect/vp6517/c7a55647-577a-4ca2-b1a2-b199b51b40f5/rain.jpeg?MOD=AJPERES";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // User interface
        imgSuggest = (ImageView) findViewById(R.id.image_suggest);

        // Weak handler
        // Chris: Set currentweather based on location to the UI
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
                        concat = weather.temperature + "°";
                        TextView txtTemperature = (TextView) findViewById(R.id.text_temperature);
                        txtTemperature.setText(concat);

                        // Icon
                        ImageView       imgPhrase = (ImageView) findViewById(R.id.image_phrase);
                        DownloadTask    task = new DownloadTask(imgPhrase);
                        task.execute(weather.path);

                        // Phrase
                        TextView txtPhrase = (TextView) findViewById(R.id.text_phrase);
                        txtPhrase.setText(weather.phrase);

                        // Maximum
                        if(weather.maximum == 9999) {
                            concat = "--";
                        } else {
                            concat = weather.maximum + "°";
                        }

                        TextView txtMaximum = (TextView) findViewById(R.id.text_maximum);
                        txtMaximum.setText(concat);

                        // Minimum
                        concat = weather.minimum + "°";
                        TextView txtMinimum = (TextView) findViewById(R.id.text_minimum);
                        txtMinimum.setText(concat);

                        break;

                    case "suggest":
                        //suggestImgURL = rainImageURL;
                        suggestContent = bundle.getParcelable("suggest");
                        suggestContentList = suggestContent.caasContentItemsList.getContentItems();
                        for(CAASContentItem item :suggestContentList){
                            if (item.getTitle().equals("Snow") ){
                                suggestImgURL = item.getElement("Image").toString();
                            }
                        }
                        Log.d("HANDLE SUGGEST", "after suggest" + suggestImgURL );
                        break;

                    case "draw":
                         suggestdrawable = ((MobileContentItem)bundle.getParcelable("draw")).drawable;
                         imgSuggest.setImageDrawable(suggestdrawable );
                         Log.d("HANDLE DRAW", "after DRAW"  );
                        break;
                }

                return false;
            }
        });



        mobile = new MobileFirst(getApplicationContext());

        currentWeather = new MobileFirstWeather();
        currentWeather.phrase = "Sun";

        content = new MobileContent(
                getApplicationContext().getString(R.string.macm_server),
                getApplicationContext().getString(R.string.macm_context),
                getApplicationContext().getString(R.string.macm_instance),
                getApplicationContext().getString(R.string.macm_api_id),
                getApplicationContext().getString(R.string.macm_api_password)
        );

        caasService = content.getService();

        CAASDataCallback callback =  new CAASDataCallback<CAASContentItemsList>() {
             @Override
             public void onSuccess(CAASRequestResult<CAASContentItemsList> requestResult) {

                 //MobileContentItem MobileContentItem = new MobileContentItem();
                 //MobileContentItem.caasContentItemsList = requestResult.getResult();

                 List<CAASContentItem> CAASConentItemList = requestResult.getResult().getContentItems();

                 for (CAASContentItem tempItem: CAASConentItemList){
                    if (tempItem.getTitle().equals(currentWeather.phrase)){
                        suggestImgURL = tempItem.getElement("Image");
                    }
                 }

                 /**
                     Bundle bundle = new Bundle();
                     Message message = new Message();
                     bundle.putString("action", "suggest");
                     bundle.putParcelable("suggest", MobileContentItem);
                     message.setData(bundle);
                     handler.sendMessage(message);
                 **/
                 Log.d("CONTENT", "OnSuccess:" );
             }

            @Override
            public void onError(CAASErrorResult caasErrorResult) {
                Log.e("CONTENT", "onError" + caasErrorResult.getMessage());
            }
        };

        CAASContentItemsRequest request = new CAASContentItemsRequest(callback);
        String path = "InterConnect2016/content types/Fashion";
        request.setPath(path);
        request.addElements("Image");
        caasService.executeRequest(request);
        Log.d("CONTENT", "after execute" );

        final CAASDataCallback<byte[]> Imgcallback = new CAASDataCallback<byte[]>() {
            @Override
            public void onSuccess(CAASRequestResult<byte[]> requestResult) {
                byte[] bytes = requestResult.getResult();

                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                BitmapDrawable drawable = new BitmapDrawable(
                        getApplicationContext().getResources(),
                        bitmap
                );

                /**
                    MobileContentItem contentItem = new MobileContentItem();
                    contentItem.drawable = drawable;
                    Bundle bundle = new Bundle();
                    Message message = new Message();
                    bundle.putString("action", "draw");
                    bundle.putParcelable("draw", contentItem);
                    message.setData(bundle);
                 **/

                imgSuggest.setImageDrawable(drawable);
                //handler.sendMessage(message);
                Log.d("Asset", "Image success: " );
            }

            @Override
            public void onError(CAASErrorResult error) {
                Log.e("Asset", "Image failed: " + error.getMessage());
            }
        };


        /**
        // Mobile Client Access
        mobile.setMobileFirstListener(new MobileFirstListener() {
            // Current weather conditions
            @Override
            public void onCurrent(MobileFirstWeather currentWeather) {
                Bundle bundle = new Bundle();
                Message message = new Message();

                bundle.putString("action", "weather");
                bundle.putParcelable("weather", currentWeather);
                message.setData(bundle);

                handler.sendMessage(message);

                content.suggest(currentWeather.phrase);
            }
        });
        **/




        // Location history
        SharedPreferences history = getPreferences(Context.MODE_PRIVATE);

        if (history.contains("latitude")) {
            float latitude = history.getFloat("latitude", 0);
            float longitude = history.getFloat("longitude", 0);

            // Weather from cached location
            mobile.currentWeather(latitude, longitude);
        }

        // Current location
        if(ContextCompat.checkSelfPermission(getApplicationContext(), "android.permission.ACCESS_COARSE_LOCATION") == PackageManager.PERMISSION_GRANTED) {
            Log.d("MAIN", "Add location monitor.");
        }

        LocationListener locationMonitor = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if(ContextCompat.checkSelfPermission(getApplicationContext(), "android.permission.ACCESS_COARSE_LOCATION") == PackageManager.PERMISSION_GRANTED) {
                    Log.d("MAIN", "Remove location monitor.");
                }

                // Just once
                gps.removeUpdates(this);

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
                Log.d("onLoacationChanged", latitude + " "+ longitude);

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
        gps.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationMonitor);



        ImageView imgNavigate = (ImageView) findViewById(R.id.image_navigate);
        imgNavigate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CAASAssetRequest assetRequest = new CAASAssetRequest(suggestImgURL, Imgcallback);
                caasService.executeRequest(assetRequest);
                Log.d("ASSET", "after execute" );
            }
        });

        /**
        // Navigation
        ImageView imgNavigate = (ImageView) findViewById(R.id.image_navigate);
        imgNavigate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, NavigateActivity.class);
                startActivity(intent);
            }
        });

        **/

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

}
