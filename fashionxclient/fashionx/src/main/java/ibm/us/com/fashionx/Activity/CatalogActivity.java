package ibm.us.com.fashionx.activity;

import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;

import com.ibm.caas.CAASContentItem;
import com.ibm.caas.CAASService;


import java.util.List;

import ibm.us.com.fashionx.model.ItemListAdapter;
import ibm.us.com.fashionx.R;
import ibm.us.com.fashionx.model.MobileFirst;
import ibm.us.com.fashionx.util.GenericCache;

public class CatalogActivity extends Activity{

    private CAASService caasService;
    private ItemListAdapter adapter;
    private List<CAASContentItem> fashionItemList;
    private ListView listView;
    private MobileFirst mobileFirst;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catalog);

        // Back
        ImageView imgBack = (ImageView) findViewById(R.id.image_back);
        imgBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Mobile Content
        caasService = new CAASService(
            getApplicationContext().getString(R.string.macm_server),
            getApplicationContext().getString(R.string.macm_context),
            getApplicationContext().getString(R.string.macm_instance),
            getApplicationContext().getString(R.string.macm_api_id),
            getApplicationContext().getString(R.string.macm_api_password)
        );

        mobileFirst = GenericCache.getInstance().get("MobileFirst");
        mobileFirst.initFashionList();

        fashionItemList = GenericCache.getInstance().get("FashionItemList");
        adapter = new ItemListAdapter(getApplicationContext(),fashionItemList);

        listView = (ListView) findViewById(R.id.listView);
        listView.setAdapter(adapter);

    }


    @Override
    protected void onResume(){
        super.onResume();
        mobileFirst.initFashionList();
        fashionItemList = GenericCache.getInstance().get("FashionItemList");
        adapter = new ItemListAdapter(getApplicationContext(),fashionItemList);
        listView.setAdapter(adapter);
    }
}
