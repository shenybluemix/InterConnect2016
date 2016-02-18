package ibm.us.com.fashionx.model;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.badoo.mobile.util.WeakHandler;

import com.google.android.gms.drive.events.ChangeListener;
import com.ibm.caas.CAASAssetRequest;
import com.ibm.caas.CAASContentItem;
import com.ibm.caas.CAASContentItemsList;
import com.ibm.caas.CAASContentItemsRequest;
import com.ibm.caas.CAASDataCallback;
import com.ibm.caas.CAASErrorResult;
import com.ibm.caas.CAASProperties;
import com.ibm.caas.CAASRequestResult;
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
import com.ibm.watson.developer_cloud.alchemy.v1.model.Sentiment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;

import ibm.us.com.fashionx.R;
import ibm.us.com.fashionx.util.GenericCache;

public class MobileFirst {

    private String weatherEndpoint;
    private Context context;
    private MobileFirstWeather weather;
    private MFPPush push;
    private MFPPushNotificationListener notificationListener;
    private ImageView imageSuggest;
    private CAASService caasService;

    public MobileFirst(Context applicationContext, ImageView imageView) {
        //observers = new ArrayList<>();
        context = applicationContext;
        imageSuggest = imageView;

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


        caasService = new CAASService(
                context.getString(R.string.macm_server),
                context.getString(R.string.macm_context),
                context.getString(R.string.macm_instance),
                context.getString(R.string.macm_api_id),
                context.getString(R.string.macm_api_password));

        caasService.setAndroidContext(applicationContext);
        GenericCache.getInstance().put("CAASService", caasService);

        initFashionList();

        weatherEndpoint = context.getString(R.string.weatherEndpoint);
        //weatherEndpoint = "https://af733bc9-5e0c-45bf-acc8-1c2da952650a:pywHxplbI3@twcservice.mybluemix.net/api/weather/v2/observations/current";

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
                //OnReceive a new content is created from MACM
                Log.d("PUSH","payload: "+ mfpSimplePushNotification.getPayload());
                Log.d("PUSH","Alert: "+ mfpSimplePushNotification.getAlert());

                initFashionList();
            }
        };

        push.listen(notificationListener);
    }


    //cache the itemList
    public void initFashionList(){

        CAASDataCallback initFashionListCallback = new CAASDataCallback<CAASContentItemsList>() {
            @Override
            public void onSuccess(CAASRequestResult<CAASContentItemsList> caasRequestResult) {
                List<CAASContentItem> CAASConentItemList = caasRequestResult.getResult().getContentItems();

                //Cache the ContentItemList
                if (GenericCache.getInstance().get("FashionItemList") != null){
                    GenericCache.getInstance().remove("FashionItemList");
                }
                else {
                    GenericCache.getInstance().put("FashionItemList", CAASConentItemList);
                }


                List<String> imgURLList = new ArrayList<String>(CAASConentItemList.size());

                for (CAASContentItem item : CAASConentItemList){
                    String strImageURL = item.getElement("Image");
                    String absoluteImageURL = caasService.getServerURL() + strImageURL;
                    imgURLList.add(absoluteImageURL);
                    cacheFashionImage(absoluteImageURL);
                }
                //Cache the Image URL List
                GenericCache.getInstance().put("FashionImageURLList", imgURLList);

                Log.d("initFashionList", imgURLList.toString());
            }

            @Override
            public void onError(CAASErrorResult caasErrorResult) {
                Log.e("initFashionList", caasErrorResult.getMessage());
            }
        };

        CAASContentItemsRequest contentRequest = new CAASContentItemsRequest(initFashionListCallback);
        String path = context.getString(R.string.macm_path);
        contentRequest.setPath(path);
        contentRequest.addProperties(CAASProperties.KEYWORDS, CAASProperties.CATEGORIES, CAASProperties.TITLE);
        contentRequest.addElements("Image");
        caasService.executeRequest(contentRequest);

    }


    private void cacheFashionImage(final String ImageURL){

        final CAASDataCallback<byte[]> CAASImgcallback = new CAASDataCallback<byte[]>() {
            @Override
            public void onSuccess(CAASRequestResult<byte[]> requestResult) {
                byte[] bytes = requestResult.getResult();

                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                //cache all the Image, use ImageURL as the key
                GenericCache.getInstance().put(ImageURL,bitmap);

                Log.d("cacheFashionImage", "Image success:");
            }

            @Override
            public void onError(CAASErrorResult error) {
                Log.e("cacheFashionImage", "Image failed: " + error.getMessage());
            }
        };

        CAASAssetRequest assetRequest = new CAASAssetRequest(ImageURL, CAASImgcallback);
        caasService.executeRequest(assetRequest);

    }

    // Current conditions from Weather Insights
    // Chris - get currentWeather from according the current location
    public MobileFirstWeather currentWeather(final float latitude, final float longitude) {
        // Protected (authenticated resource)

        Request weatherRequest = new Request( weatherEndpoint, Request.GET);

        //weatherRequest.setQueryParameter("units", "m");
        //String geocode = String.valueOf(latitude) + "," + String.valueOf(longitude);
        //weatherRequest.setQueryParameter("geocode", geocode);

        weatherRequest.setQueryParameter("lat", String.valueOf(latitude));
        weatherRequest.setQueryParameter("long", String.valueOf(longitude));

        weatherRequest.send(context,new ResponseListener() {
            @Override
            public void onSuccess(Response response) {
                Log.d("weatherRequest", " " + response.getResponseText());
                JSONObject          metadata;
                JSONObject          data;
                JSONObject          forecast;
                JSONObject          observed;
                JSONObject          metric;
                JSONObject          today;
                MobileFirstWeather  currWeather = new MobileFirstWeather();
                currWeather.latitude = latitude;
                currWeather.longitude = longitude;
                try {
                    data = new JSONObject(response.getResponseText());
                    metadata = data.getJSONObject("metadata");
                    observed = data.getJSONObject("observation");
                    if (metadata.getInt("status_code") !=200){
                        Log.e("currentWeather", "weather API Error exceeds call limiation, comeback later");
                        return;
                    }

                    currWeather.icon = observed.getInt("icon_code");
                    currWeather.rawPhrase = observed.getString("phrase_12char");
                    currWeather.convertPhrase();
                    metric = observed.getJSONObject("metric");
                    currWeather.temperature = metric.getInt("temp");
                    currWeather.maximum = metric.getInt("temp_max_24hour");
                    currWeather.minimum = metric.getInt("temp_min_24hour");

                    weather = currWeather;

                    GenericCache.getInstance().put("weather", weather);

                    Bundle bundle = new Bundle();
                    Message message = new Message();
                    bundle.putString("action", "weather");
                    bundle.putParcelable("weather", weather);
                    message.setData(bundle);
                    WeakHandler handler = GenericCache.getInstance().get("handler");
                    handler.sendMessage(message);

                    Log.d("currentWeather", weather.phrase + weather.latitude + weather.longitude );

                    getSuggestImage(weather,(Sentiment) GenericCache.getInstance().get("Sentiment"));

                } catch(JSONException jsone) {
                    Log.e("currentWeather", jsone.toString());
                }

            }

            @Override
            public void onFailure(Response response, Throwable t, JSONObject extendedInfo) {
                if (response == null){
                    Log.e("MOBILEFIRST", "reponse is null, request not reaching server??");
                }
                Log.e("MOBILEFIRST", "Fail: " + response.toString()+"\n") ;
                Log.e("MOBILEFIRST", "Fail: " + extendedInfo) ;
                weather = null;
            }
        });

        return weather;
    }


    public void getSuggestImage(MobileFirstWeather mWeather, Sentiment sentiment){

        String strSentiment;
        if (sentiment != null){
            strSentiment = sentiment.getType().toString().toLowerCase();
        }
        else {
            strSentiment = null;
        }

        List<CAASContentItem> CAASConentItemList = GenericCache.getInstance().get("FashionItemList");

        for (CAASContentItem tempItem : CAASConentItemList) {
            if (
                    mWeather != null
                    &&
                    tempItem.getKeywords() != null
                    &&
                    tempItem.getKeywords().toLowerCase().contains(mWeather.phrase.toLowerCase())
                    &&
                    tempItem.getKeywords().toLowerCase().contains(strSentiment)) {

                String suggestImgURL = tempItem.getElement("Image");
                Log.d("CONTENT", "OnSuccess: " + suggestImgURL);

                String imgURL = caasService.getServerURL() + suggestImgURL;
                Bitmap bitmap = GenericCache.getInstance().get(imgURL);
                final BitmapDrawable drawable = new BitmapDrawable(
                        context.getResources(),
                        bitmap
                );

                final Runnable runnableUi = new Runnable(){
                    @Override
                    public void run() {
                        imageSuggest.setImageDrawable(drawable);
                    }
                };

                new Thread(){
                    public void run(){
                        android.os.Handler mHandler = GenericCache.getInstance().get("mHandler");
                        mHandler.post(runnableUi);
                    }
                }.start();

            }

        }

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
