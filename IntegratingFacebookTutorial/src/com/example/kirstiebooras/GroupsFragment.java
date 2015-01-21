package com.example.kirstiebooras;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.integratingfacebooktutorial.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment representing the groups view.
 * Displays all groups the user has created.
 * Created by kirstiebooras on 1/19/15.
 */
public class GroupsFragment extends ListFragment {

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Get data
        final ArrayList<ParseObject> groups = new ArrayList<ParseObject>();

        if (ParseUser.getCurrentUser() != null) {
            Log.v("current: ", ParseUser.getCurrentUser().getEmail());
            ParseQuery<ParseObject> groupQuery = ParseQuery.getQuery("Group");
            groupQuery.whereEqualTo("users", ParseUser.getCurrentUser().getEmail());
            groupQuery.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> parseObjects, ParseException e) {
                    //Query should generate group listview using an array adapter
                    for (int i = 0; i < parseObjects.size(); i++) {
                        Log.v("groups: ", parseObjects.get(i).get("name").toString() +
                                parseObjects.get(i).get("users").toString());
                        //groups.add(parseObjects.get(0));
                        groups.add(parseObjects.get(i));
                        final GroupsAdapter adapter = new GroupsAdapter(getActivity().getBaseContext(),
                                groups);
                        setListAdapter(adapter);

                    }
                }
            });
        }

    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        // do something with the data
    }

}