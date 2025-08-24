import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Flight {
    // Properties for a single flight instance
    public int id;
    public String flightNumber;
    public String airlineName;
    public String originCity;
    public String destinationCity;
    public String departureDate;
    public String departureTime;
    public String arrivalTime;
    public int capacity;
    public double fare;
    public boolean available;

    // API-specific details
    public String duration;
    public String aircraftType;

    private static final Scanner scanner = new Scanner(System.in);
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");

    /**
     * UPDATED: Displays details of simulated flights from the database,
     * now including total capacity and calculated available seats.
     */
    public static void showFlightDetails() throws SQLException {
        // This query now calculates available seats by joining with reservations
        String query = """
            SELECT
                f.id, f.flight_number, a.name AS airline_name,
                orig.city AS origin_city, dest.city AS destination_city,
                f.departure_date, f.departure_time, f.fare, f.capacity,
                (f.capacity - COALESCE(SUM(r.number_of_seats), 0)) AS available_seats
            FROM flights f
            JOIN airlines a ON f.airline_id = a.id
            JOIN airports orig ON f.origin_airport_id = orig.id
            JOIN airports dest ON f.destination_airport_id = dest.id
            LEFT JOIN reservations r ON f.id = r.flight_id
            WHERE f.available = TRUE
            GROUP BY f.id, f.flight_number, a.name, orig.city, dest.city, f.departure_date, f.departure_time, f.fare, f.capacity;
        """;

        ResultSet rs = (ResultSet) Database.databaseQuery(query);

        if (rs == null) {
            AirlinesReservationSystem.setDisplayMessage(AirlinesReservationSystem.RED + "\t!! No available flights found in the database !!" + AirlinesReservationSystem.RESET);
            return;
        }

        String format = "║ %-5s │ %-12s │ %-15s │ %-15s │ %-14s │ %-10s │ %-9s │ %-12s ║\n";
        System.out.print(
                """
                ╔═══════╤══════════════╤═════════════════╤═════════════════╤════════════════╤════════════╤═══════════╤══════════════╗
                ║   ID  │ Flight No.   │     Airline     │      From       │       To       │  Capacity  │ Available │   Fare (INR) ║
                ╠═══════╧══════════════╧═════════════════╧═════════════════╧════════════════╧════════════╧═══════════╧══════════════╣
                """);

        while (rs.next()) {
            System.out.printf(format,
                    rs.getInt("id"),
                    rs.getString("flight_number"),
                    rs.getString("airline_name"),
                    rs.getString("origin_city"),
                    rs.getString("destination_city"),
                    rs.getInt("capacity"),
                    rs.getInt("available_seats"),
                    "Rs " + String.format("%.2f", rs.getDouble("fare"))
            );
        }
        System.out.print(
                """
                ╙───────┴──────────────┴─────────────────┴─────────────────┴────────────────┴────────────┴───────────┴──────────────╜
                """);
        rs.close();
    }

    /**
     * Adds a new simulated flight to the database.
     */
    public static void addFlight() throws SQLException {
        // This method is unchanged
        AirlinesReservationSystem.printCentered("--- Add New Simulated Flight ---");
        try {
            System.out.print("\n\t\t\t\tEnter Flight Number (e.g., 6E-2021): ");
            String flightNumber = scanner.nextLine();
            System.out.print("\t\t\t\tEnter Airline IATA Code (e.g., 6E for IndiGo): ");
            String airlineCode = scanner.nextLine().toUpperCase();
            System.out.print("\t\t\t\tEnter Origin Airport IATA Code (e.g., HYD): ");
            String originCode = scanner.nextLine().toUpperCase();
            System.out.print("\t\t\t\tEnter Destination Airport IATA Code (e.g., DEL): ");
            String destCode = scanner.nextLine().toUpperCase();
            System.out.print("\t\t\t\tEnter Departure Date (YYYY-MM-DD): ");
            String date = scanner.nextLine();
            System.out.print("\t\t\t\tEnter Departure Time (HH:MM:SS): ");
            String depTime = scanner.nextLine();
            System.out.print("\t\t\t\tEnter Arrival Time (HH:MM:SS): ");
            String arrTime = scanner.nextLine();
            System.out.print("\t\t\t\tEnter Seat Capacity: ");
            int capacity = Integer.parseInt(scanner.nextLine());
            System.out.print("\t\t\t\tEnter Fare (INR): ");
            double fare = Double.parseDouble(scanner.nextLine());

            int airlineId = getIdFromTable("airlines", "iata_code", airlineCode);
            int originId = getIdFromTable("airports", "iata_code", originCode);
            int destId = getIdFromTable("airports", "iata_code", destCode);

            if (airlineId == -1 || originId == -1 || destId == -1) {
                AirlinesReservationSystem.setDisplayMessage(AirlinesReservationSystem.RED + "\tError: Invalid Airline or Airport code. Please ensure they exist in the database." + AirlinesReservationSystem.RESET);
                return;
            }

            Database.databaseQuery(
                    "INSERT INTO flights (flight_number, airline_id, origin_airport_id, destination_airport_id, departure_date, departure_time, arrival_time, capacity, fare, available) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE);",
                    flightNumber, airlineId, originId, destId, date, depTime, arrTime, capacity, fare
            );
            AirlinesReservationSystem.setDisplayMessage(AirlinesReservationSystem.GREEN + "\tFlight added successfully!" + AirlinesReservationSystem.RESET);
        } catch (NumberFormatException e) {
            AirlinesReservationSystem.setDisplayMessage(AirlinesReservationSystem.RED + "\tInvalid number format." + AirlinesReservationSystem.RESET);
        }
    }

    /**
     * BUG FIX: Now validates that the flight ID exists before attempting to edit.
     */
    public static void editFlight(int flightId) throws SQLException {
        // First, check if the flight exists
        ResultSet rs = (ResultSet) Database.databaseQuery("SELECT * FROM flights WHERE id = ?;", flightId);

        // The rs.next() check is crucial. If it's false, the flight wasn't found.
        if (rs != null && rs.next()) {
            AirlinesReservationSystem.printCentered("--- Editing Flight ID: " + flightId + " ---");
            AirlinesReservationSystem.printCentered("(Press Enter to keep the current value)");

            try {
                System.out.print("\n\t\t\t\tUpdate Fare [" + rs.getDouble("fare") + "]: ");
                String newFare = scanner.nextLine();
                System.out.print("\t\t\t\tUpdate Availability [" + rs.getBoolean("available") + "]: ");
                String newAvailability = scanner.nextLine();

                if (!newFare.isEmpty()) {
                    Database.databaseQuery("UPDATE flights SET fare = ? WHERE id = ?;", Double.parseDouble(newFare), flightId);
                }
                if (!newAvailability.isEmpty()) {
                    Database.databaseQuery("UPDATE flights SET available = ? WHERE id = ?;", Boolean.parseBoolean(newAvailability), flightId);
                }
                AirlinesReservationSystem.setDisplayMessage(AirlinesReservationSystem.GREEN + "\tFlight details updated successfully!" + AirlinesReservationSystem.RESET);
            } catch (NumberFormatException e) {
                AirlinesReservationSystem.setDisplayMessage(AirlinesReservationSystem.RED + "\tInvalid number format." + AirlinesReservationSystem.RESET);
            } finally {
                if (rs != null) rs.close();
            }
        } else {
            // This message is shown if the flight ID does not exist
            AirlinesReservationSystem.setDisplayMessage(AirlinesReservationSystem.RED + "\tError: Flight with ID " + flightId + " not found." + AirlinesReservationSystem.RESET);
        }
    }

    /**
     * BUG FIX: Now checks if the flight was actually removed before showing a success message.
     */
    public static void removeFlight(int flightId) {
        System.out.print("\t\t\t\tAre you sure you want to remove flight ID " + flightId + "? This will also remove all reservations for it. (y/n): ");
        String confirmation = scanner.nextLine();

        if (confirmation.equalsIgnoreCase("y")) {
            // Get the number of affected rows from our updated database method
            int affectedRows = (int) Database.databaseQuery("DELETE FROM flights WHERE id = ?;", flightId);

            // Only show success if a row was actually deleted
            if (affectedRows > 0) {
                AirlinesReservationSystem.setDisplayMessage(AirlinesReservationSystem.GREEN + "\tFlight " + flightId + " has been removed." + AirlinesReservationSystem.RESET);
            } else {
                AirlinesReservationSystem.setDisplayMessage(AirlinesReservationSystem.RED + "\tError: Flight with ID " + flightId + " not found." + AirlinesReservationSystem.RESET);
            }
        } else {
            AirlinesReservationSystem.setDisplayMessage(AirlinesReservationSystem.YELLOW + "\tRemoval cancelled." + AirlinesReservationSystem.RESET);
        }
    }

    private static int getIdFromTable(String tableName, String columnName, String value) throws SQLException {
        // This method is unchanged
        ResultSet rs = (ResultSet) Database.databaseQuery("SELECT id FROM " + tableName + " WHERE " + columnName + " = ?;", value);
        if (rs != null && rs.next()) {
            int id = rs.getInt("id");
            rs.close();
            return id;
        }
        return -1;
    }
}