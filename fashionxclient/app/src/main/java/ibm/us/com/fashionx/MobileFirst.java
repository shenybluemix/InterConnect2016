package ibm.us.com.fashionx;

import android.content.Context;
import android.util.Log;
import android.content.res.AssetManager;

import com.ibm.mobilefirstplatform.clientsdk.android.core.api.BMSClient;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.Request;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.Response;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.ResponseListener;
import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPPush;
import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPPushException;
import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPPushNotificationListener;
import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPPushResponseListener;
import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPSimplePushNotification;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Properties;
import java.io.File;
import java.util.concurrent.Exchanger;

public class MobileFirst {

    //Chris what is this Listener ArrayList for?
    private ArrayList<MobileFirstListener> observers;
    private String weatherEndpoint;
    private Context context;
    private MobileFirstWeather weather;
    private MFPPush push;
    private MFPPushNotificationListener notificationListener;


    public MobileFirst(Context applicationContext) {
        observers = new ArrayList<>();
        context = applicationContext;

        // Initialize Mobilefirst Core Service
        try {
            BMSClient.getInstance().initialize(
                    context,
                    context.getString(R.string.bluemix_route),
                    context.getString(R.string.bluemix_guid)
            );
        } catch (MalformedURLException murle) {
            murle.printStackTrace();
        }

        weatherEndpoint = context.getString(R.string.weatherEndpoint);
        weather = new MobileFirstWeather();

        //Initialize Push Notification Service
        push = MFPPush.getInstance();
        push.initialize(context);

        push.register(new MFPPushResponseListener<String>() {
            @Override
            public void onSuccess(String response) {
                Log.d("PUSH", "Push service connection succeed \n");
            }

            @Override
            public void onFailure(MFPPushException exception) {
                Log.d("PUSH", "Push service connection failed \n" );

            }
        });

        //Register the push notification listener to push serivce
        notificationListener = new MFPPushNotificationListener() {

            @Override
            public void onReceive(MFPSimplePushNotification mfpSimplePushNotification) {
                //to do OnReceive a new content is created from MACM

                Log.d("PUSH","payload: "+ mfpSimplePushNotification.getPayload());
                Log.d("PUSH","Alert: "+ mfpSimplePushNotification.getAlert());

            }
        };

        push.listen(notificationListener);
    }



    // Current conditions from Weather Insights
    // Chris - get currentWeather from according the current location
    public void currentWeather(final float latitude, final float longitude) {
        // Protected (authenticated resource)

        Request weatherRequest = new Request( weatherEndpoint, Request.GET);

        weatherRequest.setQueryParameter("lat", String.valueOf(latitude));
        weatherRequest.setQueryParameter("long", String.valueOf(longitude));
        weatherRequest.send(context,new ResponseListener() {
            @Override
            public void onSuccess(Response response) {
                Log.d("weatherRequest", " " + response.getStatus());

                JSONArray           days;
                JSONObject          data;
                //JSONObject          weather;
                JSONObject          forecast;
                JSONObject          observed;
                JSONObject          metric;
                JSONObject          today;
                MobileFirstWeather  currWeather = new MobileFirstWeather();
                currWeather.latitude = latitude;
                currWeather.longitude = longitude;
                try {
                    data = new JSONObject(response.getResponseText());
                    observed = data.getJSONObject("observation");

                    currWeather.icon = observed.getInt("icon_code");
                    /*
                    currWeather.path =
                        BMSClient.getInstance().getBluemixAppRoute() +
                        "/public/weathericons/icon" +
                                currWeather.icon +
                                currWeather.icon +
                        ".png";
                    **/
                    currWeather.rawPhrase = observed.getString("phrase_12char");
                    currWeather.convertPhrase();

                    metric = observed.getJSONObject("metric");
                    currWeather.temperature = metric.getInt("temp");
                    currWeather.maximum = metric.getInt("temp_max_24hour");
                    currWeather.minimum = metric.getInt("temp_min_24hour");

                    weather = currWeather;
                    return;
                    //Chris ?????????
                    //for(MobileFirstListener observer : observers) {
                    //    observer.onCurrent(currWeather);
                    //}
                    //???????????

                } catch(JSONException jsone) {
                    jsone.printStackTrace();
                }

            }

            @Override
            public void onFailure(Response response, Throwable t, JSONObject extendedInfo) {
                if (response == null){
                    Log.e("MOBILEFIRST", "reponse is null, request not reaching server??");
                }
                Log.e("MOBILEFIRST", "Fail: " + response.toString()+"\n") ;
                Log.e("MOBILEFIRST", "Fail: " + extendedInfo) ;
            }
        });
    }

    public MobileFirstWeather getWeather(){
        return this.weather;
    }

    public MFPPush getPush(){
        return  this.push;

    }

    public MFPPushNotificationListener getNotificationListener(){
        return this.notificationListener;
    }
    public void setMobileFirstListener(MobileFirstListener observer) {
        observers.add(observer);
    }

}
