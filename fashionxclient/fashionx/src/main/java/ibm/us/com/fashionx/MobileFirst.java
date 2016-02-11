package ibm.us.com.fashionx;

import android.content.Context;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import com.badoo.mobile.util.WeakHandler;

import com.ibm.caas.CAASContentItemsRequest;
import com.ibm.caas.CAASDataCallback;
import com.ibm.caas.CAASService;
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

public class MobileFirst {


    private String weatherEndpoint;
    private Context context;
    private MobileFirstWeather weather;
    private MFPPush push;
    private MFPPushNotificationListener notificationListener;


    public MobileFirst(Context applicationContext) {
        //observers = new ArrayList<>();
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
        //weatherEndpoint = "https://af733bc9-5e0c-45bf-acc8-1c2da952650a:pywHxplbI3@twcservice.mybluemix.net/api/weather/v2/observations/current";
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

        //weatherRequest.setQueryParameter("units", "m");
        //String geocode = String.valueOf(latitude) + "," + String.valueOf(longitude);
        //weatherRequest.setQueryParameter("geocode", geocode);

        weatherRequest.setQueryParameter("lat", String.valueOf(latitude));
        weatherRequest.setQueryParameter("long", String.valueOf(longitude));

        weatherRequest.setQueryParameter("language", "en-US");


        weatherRequest.send(context,new ResponseListener() {
            @Override
            public void onSuccess(Response response) {
                Log.d("weatherRequest", " " + response.getResponseText());

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

                    currWeather.rawPhrase = observed.getString("phrase_12char");
                    currWeather.convertPhrase();

                    metric = observed.getJSONObject("metric");
                    currWeather.temperature = metric.getInt("temp");
                    currWeather.maximum = metric.getInt("temp_max_24hour");
                    currWeather.minimum = metric.getInt("temp_min_24hour");

                    weather = currWeather;
                    Bundle bundle = new Bundle();
                    Message message = new Message();
                    bundle.putString("action", "weather");
                    bundle.putParcelable("weather", weather);
                    message.setData(bundle);
                    WeakHandler handler = GenericCache.getInstance().get("handler");
                    handler.sendMessage(message);

                    CAASService caasService = GenericCache.getInstance().get("caasService");

                    CAASDataCallback CAASContentCallback = GenericCache.getInstance().get("caasContentCallback");

                    CAASContentItemsRequest contentRequest = new CAASContentItemsRequest(CAASContentCallback);
                    String path = context.getString(R.string.macm_path);
                    contentRequest.setPath(path);
                    contentRequest.addElements("Image");

                    caasService.executeRequest(contentRequest);
                    Log.d("currentWeather", "caasService after execute" );


                    return;


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
        return weather;
    }

    public MFPPush getPush(){
        return  push;

    }

    public MFPPushNotificationListener getNotificationListener(){
        return this.notificationListener;
    }



}
