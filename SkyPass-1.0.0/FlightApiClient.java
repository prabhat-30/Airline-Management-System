import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FlightApiClient {

    // **IMPORTANT**: Replace with your keys from Amadeus for Developers
    private static final String API_KEY = "R1aYPqjZrGdHwQE0yYE7GjEaZ8skgRiV";
    private static final String API_SECRET = "xUx0aAwheCbeZymZ";

    private static final OkHttpClient client = new OkHttpClient();
    private static final Gson gson = new Gson();
    private static String accessToken = null;

    private static void authenticate() throws IOException {
        String authUrl = "https://test.api.amadeus.com/v1/security/oauth2/token";
        RequestBody formBody = new FormBody.Builder()
                .add("grant_type", "client_credentials")
                .add("client_id", API_KEY)
                .add("client_secret", API_SECRET)
                .build();

        Request request = new Request.Builder()
                .url(authUrl)
                .post(formBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Authentication failed: " + response.body().string());
            }
            String jsonData = response.body().string();
            JsonObject jsonObject = gson.fromJson(jsonData, JsonObject.class);
            accessToken = jsonObject.get("access_token").getAsString();
        }
    }

    public static List<Flight> findFlights(String origin, String destination, String date) throws IOException {
        if (accessToken == null) {
            authenticate();
        }

        List<Flight> flights = new ArrayList<>();
        String searchUrl = "https://test.api.amadeus.com/v2/shopping/flight-offers" +
                "?originLocationCode=" + origin +
                "&destinationLocationCode=" + destination +
                "&departureDate=" + date +
                "&adults=1" +
                "&nonStop=true";

        Request request = new Request.Builder()
                .url(searchUrl)
                .get()
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                System.err.println("Error fetching flights. Status code: " + response.code());
                System.err.println("Server says: " + response.body().string());
                return flights;
            }

            String jsonData = response.body().string();
            JsonObject jsonObject = gson.fromJson(jsonData, JsonObject.class);

            if (jsonObject.has("data")) {
                JsonArray data = jsonObject.getAsJsonArray("data");
                int tempId = 1;
                for (int i = 0; i < data.size(); i++) {
                    JsonObject flightOffer = data.get(i).getAsJsonObject();
                    JsonObject itinerary = flightOffer.getAsJsonArray("itineraries").get(0).getAsJsonObject();
                    JsonObject segment = itinerary.getAsJsonArray("segments").get(0).getAsJsonObject();

                    Flight flight = new Flight();
                    flight.id = tempId++;

                    String carrierCode = segment.get("carrierCode").getAsString();
                    String flightNumber = segment.get("number").getAsString();
                    flight.flightNumber = carrierCode + "-" + flightNumber;

                    flight.originCity = segment.getAsJsonObject("departure").get("iataCode").getAsString();
                    flight.destinationCity = segment.getAsJsonObject("arrival").get("iataCode").getAsString();

                    String departureDateTime = segment.getAsJsonObject("departure").get("at").getAsString();
                    flight.departureTime = departureDateTime.substring(11, 16);
                    flight.departureDate = date;

                    String arrivalDateTime = segment.getAsJsonObject("arrival").get("at").getAsString();
                    flight.arrivalTime = arrivalDateTime.substring(11, 16);
                    flight.duration = itinerary.get("duration").getAsString().replace("PT", "").replace("H", "H ").replace("M", "M");
                    flight.aircraftType = segment.getAsJsonObject("aircraft").get("code").getAsString();

                    double priceInEur = Double.parseDouble(flightOffer.getAsJsonObject("price").get("total").getAsString());
                    flight.fare = priceInEur * 90;

                    flights.add(flight);
                }
            }
        }
        return flights;
    }
}
