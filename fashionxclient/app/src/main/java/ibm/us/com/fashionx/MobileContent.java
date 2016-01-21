package ibm.us.com.fashionx;

import android.content.Context;
import android.util.Log;

import com.ibm.caas.CAASContentItem;
import com.ibm.caas.CAASContentItemsList;
import com.ibm.caas.CAASContentItemsRequest;
import com.ibm.caas.CAASDataCallback;
import com.ibm.caas.CAASErrorResult;
import com.ibm.caas.CAASRequestResult;
import com.ibm.caas.CAASService;

import java.util.ArrayList;
import java.util.ArrayList;
import java.util.List;

public class MobileContent {

    public static final String CONTENT_ADVERTISEMENT = "Samples/content types/advertisement";
    public static final String CONTENT_ALL = "Samples/views/all";
    public static final String CONTENT_CATALOG = "InterConnect2016/content types/Fashion";
    public static final String CONTENT_SUGGESTION = "InterConnect2016/content types/Fashion";

    public static final String ELEMENT_IMAGE = "image";
    public static final String ELEMENT_DISCOUNT = "discount";
    public static final String ELEMENT_PLACEMENT = "placement";

    private ArrayList<MobileContentListener>    observers;
    private CAASService                         service;

    public MobileContent(String server, String context, String instance, String user, String password) {
        observers = new ArrayList<>();

        setService(new CAASService(
                server,
                context,
                instance,
                user,
                password
        ));
    }

    public void advertise(String department) {
        String[] parts = department.split(" ");

        final CAASContentItemsRequest request = new CAASContentItemsRequest(new CAASDataCallback<CAASContentItemsList>() {
            @Override
            public void onSuccess(CAASRequestResult<CAASContentItemsList> requestResult) {
                CAASContentItemsList itemslist = requestResult.getResult();
                List<CAASContentItem> items = itemslist.getContentItems();

                Log.d("CONTENT", "Advertisement found.");

                for(MobileContentListener observer : observers) {
                    observer.onAdvertise(
                            items.get(0).getElement(ELEMENT_IMAGE).toString(),
                            items.get(0).getElement(ELEMENT_DISCOUNT).toString()
                    );
                }
            }

            @Override
            public void onError(CAASErrorResult caasErrorResult) {
                Log.d("CONTENT", "Advertisement fail.");
            }
        });

        for(String part : parts) {
            request.addAnyKeywords(part);
        }

        request.addElements(ELEMENT_IMAGE);
        request.addElements(ELEMENT_DISCOUNT);
        request.setPath(CONTENT_ADVERTISEMENT);
        request.setPageSize(1);

        getService().executeRequest(request);
    }

    public void all() {
        CAASContentItemsRequest request = new CAASContentItemsRequest(new CAASDataCallback<CAASContentItemsList>() {
            @Override
            public void onSuccess(CAASRequestResult<CAASContentItemsList> requestResult) {
                Log.d("CONTENT", "All found.");
            }

            @Override
            public void onError(CAASErrorResult caasErrorResult) {
                Log.d("CONTENT", "All fail."+caasErrorResult.getMessage());

            }
        });
        request.setPath(CONTENT_ALL);
        getService().executeRequest(request);
    }

    public void catalog() {
        CAASContentItemsRequest request = new CAASContentItemsRequest(new CAASDataCallback<CAASContentItemsList>() {
            @Override
            public void onSuccess(CAASRequestResult<CAASContentItemsList> requestResult) {
                ArrayList<CatalogItem>  items = new ArrayList<>();
                CatalogItem             catalogItem;
                List<CAASContentItem>   content = requestResult.getResult().getContentItems();

                Log.d("CONTENT", "Catalog found.");

                for(CAASContentItem item : content) {
                    catalogItem = new CatalogItem();
                    catalogItem.title = item.getTitle();
//                    department.image = item.getElement(ELEMENT_IMAGE).toString();
//                    department.placement = item.getElement(ELEMENT_PLACEMENT).toString();
                    items.add(catalogItem);
                    Log.d("CONTENT","Catalog" + item.getTitle());
                }

                for(MobileContentListener observer : observers) {
                    observer.onCatalog(items);
                }
            }

            @Override
            public void onError(CAASErrorResult caasErrorResult) {
                Log.d("CONTENT", "Catalog fail."+caasErrorResult.getMessage());
            }
        });
        request.setPath(CONTENT_CATALOG);
        getService().executeRequest(request);
    }


    //input - weather : "Snow" / "Sun" / "Rain"
    public void suggest(String weather) {
        String[] parts = weather.split("[ /]");

        Log.d("CONTENT", "Weather: " + weather);

        CAASContentItemsRequest request = new CAASContentItemsRequest(new CAASDataCallback<CAASContentItemsList>() {
            @Override
            public void onSuccess(CAASRequestResult<CAASContentItemsList> requestResult) {
                List<CAASContentItem> items = requestResult.getResult().getContentItems();

                Log.d("CONTENT", "Found suggestion (" + items.size() + ").");

                for(MobileContentListener observer : observers) {
                    observer.onSuggest(items.get(0).getElement(ELEMENT_IMAGE).toString());
                }
            }

            @Override
            public void onError(CAASErrorResult caasErrorResult) {
                Log.d("CONTENT", "Suggest failure.");
            }
        });

        for(String part : parts) {
            request.addAnyKeywords(part);
        }

        request.addElements(ELEMENT_IMAGE);
        request.setPath(CONTENT_SUGGESTION);
        request.setPageSize(1);

        getService().executeRequest(request);
    }


    public void setMobileContentListener(MobileContentListener observer) {
        observers.add(observer);
    }

    public CAASService getService() {
        return service;
    }

    public void setService(CAASService service) {
        this.service = service;
    }
}
