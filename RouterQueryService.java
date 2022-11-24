package com.example.rajatkumar.homenetwork;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * This service is intended to create a connection to the router on a separate thread
 * when invoked with an intent.
 * @see android.app.IntentService
 */
public class RouterQueryService extends IntentService {

    /** URL to send JSON-RPC calls to the Router */
    private static final String UBUS_URL ="http://192.168.1.1/ubus";
    /** Login params for router -- these shouldn't be hard coded. */
    private static final String USRNM_PW = "{ \"username\": \"root\", \"password\": \"openwrt\"  }] }'";
    /** String value of JSON-RPC call to log into router. */
    private static final String GET_SESSION = "{ \"jsonrpc\": \"2.0\", \"id\": 1, \"method\": \"call\", " +
            "\"params\": [ \"00000000000000000000000000000000\", \"session\", \"login\", " + USRNM_PW + UBUS_URL;
    private ArrayList<String> vlansAL;

    public RouterQueryService() {
        //creates thread "RouterQuery"
        super("RouterQuery");
    }

    /**
     * Executes the approprate action based on the string value assigned to it via putExtra().
     * e.g. to remove a vlan: putExtra("action", "removeVlan"); putExtra("ssid", "nameOfSSIDtoRemove");
     *
     * @param intent
     * @see android.content.Intent
     */
    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Toast.makeText(this, "Connecting to Router", Toast.LENGTH_LONG).show();
        //gets the value assigned to intent action
        String action = " ";
        if (intent.getStringExtra("action")!=null) action = intent.getStringExtra("action");
        //executes the matching action
        switch (action){
            case "addVlan" :
                // gets the ssid and pw from the extras
                addVlan(intent.getStringExtra("ssid"), intent.getStringExtra("pw"));
            break;
            case "removeVlan" :
                removeVlan(intent.getStringExtra("ssid"));
                break;
            case "getVlans" :
                getVlans();
                break;
            default :
                Log.i("RouterQueryService", "Invalid RouterQuery Option");
                break;
            }
        }

    /**
     * Method for authenticating UBUS session
     * @param query_url string value of the url to query UBUS from the router (e.g. "192.168.1.1/ubus"
     * @return the session token
     */
    public String getToken(String query_url) {
        try {
            URL url = new URL(query_url);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");

            OutputStream os = conn.getOutputStream();
            os.write(GET_SESSION.getBytes(StandardCharsets.UTF_8));
            os.close();

            // read the response
            InputStream in = new BufferedInputStream(conn.getInputStream());
            String result = IOUtils.toString(in, "UTF-8");

            JSONObject myResponse = new JSONObject(result);

            in.close();
            conn.disconnect();
            JSONArray obj2 = (JSONArray) myResponse.get("result");
            JSONObject obj3 = (JSONObject) obj2.get(1);
            return obj3.get("ubus_rpc_session").toString();

        } catch (Exception e) {
            System.out.println(e);
            return null;
        }
    }
    /**
     * Method for adding a new vLan to the network, has not been tested
     * currently sets the password is hardcoded, should call to generate random password
     * (In a different class, generatePSK I believe)
     * @return response - JSON response from ubus
     */
    private String addVlan (String ssid, String pw){
        String token = getToken(UBUS_URL);
        try {
            // json to send to ubus
            String json = "{ \"jsonrpc\": \"2.0\", \"id\": 1, \"method\": \"call\", \"params\": [ " + token +
                    "\", \"modWifi\", \"addVlan\", { \"ssid\" : \" " + ssid + "\", \"passwd\" : \"" + pw + "\"  } ] }' " + UBUS_URL;
            //TODO create helper method for connection to reduce redundancy & check for unique SSID

            // set up connection
            URL url = new URL(UBUS_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");
            // sends output
            OutputStream os = conn.getOutputStream();
            os.write(json.getBytes(StandardCharsets.UTF_8));
            os.close();
            // reads response
            InputStream in = new BufferedInputStream(conn.getInputStream());
            JSONObject myResponse = new JSONObject(IOUtils.toString(in, "UTF-8"));
            // closes connection
            in.close();
            conn.disconnect();
            System.out.println(myResponse.toString());
            return myResponse.toString();
        } catch (Exception e) {
            System.out.println(e);
            return null;
        }
    }

    /**
     * Removes a VLAN from the Router Device
     * @param ssid
     * @return
     */

    private String removeVlan (String ssid){
        String token = getToken(UBUS_URL);
        try {
            // json to send to ubus
            String json = "{ \"jsonrpc\": \"2.0\", \"id\": 1, \"method\": \"call\", \"params\": [ " + token +
                    "\", \"modWifi\", \"rmVlan\", { \"id\" : \"" + ssid + "\"  } ] }'  http://192.168.1.1/ubus";
            // set up connection
            URL url = new URL(UBUS_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");
            // sends output
            OutputStream os = conn.getOutputStream();
            os.write(json.getBytes(StandardCharsets.UTF_8));
            os.close();
            // reads response
            InputStream in = new BufferedInputStream(conn.getInputStream());
            JSONObject myResponse = new JSONObject(IOUtils.toString(in, "UTF-8"));
            // closes connection
            in.close();
            conn.disconnect();
            System.out.println(myResponse.toString());
            return myResponse.toString();
        } catch (Exception e) {
            System.out.println(e);
            return null;
        }
    }

    /**
     * This method gets the list of vlans from the router and returns the list as a String
     * @return vlans
     */
    private String getVlans() {
        String token = getToken(UBUS_URL);
        try {
            //
            String json = "{ \"jsonrpc\": \"2.0\", \"id\": 1000, \"method\": \"call\", \"params\": " +
                    "[ \""+token+"\", \"modWifi\", \"getVlans\", { } ] }";

            URL url = new URL(UBUS_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");

            OutputStream os = conn.getOutputStream();
            os.write(json.getBytes(StandardCharsets.UTF_8));
            os.close();

            // read the response
            InputStream in = new BufferedInputStream(conn.getInputStream());
            String result = IOUtils.toString(in, "UTF-8");
            JSONObject myResponse = new JSONObject(result);

            in.close();
            conn.disconnect();
            System.out.println(myResponse.toString());

            String fullText = myResponse.toString();
            fullText = fullText.substring(22);
            fullText = fullText.split("}",2)[0];
            fullText = fullText.substring(0, fullText.length()-3);
            String[] rows = fullText.split(" ", 0);
            ArrayList<String> rowAl = new ArrayList<>(Arrays.asList(rows));
            ArrayList<String> cell = new ArrayList<>();
            for(int i = 0; i<rowAl.size(); i++) {
                cell.addAll(Arrays.asList(rowAl.get(i).split("\\|")));
            }
            for(int i = 0; i<cell.size(); i+=5) {
                ArrayList<String> listDevices = new ArrayList<>();
                listDevices.add(cell.get(i+1));
                //new Device(Integer.parseInt(cell.get(i)),cell.get(i+1),cell.get(i+2),cell.get(i+3),cell.get(i+4)));
            }
            return myResponse.toString();
        } catch (Exception e) {
            System.out.println(e);
            return null;
        }
    }
    // Just for testing app/router setup
    private String testFile (String token){
        try {
            // json to send to ubus
            String json = "{ \"jsonrpc\": \"2.0\", \"id\": 1, \"method\": \"call\", \"params\": [ " + token +
                    "\", \"file\", \"write\", { \"path\" : \"/test/write.txt\", \"data\" : " +
                    "\"\\nblafdaaoildnfv\n\" } ] }" + UBUS_URL;
            // set up connection
            URL url = new URL(UBUS_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");
            // sends output
            OutputStream os = conn.getOutputStream();
            os.write(json.getBytes(StandardCharsets.UTF_8));
            os.close();
            // reads response
            InputStream in = new BufferedInputStream(conn.getInputStream());
            JSONObject myResponse = new JSONObject(IOUtils.toString(in, "UTF-8"));
            // closes connection
            in.close();
            conn.disconnect();
            System.out.println(myResponse.toString());
            return myResponse.toString();
        } catch (Exception e) {
            System.out.println(e);
            return null;
        }
    }
}
