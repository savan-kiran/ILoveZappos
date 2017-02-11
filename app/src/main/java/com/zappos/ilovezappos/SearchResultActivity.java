package com.zappos.ilovezappos;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LinearInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.TextView;

import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.zappos.ilovezappos.databinding.ActivitySearchResultBinding;
import com.zappos.ilovezappos.model.Product;
import com.zappos.ilovezappos.model.SearchResult;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Savan Kiran on 02/09/17.
 */

public class SearchResultActivity extends AppCompatActivity {
    private static final String API_URL = "https://api.zappos.com/Search?";

    private RetrieveTask retrieveTask;
    private ActivitySearchResultBinding binding;
    private Product firstProduct;

    private TextView productUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_search_result);

        productUrl = (TextView) findViewById(R.id.product_url);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final View viewF = view;
                if(firstProduct != null) {
                    ScaleAnimation downScaleAnimation = new ScaleAnimation(1.0f, 0.0f, 1.0f, 0.0f,
                            Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                    downScaleAnimation.setDuration(150);

                    ScaleAnimation upScaleAnimation = new ScaleAnimation(0.0f, 1.0f, 0.0f, 1.0f,
                            Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                    upScaleAnimation.setDuration(150);

                    AnimationSet scaleAnimation = new AnimationSet(true);
                    scaleAnimation.addAnimation(downScaleAnimation);
                    scaleAnimation.addAnimation(upScaleAnimation);
                    scaleAnimation.setInterpolator(new AccelerateDecelerateInterpolator());

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            fab.setImageResource(firstProduct.isAddedToCart()?
                                    R.drawable.add_shopping_cart:R.drawable.clear_shopping_cart);
                            Snackbar.make(viewF, firstProduct.getProductName() +
                                    (firstProduct.isAddedToCart()?" removed from " : " added to ") +
                                    "cart", Snackbar.LENGTH_LONG)
                                    .setAction("Action", null).show();
                            firstProduct.setAddedToCart(!firstProduct.isAddedToCart());
                        }
                    }, 200);

                    fab.setAnimation(scaleAnimation);
                    scaleAnimation.start();
                }
            }
        });

        productUrl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(firstProduct != null) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse(firstProduct.getProductUrl()));
                    startActivity(browserIntent);
                }
            }
        });

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            retrieveTask = new RetrieveTask();
            retrieveTask.execute(query);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_search_result, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    class RetrieveTask extends AsyncTask<String, Void, String> {

        private Exception exception;

        protected void onPreExecute() {

        }

        protected String doInBackground(String... searchString) {
            String api_key = getResources().getString(R.string.api_key);
            // Do some validation here

            try {
                URL url = new URL(API_URL + "term=" + searchString[0] + "&key=" + api_key);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                try {
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuilder.append(line).append("\n");
                    }
                    bufferedReader.close();
                    return stringBuilder.toString();
                } finally {
                    urlConnection.disconnect();
                }
            } catch (Exception e) {
                Log.e("ERROR", e.getMessage(), e);
                return null;
            }
        }

        protected void onPostExecute(String response) {
            if (response == null) {
                response = "THERE WAS AN ERROR";
            }
            SearchResult searchResult = SearchResult.parseJSON(response);
            firstProduct = searchResult.getResults().get(0);
            binding.contentSearchResult.setProduct(firstProduct);
            /*View color = findViewById(R.id.color_id);
            color.setBackgroundColor(firstProduct.getColorId());*/
        }
    }

}
