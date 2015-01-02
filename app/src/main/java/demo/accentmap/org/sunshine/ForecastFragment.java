package demo.accentmap.org.sunshine;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ForecastFragment extends Fragment {
    private ArrayAdapter<String> mForecastAdapter;
    public ForecastFragment() {
    }
    @Override
    public void onStart() {

        super.onStart();
        updateWeather();
    }

    @Override
    public void onCreate(Bundle b) {

        super.onCreate(b);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        inflater.inflate(R.menu.forecastfragment, menu);
    }
    public void updateWeather() {

        FetchWeatherTask weatherTask = new FetchWeatherTask();
        String location = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(getString(R.string.pref_location_key), "90210");
        Log.i("ForecastFragment", "the location found in preferences is " + location);
        weatherTask.execute(location);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem m) {
        int id = m.getItemId();
        if (id == R.id.action_refresh) {

            updateWeather();
            return true;
        } else if (id ==R.id.action_view_location) {
            Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
            String location = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(getString(R.string.pref_location_key), "90210");

            Uri buildUri = Uri.parse("geo:0,0").buildUpon()
                    .appendQueryParameter("q", location).build();
            intent.setData(buildUri);
            if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast toast = Toast.makeText(getActivity(), "No Map App Available", Toast.LENGTH_SHORT);
                toast.show();
            }
            return true;
        }
        return super.onOptionsItemSelected(m);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        List<String> weathers = new ArrayList<String>();

        mForecastAdapter = new ArrayAdapter<String>(getActivity(), R.layout.list_item_forecast, R.id.list_item_forecast_textview, weathers);
        ListView lv = (ListView) rootView.findViewById(R.id.listview_forecast);
        lv.setAdapter(mForecastAdapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Context context = getActivity();
                CharSequence text = mForecastAdapter.getItem(i);
                int duration = Toast.LENGTH_SHORT;

                //Toast toast = Toast.makeText(context, text, duration);
                //toast.show();

                Intent intent = new Intent(getActivity(), DetailActivity.class);

                intent.putExtra(Intent.EXTRA_TEXT, mForecastAdapter.getItem(i));
                startActivity(intent);
            }

        });
        return rootView;
    }

    /* The date/time conversion code is going to be moved outside the asynctask later,
 * so for convenience we're breaking it out into its own method now.
 */
    private String getReadableDateString(long time){
        // Because the API returns a unix timestamp (measured in seconds),
        // it must be converted to milliseconds in order to be converted to valid date.
        Date date = new Date(time * 1000);
        SimpleDateFormat format = new SimpleDateFormat("E, MMM d");
        return format.format(date).toString();
    }

    /**
     * Prepare the weather high/lows for presentation.
     */
    private String formatHighLows(double high, double low, boolean useMetric) {
        // For presentation, assume the user doesn't care about tenths of a degree.
        long roundedHigh = Math.round(high);
        long roundedLow = Math.round(low);

        String highLowStr = null;
        if (useMetric) {
            highLowStr = roundedHigh + "C/" + roundedLow + "C";
        } else {
            highLowStr = roundedHigh + "F/" + roundedLow + "F";
        }
        return highLowStr;
    }

    /**
     * Take the String representing the complete forecast in JSON Format and
     * pull out the data we need to construct the Strings needed for the wireframes.
     *
     * Fortunately parsing is easy:  constructor takes the JSON string and converts it
     * into an Object hierarchy for us.
     */
    private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays, boolean useMetric)
            throws JSONException {

        // These are the names of the JSON objects that need to be extracted.
        final String OWM_LIST = "list";
        final String OWM_WEATHER = "weather";
        final String OWM_TEMPERATURE = "temp";
        final String OWM_MAX = "max";
        final String OWM_MIN = "min";
        final String OWM_DATETIME = "dt";
        final String OWM_DESCRIPTION = "main";
        Log.i("ForecastFragment", forecastJsonStr);
        JSONObject forecastJson = new JSONObject(forecastJsonStr);
        JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

        String[] resultStrs = new String[numDays+1];
        resultStrs[0] = "Weather Data";
        for(int i = 0; i < weatherArray.length(); i++) {
            // For now, using the format "Day, description, hi/low"
            String day;
            String description;
            String highAndLow;

            // Get the JSON object representing the day
            JSONObject dayForecast = weatherArray.getJSONObject(i);

            // The date/time is returned as a long.  We need to convert that
            // into something human-readable, since most people won't read "1400356800" as
            // "this saturday".
            long dateTime = dayForecast.getLong(OWM_DATETIME);
            day = getReadableDateString(dateTime);

            // description is in a child array called "weather", which is 1 element long.
            JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
            description = weatherObject.getString(OWM_DESCRIPTION);

            // Temperatures are in a child object called "temp".  Try not to name variables
            // "temp" when working with temperature.  It confuses everybody.
            JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
            double high = temperatureObject.getDouble(OWM_MAX);
            double low = temperatureObject.getDouble(OWM_MIN);
            if (!useMetric) {
                high = high * 9.0/5.0 + 32.0;
                low = low * 9.0/5.0 + 32.0;
            }

            highAndLow = formatHighLows(high, low, useMetric);
            resultStrs[i+1] = day + " - " + description + " - " + highAndLow;
        }

        return resultStrs;
    }

    private class FetchWeatherTask extends AsyncTask<String, Void, String[]> {


        @Override
        protected void onPostExecute(String[] strings) {
            if (strings != null) {

                mForecastAdapter.clear();
                for (String aString : strings) {
                    mForecastAdapter.add(aString);
                }
            }
        }

        @Override
        protected String[] doInBackground(String... params) {
            if (params.length == 0) {

                return null;
            }
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            String forecastJsonStr = null;
            String zipCode = params[0];

            final String QUERY_PARAM="q";
            final String FORMAT_PARAM="mode";
            final String UNITS_PARAM="units";
            final String DAYS_PARAM="cnt";

            try {
                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are avaiable at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast
                Uri buildUri = Uri.parse("http://api.openweathermap.org/data/2.5/forecast/daily").buildUpon()
                        .appendQueryParameter(QUERY_PARAM, zipCode)
                        .appendQueryParameter(FORMAT_PARAM, "json")
                        .appendQueryParameter(UNITS_PARAM, "metric")
                        .appendQueryParameter(DAYS_PARAM, "7")
                        .build();
                //            builder.appendQueryParameter("mode", "json");
                //            builder.appendQueryParameter("units", "metric");
                //            builder.appendQueryParameter("cnt", "7");
                URL url = new URL(buildUri.toString());
                Log.i("FetchWeatherTask", "the url is " + url.toString());

                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    forecastJsonStr = null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    forecastJsonStr = null;
                }
                forecastJsonStr = buffer.toString();
                String useMetric = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(getString(R.string.pref_units_key), "1");

                boolean useMetricBool = false;
                if (useMetric.equals("1")){
                    useMetricBool = true;
                }
                String[] results = getWeatherDataFromJson(forecastJsonStr, 7, useMetricBool);
                Log.i("FetchWeatherTask", "Successfully downloaded data");
                Log.i("FetchWeatherTask", "useMetricString is " + useMetric);
                Log.i("FetchWeatherTask", "useMetric is " + useMetricBool);
                return results;
            } catch (JSONException je) {
                Log.e("FetchWeatherTask", "Error ", je);
            } catch (IOException e) {
                Log.e("PlaceholderFragment", "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attemping
                // to parse it.
                forecastJsonStr = null;
            } finally{
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e("PlaceholderFragment", "Error closing stream", e);
                    }
                }
            }
            return null;
        }
    }
}
