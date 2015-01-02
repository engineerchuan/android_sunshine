package demo.accentmap.org.sunshine;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


public class DetailActivity extends ActionBarActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new DetailFragment())
                    .commit();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_detail, menu);

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

            Intent intent = new Intent(this, SettingsActivity.class);
            //intent.putExtra(Intent.EXTRA_TEXT, mForecastAdapter.getItem(i));
            startActivity(intent);
            Log.i("DetailActivity", "launching settings intent");
            return true;

        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class DetailFragment extends Fragment {
        private String mForecastStr;
        public DetailFragment() {
            setHasOptionsMenu(true);
        }
        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            inflater.inflate(R.menu.detailfragment, menu);
            MenuItem item = menu.findItem(R.id.menu_item_share);
            ShareActionProvider mShareActionProvider = (ShareActionProvider) (MenuItemCompat.getActionProvider(item));

            Intent intent = new Intent(Intent.ACTION_SEND);
            String location = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(getString(R.string.pref_location_key), "90210");

            String weatherInfo = getActivity().getIntent().getExtras().getString(Intent.EXTRA_TEXT);
            intent.putExtra(Intent.EXTRA_TEXT, "The weather in " + location + " is " + weatherInfo);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            intent.setType("text/plain");
            if (mShareActionProvider != null) {
                mShareActionProvider.setShareIntent(intent);
            }
            Log.i("DetailActivity", "launching sahre intent");

        }
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_detail, container, false);
            mForecastStr = getActivity().getIntent().getExtras().getString(Intent.EXTRA_TEXT);
            TextView k = (TextView) rootView.findViewById(R.id.main_text);
            k.setText(mForecastStr);
            return rootView;
        }
    }
}
