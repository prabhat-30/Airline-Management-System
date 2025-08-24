import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * A simple data object (POJO) to hold flight details parsed from an API response.
 */
public class ApiFlight {
    private String flightNumber;
    private String airlineName; // Full name, e.g., "SpiceJet"
    private String airlineIata; // e.g., "SG"
    private String originAirportName; // Full name, e.g., "Rajiv Gandhi International Airport"
    private String originCityName; // e.g., "Hyderabad"
    private String originAirportIata; // e.g., "HYD"
    private String destinationAirportName;
    private String destinationCityName;
    private String destinationAirportIata;
    private LocalDate departureDate;
    private LocalTime departureTime;
    private LocalTime arrivalTime;
    private BigDecimal fare;

    // --- Getters and Setters for all fields ---
    public String getFlightNumber() { return flightNumber; }
    public void setFlightNumber(String flightNumber) { this.flightNumber = flightNumber; }
    public String getAirlineName() { return airlineName; }
    public void setAirlineName(String airlineName) { this.airlineName = airlineName; }
    public String getAirlineIata() { return airlineIata; }
    public void setAirlineIata(String airlineIata) { this.airlineIata = airlineIata; }
    public String getOriginAirportName() { return originAirportName; }
    public void setOriginAirportName(String originAirportName) { this.originAirportName = originAirportName; }
    public String getOriginCityName() { return originCityName; }
    public void setOriginCityName(String originCityName) { this.originCityName = originCityName; }
    public String getOriginAirportIata() { return originAirportIata; }
    public void setOriginAirportIata(String originAirportIata) { this.originAirportIata = originAirportIata; }
    public String getDestinationAirportName() { return destinationAirportName; }
    public void setDestinationAirportName(String destinationAirportName) { this.destinationAirportName = destinationAirportName; }
    public String getDestinationCityName() { return destinationCityName; }
    public void setDestinationCityName(String destinationCityName) { this.destinationCityName = destinationCityName; }
    public String getDestinationAirportIata() { return destinationAirportIata; }
    public void setDestinationAirportIata(String destinationAirportIata) { this.destinationAirportIata = destinationAirportIata; }
    public LocalDate getDepartureDate() { return departureDate; }
    public void setDepartureDate(LocalDate departureDate) { this.departureDate = departureDate; }
    public LocalTime getDepartureTime() { return departureTime; }
    public void setDepartureTime(LocalTime departureTime) { this.departureTime = departureTime; }
    public LocalTime getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(LocalTime arrivalTime) { this.arrivalTime = arrivalTime; }
    public BigDecimal getFare() { return fare; }
    public void setFare(BigDecimal fare) { this.fare = fare; }
}
