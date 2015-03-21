package com.example.kirstiebooras;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.parse.FunctionCallback;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.integratingfacebooktutorial.R;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Activity to create a new transaction within a group.
 * Created by kirstiebooras on 1/29/15.
 */
public class CreateTransactionActivity extends Activity {

    private static final String TAG = "CreateTransactionActivity";

    private ArrayAdapter<ParseObject> mAdapter;
    private ArrayList<ParseObject> mGroupsList;
    private Spinner mSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.create_transaction_activity);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        RelativeLayout rl = (RelativeLayout) findViewById(R.id.relativeLayout);
        mSpinner = (Spinner) rl.findViewById(R.id.groupsDropDown);

        mGroupsList = new ArrayList<ParseObject>();
        mAdapter = new GroupsSpinnerAdapter(getApplicationContext(), mGroupsList);
        mAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner.setAdapter(mAdapter);

        getGroupsFromParse();
        // TODO: get this from the HomeActivity. Just need to send groupName and group objectId
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.secondary_items, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.logout:
                ParseUser.logOut();
                Log.i(TAG, "User signed out!");
                ParseMethods.unpinData(Constants.CLASSNAME_TRANSACTION);
                ParseMethods.unpinData(Constants.CLASSNAME_GROUP);
                startSigninRegisterActivity();
                return true;

            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void startSigninRegisterActivity() {
        Intent intent = new Intent(this, SigninRegisterActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    public void onSplitBillClick(View view) {
        // Get user input
        String description = ((EditText) findViewById(R.id.description)).getText().toString();
        String amount = ((EditText) findViewById(R.id.amount)).getText().toString();

        // Check the form is complete
        if (description.equals("") || amount == null) {
            Toast.makeText(getApplicationContext(),
                    getResources().getString(R.string.complete_form_toast), Toast.LENGTH_LONG).show();
            return;
        }

        // Check for valid monetary input
        if (!validMonetaryInput(amount)) {
            Toast.makeText(getApplicationContext(),
                    getResources().getString(R.string.invalid_amount_toast),
                    Toast.LENGTH_LONG).show();
            return;
        }

        double amountValue = Double.valueOf(amount);
        String totalAmount = String.format("%.2f", amountValue);

        // Get person owed info
        ParseUser personOwed = ParseUser.getCurrentUser();
        String personOwedEmail = personOwed.getEmail();
        String personOwedName = personOwed.getString(Constants.USER_FULL_NAME);

        // Get group info
        ParseObject group = (ParseObject) mSpinner.getSelectedItem();
        String groupId = group.getObjectId();
        String groupName = group.getString(Constants.GROUP_NAME);

        // Get members of the group to create arrays for paid and datePaid and to get split amount
        @SuppressWarnings("unchecked")
        ArrayList<String> members = (ArrayList<String>) group.get(Constants.GROUP_MEMBERS);
        @SuppressWarnings("unchecked")
        ArrayList<String> displayNames = (ArrayList<String>) group.get(Constants.GROUP_DISPLAY_NAMES);

        // Get split amount
        String splitAmount = getSplitAmount(amountValue, members.size());

        // Create the object
        ParseMethods.createTransactionParseObject(groupId, groupName, personOwedEmail, description,
                totalAmount, members, displayNames, splitAmount);

        // Send emails to group members
        // TODO sendEmails(personOwedName, groupName, description, splitAmount, (String[]) members.toArray());

        finish();
    }

    /**
     * Check if the amount entered by the user is valid.
     * @param amount: The amound the user entered
     * @return: True if valid
     */
    private boolean validMonetaryInput(String amount) {
        int decimal = amount.lastIndexOf('.');
        int decimalPlaces = amount.substring(decimal+1).length();
        if (decimalPlaces > 2 || decimalPlaces < -1) {
            return false;
        }
        return true;
    }

    private String getSplitAmount(double amountValue, double numMembers) {
        double dividedAmount = amountValue / numMembers;
        BigDecimal bd = new BigDecimal(dividedAmount);
        return bd.setScale(2, BigDecimal.ROUND_FLOOR).toString();
    }

    /**
     * Send email to every one splitting the transaction.
     * @param fromName: The person owed
     * @param groupName: The name of the group with the transaction
     * @param chargeDescription: The description for the transaction
     * @param amount: The amount each person owes
     * @param members: An array of all persons splitting the bills
     */
    private void sendEmails(String fromName, String groupName, String chargeDescription,
                            String amount, String[] members) {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("fromName", fromName);
        map.put("groupName", groupName);
        map.put("chargeDescription", chargeDescription);
        map.put("amount", amount);
        map.put("key", getString(R.string.MANDRILL_API_KEY));
        ParseMethods.sendEmails(map, members);
    }

    @Override
    public void finish() {
        // TODO Tell homeactivity specifically what object to get using data.putExtra()
        setResult(RESULT_OK);
        super.finish();
    }

    private void getGroupsFromParse() {
        if (ParseUser.getCurrentUser() != null) {
            ParseQuery<ParseObject> groupQuery = ParseQuery.getQuery("Group");
            groupQuery.whereEqualTo(Constants.GROUP_MEMBERS, ParseUser.getCurrentUser().getEmail());
            groupQuery.fromLocalDatastore();
            // TODO find in background && move to ParseMethods??
            try {
                List<ParseObject> groups = groupQuery.find();
                Log.i(TAG, "Found " + groups.size() + " objects.");
                // Query should generate Spinner data using an array adapter
                // Create a key-value pairing the name to the object id so we can get the id
                mGroupsList.clear();
                mGroupsList.addAll(groups);
                mAdapter.notifyDataSetChanged();
            }
            catch (ParseException e) {
                Log.e(TAG, "Query error: " + e.getMessage());
            }
        }
    }
}
