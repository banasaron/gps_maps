package com.app.gps_maps;

import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.LifecycleOwnerKt;


import com.app.gps_maps.api.ApiService;
import com.app.gps_maps.api.HourlyData;
import com.app.gps_maps.api.RetrofitHelper;
import com.app.gps_maps.api.WeatherResponse;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.CoroutineStart;
import kotlinx.coroutines.Dispatchers;
import retrofit2.Call;
import retrofit2.Response;

public class WeatherActivity extends AppCompatActivity {
    Response<WeatherResponse> response;
    WeatherResponse weatherResponse;
    int dayCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_weather);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        Bundle bundle = getIntent().getExtras();
        fetchTemperature(bundle.getDouble("latitude"), bundle.getDouble("longitude"));
    }

    private void fetchTemperature(Double lat, Double lon){
        BuildersKt.launch(LifecycleOwnerKt.getLifecycleScope(this), Dispatchers.getIO(), CoroutineStart.DEFAULT, ((coroutineScope, continuation) -> {
            ApiService service = RetrofitHelper.INSTANCE.createRetrofitService(this, ApiService.class);
            try{
                int forecastDays = 16;
                int pastDays = 30;
                Call<WeatherResponse> call = service.getWeather(RetrofitHelper.BASE_URL+"forecast?latitude="+lat+"&longitude="+lon+"&hourly=temperature_2m&forecast_days="+forecastDays+"&past_days="+pastDays);
                response = call.execute();
                BuildersKt.launch(LifecycleOwnerKt.getLifecycleScope(this), Dispatchers.getMain(), CoroutineStart.DEFAULT, (scopeUI, continuationUI)->{
                    if(response.isSuccessful()){
                        weatherResponse = response.body();
                        if(weatherResponse != null && weatherResponse.getHourly() != null){
                            listUpdate(pastDays+1);
                            final int[] currentDay = {pastDays+1};
                            findViewById(R.id.nextDay).setOnClickListener(v->{
                                Log.d("asdhjkbasd", "fetchTemperature: "+currentDay[0]+" "+dayCount);
                                if(currentDay[0] < dayCount-1) {
                                    currentDay[0]++;
                                    listUpdate(currentDay[0]);
                                }
                            });
                            findViewById(R.id.previousDay).setOnClickListener(v->{
                                Log.d("asdhjkbasd", "fetchTemperature: "+currentDay[0]+" "+dayCount);
                                if(currentDay[0] > 0) {
                                    currentDay[0]--;
                                    listUpdate(currentDay[0]);
                                }
                            });
                        }
                    }
                    Log.d("kxjasjx",  response.body().toString());
                    return null;
                });
            }catch(Exception e){
                Log.d("asdasd", "fetchCity: "+e);
            }
            return null;
        }));
    }
    private void listUpdate(int day_int){
        HourlyData hourlyData = weatherResponse.getHourly();

        ArrayList<ArrayList<Map<String, Float>>> daysSeparated = new ArrayList<>();

        String[] hours = hourlyData.getTime().toArray(new String[hourlyData.getTime().size()]);;
        Float[] temperatures = hourlyData.getTemperature().toArray(new Float[hourlyData.getTemperature().size()]);
        dayCount = hours.length/24;

        String[] dates = new String[dayCount];

        for (int i = 0; i < dayCount; i++) {
            ArrayList<Map<String, Float>> day = new ArrayList<>();
            boolean isDateAdded = false;
            for (int j = 0; j < 24; j++) {
                if(!isDateAdded) dates[i] = hours[i*24+j].substring(0, 10);
                HashMap hashMap = new HashMap<>();
                hashMap.put("hour", hours[i*24+j].substring(11));
                hashMap.put("temperature", temperatures[i*24+j]+"°C");
                day.add(hashMap);
                isDateAdded = true;
            }
            daysSeparated.add(day);
        }

        ArrayList<Map<String, Float>> weather = daysSeparated.get(day_int);


        TextView dayView = findViewById(R.id.day);
        String currentDay = dates[day_int];
        currentDay = currentDay.replace("-", " ");
        dayView.setText(currentDay);


        ListView listView = findViewById(R.id.listView);

        SimpleAdapter simpleAdapter = new SimpleAdapter(
                this,
                weather,
                R.layout.list_item,
                new String[]{"hour", "temperature"},
                new int[]{R.id.hour, R.id.temperature}
        );
        listView.setAdapter(simpleAdapter);

    }
}