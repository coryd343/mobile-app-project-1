package group9.tcss450.uw.edu.chatappgroup9;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import group9.tcss450.uw.edu.chatappgroup9.model.NothingSelectedSpinnerAdapter;
import group9.tcss450.uw.edu.chatappgroup9.model.RecyclerViewAdapterMessages;
import group9.tcss450.uw.edu.chatappgroup9.utils.ListenManager;
import group9.tcss450.uw.edu.chatappgroup9.utils.SendPostAsyncTask;

/**
 * Chat class listens and sends message. You can add a friends to a chat through selecting
 * your friends from the spinner on the top. You can also leave a chat. leaving a chat will not
 * accept any new message.
 */
public class ChatFragmentV2 extends Fragment implements AdapterView.OnItemSelectedListener {
    private String myUsername;
    private String mySendUrl;

    /** listens messages **/
    private ListenManager myListenManager;

    /** all messages inside this recycler view **/
    private RecyclerView myRecyclerView;
    private RecyclerViewAdapterMessages myAdapterMessages;

    /** all contacts' member id and username, must be initialized and != null**/
    private ArrayList<String> myContactsList;
    private ArrayList<String> myCopiedContactsList;
    /** all contacts **/
    private Spinner mySpinner;

    /** all messages will send to this chat id**/
    private String myTargetChatId;
    private String myTargetUsername;
    private final String TAG = "Chat FragmentV2";


    public ChatFragmentV2() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_chat_v2, container, false);

        v.findViewById(R.id.chatSendButton).setOnClickListener(this::sendMessage);
        myRecyclerView = v.findViewById(R.id.chatRecyclerViewAllMessages);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        linearLayoutManager.setStackFromEnd(true);
        myRecyclerView.setLayoutManager(linearLayoutManager);
        //initialized with dummy value
        myAdapterMessages = new RecyclerViewAdapterMessages(new ArrayList<String>());
        myRecyclerView.setAdapter(myAdapterMessages);
        TextView chattingWith = v.findViewById(R.id.chatTextViewChattingWith);

        if (getArguments() == null) {
            myTargetChatId = "1";
            chattingWith.setText("Chat ID " + myTargetChatId + " global ");
        } else {
            myTargetChatId = getArguments().getString("TARGET_CHAT_ID");
            myTargetUsername = getArguments().getString("TARGET_USERNAME");
            myContactsList = getArguments().getStringArrayList("CONTACTS_ID_USERNAME");
            chattingWith.setText("Chatting with " + myTargetUsername);
            Log.e(TAG, "current TARGET_CHAT_ID : " + myTargetChatId);
        }

        mySpinner = v.findViewById(R.id.chatSpinnerFriends);
        setUpSpinner();
        Button b =  v.findViewById(R.id.chatButtonLeave);
        b.setOnClickListener(this::leaveButtonOnClick);

        return v;
    }


    private void setUpSpinner() {

        if (myContactsList != null) {
            myCopiedContactsList = (ArrayList<String>) myContactsList.clone();
            ArrayList<String> usernameList = splitContactList(myCopiedContactsList);
            //dummy value
            myCopiedContactsList.add(0, "-1");
            ArrayAdapter<String> adapter = new ArrayAdapter(getActivity(), R.layout.spinner_item_contact,
                    R.id.spinnerItemTextViewUsername, usernameList);

            adapter.setDropDownViewResource(R.layout.spinner_item_contact);
            NothingSelectedSpinnerAdapter spinnerAdapter = new NothingSelectedSpinnerAdapter(adapter, R.layout.spinner_item_contact, getContext());
            mySpinner.setOnItemSelectedListener(this);
            mySpinner.setAdapter(adapter);

        } else {
            //TODO get a new contact list

        }

    }

    /**
     * Extras the all the user names from the list and return.
     * @param theList
     * @return a list of username
     */
    private ArrayList<String> splitContactList(ArrayList<String> theList) {
        ArrayList<String> usernameList = new ArrayList<>();
        usernameList.add("Add another friend to chat");
        if (theList != null) {
            for (int i = 0; i < theList.size(); i++) {
                String[] idUsername = theList.get(i).split(":");
                usernameList.add(idUsername[1]);
            }
        }
        return usernameList;
    }


    /**
     * listener for the leave button, leave this chat.
     * @param view
     */
    public void leaveButtonOnClick(View view) {
        Uri uri = new Uri.Builder().scheme("https").appendPath(getString(R.string.ep_base_url))
                .appendPath(getString(R.string.ep_leaveChat)).build();

        //build the JSON object
        JSONObject msg = new JSONObject();

        try {
            msg.put("chatID", myTargetChatId);
            msg.put("username", myUsername);

        } catch (JSONException e) {
            Log.d("hello", "hello");
            e.printStackTrace();
        }
        Log.d(TAG, "leaveButtonOnClick: sending async" + msg.toString() + uri.toString());
        new SendPostAsyncTask.Builder(uri.toString(), msg)
                .onPostExecute(this::handlechatOnPost)
                .onCancelled(this::handleError)
                .build().execute();

    }

    private void handlechatOnPost(String s) {
        LandingFragment frag = new LandingFragment();
        getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainer, frag, getString(R.string.keys_landing_fragment_tag)).commit();
    }


    private void handleError(final Exception e) {
        Log.e("LISTEN ERROR!!!", e.getMessage());
    }


    @Override
    public void onStart() {
        super.onStart();
        SharedPreferences prefs =
                getActivity().getSharedPreferences(
                        getString(R.string.keys_shared_prefs),
                        Context.MODE_PRIVATE);
        if (!prefs.contains(getString(R.string.keys_shared_prefs_username))) {
            throw new IllegalStateException("No username in prefs!");
        }
        myUsername = prefs.getString(getString(R.string.keys_shared_prefs_username), "");

        mySendUrl = new Uri.Builder()
                .scheme("https")
                .appendPath(getString(R.string.ep_base_url))
                .appendPath(getString(R.string.ep_send_message))
                .build()
                .toString();
        Uri retrieve = new Uri.Builder()
                .scheme("https")
                .appendPath(getString(R.string.ep_base_url))
                .appendPath(getString(R.string.ep_get_message))
                .appendQueryParameter("chatId", myTargetChatId)
                .build();
        Log.e(TAG, "listen message from " + myTargetChatId);


        //no record of a saved timestamp. must be a first time login
        myListenManager = new ListenManager.Builder(retrieve.toString(),
                this::publishProgress)
                .setExceptionHandler(this::handleError)
                .setDelay(1000)
                .build();


    }

    @Override
    public void onResume() {
        super.onResume();
        myListenManager.startListening();
    }


    @Override
    public void onStop() {
        super.onStop();
        String latestMessage = myListenManager.stopListening();
        SharedPreferences prefs =
                getActivity().getSharedPreferences(
                        getString(R.string.keys_shared_prefs),
                        Context.MODE_PRIVATE);
        // Save the most recent message timestamp
        prefs.edit().putString(
                getString(R.string.keys_prefs_time_stamp),
                latestMessage)
                .apply();
    }


    /**
     * getting messages from server
     * @param messages
     */
    private void publishProgress(JSONObject messages) {
        final String[] msgs;
        if (messages.has(getString(R.string.keys_json_messages))) {
            try {

                JSONArray jMessages = messages.getJSONArray(getString(R.string.keys_json_messages));

                msgs = new String[jMessages.length()];
                for (int i = 0; i < jMessages.length(); i++) {

                    JSONObject msg = jMessages.getJSONObject(i);
                    String username = msg.get(getString(R.string.keys_json_username)).toString();
                    String userMessage = msg.get(getString(R.string.keys_json_message)).toString();
                    msgs[i] = myUsername + ":" + username + ":" + userMessage;
                }
            } catch (JSONException e) {
                e.printStackTrace();
                return;
            }

            getActivity().runOnUiThread(() -> {
                for (String s : msgs) {
                    myAdapterMessages.addData(s);
                    myRecyclerView.scrollToPosition(myAdapterMessages.getItemCount() - 1);
                }
//                Log.e("ChatFragemnt", myAdapterMessages.getItemCount() + "");
//                myRecyclerView.scrollToPosition(myAdapterMessages.getItemCount() - 1);

            });
        }
    }

    private void sendMessage(final View theButton) {
        JSONObject messageJson = new JSONObject();
        String msg = ((EditText) getView().findViewById(R.id.chatInputEditText))
                .getText().toString();
        try {
            messageJson.put(getString(R.string.keys_json_username), myUsername);
            messageJson.put(getString(R.string.keys_json_message), msg);
            messageJson.put(getString(R.string.keys_json_send_message_chat_id), myTargetChatId);

            Log.e(TAG, "message sent " + msg.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        new SendPostAsyncTask.Builder(mySendUrl, messageJson)
                .onPostExecute(this::endOfSendMsgTask)
                .onCancelled(this::handleError)
                .build().execute();
    }

    private void handleError(final String msg) {
        Log.e("CHAT ERROR!!!", msg.toString());
    }

    private void endOfSendMsgTask(final String result) {
        try {
            JSONObject res = new JSONObject(result);

            if (res.get(getString(R.string.keys_json_success)).toString()
                    .equals(getString(R.string.keys_json_success_value_true))) {

                ((EditText) getView().findViewById(R.id.chatInputEditText))
                        .setText("");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        if (adapterView.getCount() < 1) {
            return;
        }
        Log.e(TAG, "onItemSelected");
        //get the corresponding id in index i -1.
        String idUsername = myCopiedContactsList.get(i);
        String id = idUsername.split(":")[0];

        Log.e(TAG, "memberId = " + id);
        Uri uri = new Uri.Builder().scheme("https")
                .appendPath(getString(R.string.ep_base_url))
                .appendPath(getString(R.string.ep_addToChat)).build();

        //build the JSON object
        JSONObject msg = new JSONObject();
        try {
            msg.put(getString(R.string.keys_json_chat_id), myTargetChatId);
            msg.put(getString(R.string.keys_json_memberid), id);

        } catch (JSONException e) {
            Log.e(TAG, "JSON parse error " + e.getMessage());
            e.printStackTrace();
        }
        Log.e(TAG, "onItemSelected: sending async " + msg.toString() + uri.toString());
        new SendPostAsyncTask.Builder(uri.toString(), msg)
                .onPostExecute(this::handleResetOnPost)
                .onCancelled(this::handleError)
                .build().execute();

    }

    private void handleResetOnPost(String s) {
        Toast.makeText(getActivity(), "we Added a user", Toast.LENGTH_SHORT);
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }
}