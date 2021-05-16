package com.smd.leansnow;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PointF;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.expressions.Expression;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;
import timber.log.Timber;

import static androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE;
import static com.mapbox.mapboxsdk.style.expressions.Expression.eq;
import static com.mapbox.mapboxsdk.style.expressions.Expression.get;
import static com.mapbox.mapboxsdk.style.expressions.Expression.literal;
import static com.mapbox.mapboxsdk.style.expressions.Expression.match;
import static com.mapbox.mapboxsdk.style.expressions.Expression.stop;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAnchor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconOffset;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconSize;
import static com.smd.leansnow.Util.convertToLatLng;

interface GithubGistService {
    @GET("{githubUser}/{gistId}/raw")
    Call<JsonObject> getFeatureCollection(@Path("githubUser") String githubUser, @Path("gistId") String gistId);
}

public class MainActivity extends BaseActivity implements OnMapReadyCallback, MapboxMap.OnMapClickListener {
    private static final String LEAN_SNOW_MAP_STYLE_URL = "mapbox://styles/vlasebian/cko61vx0w2r1p17nwtzuve0a7";


    public static final String GITHUB_USERNAME = "vlasebian";

    public static final String GITHUB_GIST_BASE_URL = "https://gist.githubusercontent.com/";

    public static final String FEATURE_COLLECTION_GIST_ID = "83ee4f86645c977956f5aa1f69b900b4";

    private static final String FEATURE_COLLECTION_LAYER_ID = "ski.resorts.feature.collection";

    private static final String FEATURE_SYMBOL_ICON_ID = "feature.id";

    private static final String SOURCE_ID = "feature.source";

    private static final String CALLOUT_LAYER_ID = "callout.id";

    private static final String PROPERTY_SELECTED = "selected";

    private static final String PROPERTY_TITLE = "title";

//    private static com.smd.leansnow.databinding.ActivityMainBinding binding;

    private MapView mapView;

    private MapboxMap mapboxMap;

    private RecyclerView recyclerView;

    private FeatureCollection featureCollection;

    private GeoJsonSource source;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Mapbox.getInstance(this, getString(R.string.mapbox_access_token));

//        // Create bindings for the UI elements.
//        binding = com.smd.leansnow.databinding.ActivityMainBinding.inflate(getLayoutInflater());
//        View view = binding.getRoot();
//        setContentView(view);
        setContentView(R.layout.activity_main);


        recyclerView = findViewById(R.id.rv_on_top_of_map);

        mapView = (MapView) findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);


//        if (!isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
//            ActivityCompat.requestPermissions(
//                    this,
//                    new String[import com.mapbox.geojson.Feature] { Manifest.permission.ACCESS_FINE_LOCATION },
//                    PERMISSIONS_REQUEST_LOCATION
//            );
//        }

    }

    @Override
    public void onMapReady(@NonNull MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;

        Style.Builder leanSnowMapStyleBuilder = new Style.Builder().fromUri(LEAN_SNOW_MAP_STYLE_URL);
        mapboxMap.setStyle(leanSnowMapStyleBuilder, new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                // Map is set up and the style has loaded. Add data or make other map adjustments here.
                mapboxMap.getUiSettings().setCompassEnabled(false);
                mapboxMap.getUiSettings().setLogoEnabled(false);
                mapboxMap.getUiSettings().setAttributionEnabled(false);

                loadFeatures(style);
                setupCalloutLayer(style);

                mapboxMap.addOnMapClickListener(MainActivity.this);
            }
        });

    }

    @Override
    public boolean onMapClick(@NonNull LatLng point) {
        Timber.d("clicked on map.");
        PointF screenPoint = mapboxMap.getProjection().toScreenLocation(point);
        List<Feature> features = mapboxMap.queryRenderedFeatures(screenPoint, FEATURE_COLLECTION_LAYER_ID);
        if (!features.isEmpty()) {
            // we received a click event on the callout layer
            Feature feature = features.get(0);
            PointF symbolScreenPoint = mapboxMap.getProjection().toScreenLocation(convertToLatLng(feature));
            Timber.d("clicked on feature");
//            handleClickCallout(feature, screenPoint, symbolScreenPoint);
            return handleClickIcon(screenPoint);
        } else {
            // we didn't find a click event on callout layer, try clicking maki layer
            Timber.d("did not click on feature");
            return false;
        }
    }

    public void loadFeatures(Style loadedMapStyle) {
        Timber.d("Feature collection loading in progress...");

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(GITHUB_GIST_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        GithubGistService gistService = retrofit.create(GithubGistService.class);

        Call<JsonObject> gistServiceCall = gistService.getFeatureCollection(GITHUB_USERNAME, FEATURE_COLLECTION_GIST_ID);

        gistServiceCall.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                Timber.d(response.body().toString());
                MainActivity.this.featureCollection = FeatureCollection.fromJson(response.body().toString());
                MainActivity.this.source = new GeoJsonSource(SOURCE_ID, MainActivity.this.featureCollection);

                loadedMapStyle.addSource(MainActivity.this.source);
                loadedMapStyle.addImage(FEATURE_SYMBOL_ICON_ID, BitmapFactory.decodeResource(MainActivity.this.getResources(), R.drawable.blue_marker_view));
                loadedMapStyle.addLayer(new SymbolLayer(FEATURE_COLLECTION_LAYER_ID, SOURCE_ID).withProperties(
                        iconImage(FEATURE_SYMBOL_ICON_ID),

                        /* allows show all icons */
                        iconAllowOverlap(true),

                        iconIgnorePlacement(true),

//                        /* when feature is in selected state, grow icon */
                        iconSize(match(Expression.toString(get(PROPERTY_SELECTED)), literal(1.0f),
                                stop("true", 1.5f)))
                        ));
                setupRecyclerView();
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Could not fetch ski resorts.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupCalloutLayer(@NonNull Style loadedMapStyle) {
        loadedMapStyle.addLayer(new SymbolLayer(CALLOUT_LAYER_ID, SOURCE_ID)
                .withProperties(
                        /* show image with id title based on the value of the title feature property */
//                        iconImage("{title}"),

                        /* set anchor of icon to bottom-left */
                        iconAnchor(Property.ICON_ANCHOR_BOTTOM_LEFT),

                        /* offset icon slightly to match bubble layout */
                        iconOffset(new Float[] {-20.0f, -10.0f})
                )

                /* add a filter to show only when selected feature property is true */
                .withFilter(eq((get(PROPERTY_SELECTED)), literal(true))));
    }

    private void setupRecyclerView() {
        RecyclerView.Adapter adapter = new LocationRecyclerViewAdapter(this, featureCollection);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(adapter);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == SCROLL_STATE_IDLE) {
                    int index = layoutManager.findFirstVisibleItemPosition();
                    setSelected(index, false);
                }
            }
        });
        SnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(recyclerView);
    }

    private boolean handleClickIcon(PointF screenPoint) {
        List<Feature> features = mapboxMap.queryRenderedFeatures(screenPoint, FEATURE_COLLECTION_LAYER_ID);
        Timber.d(features.toString());
        if (!features.isEmpty()) {
            String title = features.get(0).getStringProperty(PROPERTY_TITLE);
            List<Feature> featureList = featureCollection.features();
            for (int i = 0; i < featureList.size(); i++) {
                if (featureList.get(i).getStringProperty(PROPERTY_TITLE).equals(title)) {
                    setSelected(i, true);
                }
            }

            return true;
        }
        return false;
    }

    /**
     * Set a feature selected state with the ability to scroll the RecycleViewer to the provided index.
     *
     * @param index      the index of selected feature
     * @param withScroll indicates if the recyclerView position should be updated
     */
    private void setSelected(int index, boolean withScroll) {
        if (recyclerView.getVisibility() == View.GONE) {
            recyclerView.setVisibility(View.VISIBLE);
        }

        deselectAll(false);

        Feature feature = featureCollection.features().get(index);
        selectFeature(feature);
//        animateCameraToSelection(feature);
//        refreshSource();
//        loadMapillaryData(feature);

        if (withScroll) {
            recyclerView.scrollToPosition(index);
        }
    }

    private void deselectAll(boolean hideRecycler) {
        for (Feature feature : featureCollection.features()) {
            feature.properties().addProperty(PROPERTY_SELECTED, false);
        }

        if (hideRecycler) {
            recyclerView.setVisibility(View.GONE);
        }
    }

    private void selectFeature(Feature feature) {
        feature.properties().addProperty(PROPERTY_SELECTED, true);
    }

    private Feature getSelectedFeature() {
        if (featureCollection != null) {
            for (Feature feature : featureCollection.features()) {
                if (feature.getBooleanProperty(PROPERTY_SELECTED)) {
                    return feature;
                }
            }
        }

        return null;
    }


//    private void setupMakiLayer(@NonNull Style loadedMapStyle) {
//        loadedMapStyle.addLayer(new SymbolLayer(MAKI_LAYER_ID, SOURCE_ID)
//                .withProperties(
//                        /* show maki icon based on the value of poi feature property
//                         * https://www.mapbox.com/maki-icons/
//                         */
//                        iconImage("{poi}-15"),
//
//                        /* allows show all icons */
//                        iconAllowOverlap(true),
//
//                        /* when feature is in selected state, grow icon */
//                        iconSize(match(Expression.toString(get(PROPERTY_SELECTED)), literal(1.0f),
//                                stop("true", 1.5f))))
//        );
//    }

    /**
     * Set the favourite state of a feature based on the index.
     *
     */
//    private void toggleFavourite(int index) {
//        Feature feature = featureCollection.features().get(index);
//        String title = feature.getStringProperty(PROPERTY_TITLE);
//        boolean currentState = feature.getBooleanProperty(PROPERTY_FAVOURITE);
//        feature.properties().addProperty(PROPERTY_FAVOURITE, !currentState);
////        View view = viewMap.get(title);
//
//        ImageView imageView = view.findViewById(R.id.logoView);
//        imageView.setImageResource(currentState ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
//        Bitmap bitmap = SymbolGenerator.generate(view);
//        mapboxMap.getStyle(new Style.OnStyleLoaded() {
//            @Override
//            public void onStyleLoaded(@NonNull Style style) {
//                style.addImage(title, bitmap);
//                refreshSource();
//            }
//        });
//    }

    private Boolean isPermissionGranted(String permission) {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onSaveInstanceState(@NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    class LocationRecyclerViewAdapter extends RecyclerView.Adapter<MainActivity.LocationRecyclerViewAdapter.MyViewHolder> {
        private final List<Feature> featureCollection;
        private MainActivity activity;

        LocationRecyclerViewAdapter(MainActivity mainActivity, FeatureCollection featureCollection) {
            this.featureCollection = featureCollection.features();
            this.activity = mainActivity;
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.cardview_symbol_layer, parent, false);
            return new LocationRecyclerViewAdapter.MyViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
            Feature feature = featureCollection.get(position);
            String title = feature.getStringProperty(PROPERTY_TITLE);
            holder.title.setText(title);
            Timber.d("in on bind view holder");
            Timber.d(holder.title.getText().toString());
            holder.setClickListener(new ItemClickListener() {
                @Override
                public void onClick(View view, int position) {
                    if (activity != null) {
                        Intent skiResortInformationActivityIntent = new Intent(MainActivity.this, SkiResortInformation.class);
                        skiResortInformationActivityIntent.putExtra("skiResortName", title);
                        startActivity(skiResortInformationActivityIntent);
//                        activity.toggleFavourite(position);
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return this.featureCollection.size();
        }

        class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            TextView title;
            CardView singleCard;
            ItemClickListener clickListener;

            MyViewHolder(View view) {
                super(view);
                title = view.findViewById(R.id.textview_title);
                singleCard = view.findViewById(R.id.single_location_cardview);
                singleCard.setOnClickListener(this);
            }

            void setClickListener(ItemClickListener itemClickListener) {
                this.clickListener = itemClickListener;
            }

            @Override
            public void onClick(View view) {
                clickListener.onClick(view, getLayoutPosition());
            }
        }
    }

    interface ItemClickListener {
        void onClick(View view, int position);
    }

}
