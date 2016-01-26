package ibm.us.com.fashionx;

import android.content.Context;
import android.util.Log;
import android.content.res.AssetManager;

import com.ibm.mobilefirstplatform.clientsdk.android.core.api.BMSClient;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.Request;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.Response;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.ResponseListener;

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

    public MobileFirst(Context applicationContext) {
        observers = new ArrayList<>();
        context = applicationContext;

        // Authenticate mobile client access (MCA)
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
    }



    // Current conditions from Weather Insights
    // Chris - get currentWeather from /api/weather according the current location
    public void currentWeather(float latitude, float longitude) {
        // Protected (authenticated resource)

        Request weatherRequest = new Request( weatherEndpoint, Request.GET);

        weatherRequest.setQueryParameter("lat", String.valueOf(latitude));
        weatherRequest.setQueryParameter("long", String.valueOf(longitude));

        weatherRequest.send(context,new ResponseListener() {
            @Override
            public void onSuccess(Response response) {
                Log.d("weatherRequest", response.getResponseText());

                JSONArray           days;
                JSONObject          data;
                JSONObject          weather;
                JSONObject          forecast;
                JSONObject          observed;
                JSONObject          imperial;
                JSONObject          today;
                MobileFirstWeather  currWeather;

                try {
                    data = new JSONObject(response.getResponseText());

                    // Get pertinent objects
                    weather = data.getJSONObject("current");
                    observed = weather.getJSONObject("observation");
                    imperial = observed.getJSONObject("imperial");

                    forecast = data.getJSONObject("forecast");
                    days = forecast.getJSONArray("forecasts");
                    today = days.getJSONObject(0);

                    // Populate weather results
                    currWeather = new MobileFirstWeather();
                    currWeather.icon = observed.getInt("icon_code");
                    currWeather.path =
                        BMSClient.getInstance().getBluemixAppRoute() +
                        "/public/weathericons/icon" +
                                currWeather.icon +
                        ".png";
                    currWeather.temperature = imperial.getInt("temp");
                    currWeather.phrase = observed.getString("phrase_12char");

                    // Maximum may be null after peak of day
                    if(today.isNull("max_temp")) {
                        currWeather.maximum = 9999;
                    } else {
                        currWeather.maximum = today.getInt("max_temp");
                    }

                    currWeather.minimum = today.getInt("min_temp");

                    //Chris ?????????
                    for(MobileFirstListener observer : observers) {
                        observer.onCurrent(currWeather);
                    }
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

    public void setMobileFirstListener(MobileFirstListener observer) {
        observers.add(observer);
    }

}
