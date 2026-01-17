package ro.pub.cs.systems.eim.practicaltest02.network;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;
import cz.msebera.android.httpclient.util.EntityUtils;
import ro.pub.cs.systems.eim.practicaltest02.general.Utilities;
import ro.pub.cs.systems.eim.practicaltest02.model.WeatherForecastInformation;

public class CommunicationThread extends Thread{
    private final ServerThread serverThread;
    private final Socket socket;

    public CommunicationThread(ServerThread serverThread, Socket socket) {
        this.serverThread = serverThread;
        this.socket = socket;
    }

    @Override
    public void run() {
        if (socket == null) {
            Log.e("[PracticalTest02]", "[COMMUNICATION THREAD] Socket is null!");
            return;
        }
        try {
            BufferedReader bufferedReader = Utilities.getReader(socket);
            PrintWriter printWriter = Utilities.getWriter(socket);

            Log.i("[PracticalTest02]", "[COMMUNICATION THREAD] Waiting for parameters from client (city / information type!");
            String city = bufferedReader.readLine();
            String informationType = bufferedReader.readLine();
            if (city == null || city.isEmpty() || informationType == null || informationType.isEmpty()) {
                Log.e("[PracticalTest02]", "[COMMUNICATION THREAD] Error receiving parameters from client (city / information type!");
                return;
            }

            HashMap<String, WeatherForecastInformation> data = serverThread.getData();
            WeatherForecastInformation weatherForecastInformation;
            if (data.containsKey(city)) {
                Log.i("[PracticalTest02]", "[COMMUNICATION THREAD] Getting the information from the cache...");
                weatherForecastInformation = data.get(city);
            } else {
                Log.i("[PracticalTest02]", "[COMMUNICATION THREAD] Getting the information from the webservice...");
                HttpClient httpClient = new DefaultHttpClient();
                String pageSourceCode = "";
                HttpGet httpGet = new HttpGet("https://api.openweathermap.org/data/2.5/weather" + "?q=" + city + "&APPID=" + "e03c3b32cfb5a6f7069f2ef29237d87e" + "&units=" + "metric");
                HttpResponse httpGetResponse = httpClient.execute(httpGet);
                HttpEntity httpGetEntity = httpGetResponse.getEntity();
                if (httpGetEntity != null) {
                    pageSourceCode = EntityUtils.toString(httpGetEntity);
                }

                JSONObject content = new JSONObject(pageSourceCode);

                JSONArray weatherArray = content.getJSONArray("weather");
                JSONObject weather;
                StringBuilder condition = new StringBuilder();
                for (int i = 0; i < weatherArray.length(); i++) {
                    weather = weatherArray.getJSONObject(i);
                    condition.append(weather.getString("main")).append(" : ").append(weather.getString("description"));

                    if (i < weatherArray.length() - 1) {
                        condition.append(";");
                    }
                }

                JSONObject main = content.getJSONObject("main");
                String temperature = main.getString("temp");
                String pressure = main.getString("pressure");
                String humidity = main.getString("humidity");

                JSONObject wind = content.getJSONObject("wind");
                String windSpeed = wind.getString("speed");

                weatherForecastInformation = new WeatherForecastInformation(
                        temperature, windSpeed, condition.toString(), pressure, humidity
                );
                serverThread.setData(city, weatherForecastInformation);
            }
            if (weatherForecastInformation == null) {
                Log.e("[PracticalTest02]", "[COMMUNICATION THREAD] Weather Forecast Information is null!");
                return;
            }
            String result;
            switch(informationType) {
                case "all":
                    result = weatherForecastInformation.toString();
                    break;
                case "temperature":
                    result = weatherForecastInformation.getTemperature();
                    break;
                case "wind_speed":
                    result = weatherForecastInformation.getWindSpeed();
                    break;
                case "condition":
                    result = weatherForecastInformation.getCondition();
                    break;
                case "humidity":
                    result = weatherForecastInformation.getHumidity();
                    break;
                case "pressure":
                    result = weatherForecastInformation.getPressure();
                    break;
                default:
                    result = "[COMMUNICATION THREAD] Wrong information type (all / temperature / wind_speed / condition / humidity / pressure)!";
            }
            printWriter.println(result);
            printWriter.flush();
        } catch (IOException | JSONException ioException) {
            Log.e("[PracticalTest02]", "[COMMUNICATION THREAD] An exception has occurred: " + ioException.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException ioException) {
                Log.e("[PracticalTest02]", "[COMMUNICATION THREAD] An exception has occurred: " + ioException.getMessage());
            }
        }
    }
}
