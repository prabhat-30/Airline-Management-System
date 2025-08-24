import java.sql.*;
import java.util.Set;
import java.util.UUID;

/**
 * Service class to handle the logic of booking a flight.
 * NOW INCLUDES seat selection management during the booking process.
 */
public class FlightBookingService {

    private final Connection connection;

    public FlightBookingService(Connection dbConnection) {
        this.connection = dbConnection;
    }

    /**
     * Main method to handle booking. It gets/creates the flight, manages seat selection,
     * and then creates the reservation.
     *
     * @param userId The ID of the user booking the flight.
     * @param flightData The flight details from the API.
     * @param numSeats The number of seats to book.
     * @return The generated Ticket ID if successful, null otherwise.
     */
    public String bookFlight(int userId, ApiFlight flightData, int numSeats) throws SQLException {
        // Step 1: Get or create the flight record and get its ID
        int flightId = getOrCreateFlight(flightData);
        if (flightId == -1) {
            System.err.println("Failed to get or create the flight in the database.");
            return null; // Exit if flight creation fails
        }

        // Step 2: Manage Seat Selection
        SeatManager seatManager = new SeatManager(connection);
        Set<String> bookedSeats = seatManager.getBookedSeats(flightId);
        int capacity = getFlightCapacity(flightId);

        seatManager.displaySeatMap(capacity, bookedSeats);
        String selectedSeats = seatManager.selectSeats(numSeats, bookedSeats);


        // Step 3: Create the reservation with the selected seats
        return createReservation(userId, flightId, numSeats, selectedSeats);
    }

    /**
     * Checks if a flight exists in the DB based on flight number and date. If not, it creates it.
     * Also creates airline and airport records if they don't exist.
     *
     * @param flight The flight data from the API.
     * @return The database ID of the flight.
     */
    private int getOrCreateFlight(ApiFlight flight) throws SQLException {
        String checkSql = "SELECT id FROM flights WHERE flight_number = ? AND departure_date = ?";
        try (PreparedStatement ps = connection.prepareStatement(checkSql)) {
            ps.setString(1, flight.getFlightNumber());
            ps.setDate(2, Date.valueOf(flight.getDepartureDate()));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("id"); // Flight already exists
            }
        }

        // Flight doesn't exist, so create it by getting dependencies first
        int airlineId = getOrCreateAirline(flight.getAirlineName(), flight.getAirlineIata());
        int originAirportId = getOrCreateAirport(flight.getOriginAirportIata());
        int destAirportId = getOrCreateAirport(flight.getDestinationAirportIata());

        String insertSql = "INSERT INTO flights (flight_number, airline_id, origin_airport_id, destination_airport_id, departure_date, departure_time, arrival_time, fare) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, flight.getFlightNumber());
            ps.setInt(2, airlineId);
            ps.setInt(3, originAirportId);
            ps.setInt(4, destAirportId);
            ps.setDate(5, Date.valueOf(flight.getDepartureDate()));
            ps.setTime(6, Time.valueOf(flight.getDepartureTime()));
            ps.setTime(7, Time.valueOf(flight.getArrivalTime()));
            ps.setBigDecimal(8, flight.getFare());

            int affectedRows = ps.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getInt(1); // Return the new flight ID
                    }
                }
            }
        }
        return -1; // Indicate failure
    }

    /**
     * Creates a reservation record in the database, now including the selected seat numbers.
     *
     * @param userId The user's ID.
     * @param flightId The flight's ID.
     * @param numSeats The number of seats.
     * @param seatNumbers A comma-separated string of selected seats.
     * @return The unique ticket ID.
     */
    private String createReservation(int userId, int flightId, int numSeats, String seatNumbers) throws SQLException {
        String ticketId = "TKT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String sql = "INSERT INTO reservations (ticket_id, user_id, flight_id, number_of_seats, seat_numbers) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, ticketId);
            ps.setInt(2, userId);
            ps.setInt(3, flightId);
            ps.setInt(4, numSeats);
            ps.setString(5, seatNumbers); // Add seat numbers to the query
            ps.executeUpdate();
        }
        return ticketId;
    }

    private int getFlightCapacity(int flightId) throws SQLException {
        String sql = "SELECT capacity FROM flights WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, flightId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("capacity");
            }
        }
        // Return a default capacity if for some reason it's not found (should not happen)
        return 180;
    }

    private int getOrCreateAirline(String name, String iata) throws SQLException {
        // ... (This method is unchanged)
        String checkSql = "SELECT id FROM airlines WHERE iata_code = ?";
        try (PreparedStatement ps = connection.prepareStatement(checkSql)) {
            ps.setString(1, iata);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        }

        String insertSql = "INSERT INTO airlines (name, iata_code) VALUES (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name != null ? name : "Unknown Airline");
            ps.setString(2, iata);
            ps.executeUpdate();
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        }
        throw new SQLException("Creating airline failed, no ID obtained.");
    }

    private int getOrCreateAirport(String iata) throws SQLException {
        // ... (This method is unchanged)
        String checkSql = "SELECT id FROM airports WHERE iata_code = ?";
        try (PreparedStatement ps = connection.prepareStatement(checkSql)) {
            ps.setString(1, iata);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        }

        String insertSql = "INSERT INTO airports (name, city, iata_code) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, "Airport " + iata);
            ps.setString(2, "City " + iata);
            ps.setString(3, iata);
            ps.executeUpdate();
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        }
        throw new SQLException("Creating airport failed, no ID obtained.");
    }
}