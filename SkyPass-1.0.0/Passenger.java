import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Scanner;

/**
 * UPDATED Passenger class.
 * The booking process now includes seat selection via the FlightBookingService.
 * The e-ticket generation now displays the selected seat numbers.
 */
public class Passenger {

    private final User currentUser;
    private static final Scanner scanner = new Scanner(System.in);

    public Passenger(User user) {
        this.currentUser = user;
    }

    private void showAppTitle() {
        AirlinesReservationSystem.clearScreen();
        AirlinesReservationSystem.printCentered("\n");
        AirlinesReservationSystem.printCentered("╔══════════════════════════════════════════════════════╗");
        AirlinesReservationSystem.printCentered("║          Welcome to Skypass Passenger Portal         ║");
        AirlinesReservationSystem.printCentered("╚══════════════════════════════════════════════════════╝");
        AirlinesReservationSystem.printCentered(
                "\t  Logged in as: " + AirlinesReservationSystem.YELLOW + currentUser.getFirstName() + " " + currentUser.getLastName() + " (" + currentUser.getUsername() + ") " + AirlinesReservationSystem.RESET);
        AirlinesReservationSystem.showDisplayMessage();
    }

    public void passengerMenu() throws Exception {
        int choice;
        do {
            showAppTitle();
            System.out.println("""
                    \t\t\t\t╔══════════════════════════════════════════════════════╗
                    \t\t\t\t║  1. Search and Book a Flight (via API)               ║
                    \t\t\t\t║  2. View My Reservations                             ║
                    \t\t\t\t╟──────────────────────────────────────────────────────╢
                    \t\t\t\t║  3. Logout                                           ║
                    \t\t\t\t╚══════════════════════════════════════════════════════╝
                    """);
            System.out.print("\t\t\t\tEnter your choice: ");
            try {
                choice = Integer.parseInt(scanner.nextLine());
                switch (choice) {
                    case 1:
                        searchAndBookFlights();
                        System.out.print("\n\n\t\t\t\tPress enter to return to the menu...");
                        scanner.nextLine();
                        break;
                    case 2:
                        showAppTitle();
                        showMyReservations();
                        System.out.print("\n\n\t\t\t\tPress enter to continue...");
                        scanner.nextLine();
                        break;
                    case 3:
                        AirlinesReservationSystem.setDisplayMessage(AirlinesReservationSystem.GREEN + "\tLogged out successfully" + AirlinesReservationSystem.RESET);
                        return;
                    default:
                        AirlinesReservationSystem.setDisplayMessage(AirlinesReservationSystem.RED + "\t    ERROR ! Please enter valid option !" + AirlinesReservationSystem.RESET);
                }
            } catch (NumberFormatException e) {
                AirlinesReservationSystem.setDisplayMessage(AirlinesReservationSystem.RED + "\t    ERROR ! Please enter a valid number !" + AirlinesReservationSystem.RESET);
                choice = 0;
            }
        } while (choice != 3);
    }

    public void searchAndBookFlights() {
        try {
            showAppTitle();
            System.out.print("\t\t\t\tEnter the origin airport IATA code (e.g., HYD): ");
            String origin = scanner.nextLine().toUpperCase();
            System.out.print("\t\t\t\tEnter the destination airport IATA code (e.g., DEL): ");
            String destination = scanner.nextLine().toUpperCase();
            System.out.print("\t\t\t\tEnter the future departure date (YYYY-MM-DD): ");
            String date = scanner.nextLine();

            // Assumes FlightApiClient.findFlights() returns a List<Flight>
            List<Flight> availableFlights = FlightApiClient.findFlights(origin, destination, date);

            if (availableFlights.isEmpty()) {
                AirlinesReservationSystem.setDisplayMessage(AirlinesReservationSystem.RED + "\tSorry! No future flights found for the given route and date." + AirlinesReservationSystem.RESET);
                return;
            }

            showAppTitle();
            displayApiFlights(availableFlights);

            System.out.print("\n\t\t\t\tEnter the ID of the flight you wish to book (or 0 to return): ");
            int selection = Integer.parseInt(scanner.nextLine());

            if (selection > 0 && selection <= availableFlights.size()) {
                Flight selectedFlight = availableFlights.get(selection - 1);
                ApiFlight flightToBook = convertToApiFlight(selectedFlight, date);

                System.out.print("\t\t\t\tEnter number of seats to book: ");
                int numSeats = Integer.parseInt(scanner.nextLine());

                Connection conn = Database.getConnection();
                FlightBookingService bookingService = new FlightBookingService(conn);
                // The booking service now handles the entire seat selection process internally
                String ticketId = bookingService.bookFlight(currentUser.getId(), flightToBook, numSeats);

                if (ticketId != null) {
                    String successMessage = AirlinesReservationSystem.GREEN + "\tBooking successful!" + AirlinesReservationSystem.RESET;
                    AirlinesReservationSystem.setDisplayMessage(successMessage);

                    // Call the method here to print the ticket
                    System.out.println("\n");
                    generateTicket(ticketId);

                } else {
                    AirlinesReservationSystem.setDisplayMessage(AirlinesReservationSystem.RED + "\tBooking failed. Please try again." + AirlinesReservationSystem.RESET);
                }
            }

        } catch (NumberFormatException e) {
            AirlinesReservationSystem.setDisplayMessage(AirlinesReservationSystem.RED + "\tInvalid input. Please enter a number." + AirlinesReservationSystem.RESET);
        } catch (SQLException e) {
            AirlinesReservationSystem.setDisplayMessage(AirlinesReservationSystem.RED + "\tA database error occurred during booking." + AirlinesReservationSystem.RESET);
            System.err.println("Database Error: " + e.getMessage());
        } catch (Exception e) {
            AirlinesReservationSystem.setDisplayMessage(AirlinesReservationSystem.RED + "\tAn error occurred while fetching flight data." + AirlinesReservationSystem.RESET);
            System.err.println("API Error: " + e.getMessage());
        }
    }

    private void displayApiFlights(List<Flight> flights) {
        AirlinesReservationSystem.printCentered("--- Future Flights Found ---");
        String headerFormat = "║ %-4s │ %-12s │ %-6s │ %-6s │ %-8s │ %-8s │ %-8s │ %-8s │ %-10s ║\n";
        String dataFormat = "║ %-4d │ %-12s │ %-6s │ %-6s │ %-8s │ %-8s │ %-8s │ %-8s │ Rs %-7.0f ║\n";
        String border = "╠══════╪══════════════╪════════╪════════╪══════════╪══════════╪══════════╪══════════╪════════════╣\n";

        System.out.print(
                "╔══════╤══════════════╤════════╤════════╤══════════╤══════════╤══════════╤══════════╤════════════╗\n");
        System.out.printf(headerFormat, "ID", "Flight No.", "From", "To", "Depart", "Arrive", "Duration", "Aircraft", "Fare (INR)");
        System.out.print(border);

        for (int i = 0; i < flights.size(); i++) {
            Flight flight = flights.get(i);
            System.out.printf(dataFormat,
                    i + 1,
                    flight.flightNumber,
                    flight.originCity,
                    flight.destinationCity,
                    flight.departureTime,
                    flight.arrivalTime,
                    flight.duration,
                    flight.aircraftType,
                    flight.fare);
        }
        System.out.print(
                "╚══════╧══════════════╧════════╧════════╧══════════╧══════════╧══════════╧══════════╧════════════╝\n");
    }

    public void showMyReservations() throws SQLException {
        // This method is unchanged
        String query = """
                    SELECT
                        r.ticket_id, f.flight_number, a.name AS airline_name,
                        f.departure_date, orig.city AS origin_city, dest.city AS destination_city
                    FROM reservations r
                    JOIN flights f ON r.flight_id = f.id
                    JOIN airlines a ON f.airline_id = a.id
                    JOIN airports orig ON f.origin_airport_id = orig.id
                    JOIN airports dest ON f.destination_airport_id = dest.id
                    WHERE r.user_id = ?;
                """;
        ResultSet rs = (ResultSet) Database.databaseQuery(query, currentUser.getId());

        if (rs == null || !rs.isBeforeFirst()) {
            AirlinesReservationSystem.setDisplayMessage(AirlinesReservationSystem.YELLOW + "\tYou have no reservations." + AirlinesReservationSystem.RESET);
            return;
        }

        AirlinesReservationSystem.printCentered("--- Your Reservations ---");
        while (rs.next()) {
            System.out.printf("\n\t\t\t\tTicket ID: %s | Flight: %s (%s) | Date: %s | Route: %s -> %s",
                    rs.getString("ticket_id"),
                    rs.getString("flight_number"),
                    rs.getString("airline_name"),
                    rs.getString("departure_date"),
                    rs.getString("origin_city"),
                    rs.getString("destination_city")
            );
        }
        System.out.println();
    }

    private ApiFlight convertToApiFlight(Flight flight, String departureDateStr) {
        // This method is unchanged
        ApiFlight apiFlight = new ApiFlight();
        apiFlight.setFlightNumber(flight.flightNumber);
        apiFlight.setAirlineIata(flight.flightNumber.substring(0, 2));
        apiFlight.setAirlineName(flight.airlineName);
        apiFlight.setOriginAirportIata(flight.originCity);
        apiFlight.setOriginCityName(flight.originCity);
        apiFlight.setDestinationAirportIata(flight.destinationCity);
        apiFlight.setDestinationCityName(flight.destinationCity);
        apiFlight.setDepartureDate(LocalDate.parse(departureDateStr));
        apiFlight.setDepartureTime(LocalTime.parse(flight.departureTime));
        apiFlight.setArrivalTime(LocalTime.parse(flight.arrivalTime));
        apiFlight.setFare(BigDecimal.valueOf(flight.fare));
        return apiFlight;
    }

    /**
     * CORRECTED: Generates a perfectly formatted ticket with aligned borders.
     *
     * @param ticketId The unique ID of the ticket to generate.
     */
    public void generateTicket(String ticketId) throws SQLException {
        String query = """
                    SELECT
                        r.ticket_id, r.number_of_seats, r.seat_numbers,
                        u.first_name, u.last_name,
                        f.flight_number, f.departure_date, f.departure_time, f.arrival_time, f.fare,
                        al.name AS airline_name,
                        orig.name AS origin_airport_name, orig.city AS origin_city,
                        dest.name AS destination_airport_name, dest.city AS destination_city
                    FROM reservations r
                    JOIN users u ON r.user_id = u.id
                    JOIN flights f ON r.flight_id = f.id
                    JOIN airlines al ON f.airline_id = al.id
                    JOIN airports orig ON f.origin_airport_id = orig.id
                    JOIN airports dest ON f.destination_airport_id = dest.id
                    WHERE r.ticket_id = ?;
                """;

        ResultSet rs = (ResultSet) Database.databaseQuery(query, ticketId);

        if (rs != null && rs.next()) {
            // Format each line's content separately
            String passengerLine = String.format("Passenger: %-25s   Ticket ID: %s",
                    rs.getString("first_name") + " " + rs.getString("last_name"),
                    rs.getString("ticket_id"));

            String flightLine = String.format("Flight: %-28s   Date: %s",
                    rs.getString("flight_number") + " (" + rs.getString("airline_name") + ")",
                    rs.getDate("departure_date"));

            String fromLine = String.format("From: %-30s   (%s)",
                    rs.getString("origin_airport_name"),
                    rs.getString("origin_city"));

            String toLine = String.format("To:   %-30s   (%s)",
                    rs.getString("destination_airport_name"),
                    rs.getString("destination_city"));

            String timeLine = String.format("Departure: %-25s   Arrival: %s",
                    rs.getTime("departure_time"),
                    rs.getTime("arrival_time"));

            String seatsLine = String.format("Seats Booked: %-22s   Total Fare: Rs %.2f",
                    rs.getString("seat_numbers"),
                    rs.getBigDecimal("fare").multiply(java.math.BigDecimal.valueOf(rs.getInt("number_of_seats"))));

            // Build the final ticket using format specifiers to ensure alignment
            String ticketDetails = String.format("""
                            +--------------------------------------------------------------------+
                            |                        SKYPASS E-TICKET                            |
                            +--------------------------------------------------------------------+
                            | %-66s |
                            | %-66s |
                            | %-66s |
                            | %-66s |
                            | %-66s |
                            | %-66s |
                            | %-66s |
                            | %-66s |
                            +--------------------------------------------------------------------+
                            |          Thank you for flying with us! Safe travels.               |
                            +--------------------------------------------------------------------+
                            """,
                    passengerLine,
                    flightLine,
                    "", // Blank line
                    fromLine,
                    toLine,
                    "", // Blank line
                    timeLine,
                    seatsLine
            );

            System.out.println(ticketDetails);

            try (FileWriter writer = new FileWriter(ticketId + ".txt")) {
                writer.write(ticketDetails);
                System.out.println("\n\t\t\t\tTicket also saved to " + ticketId + ".txt");
            } catch (IOException e) {
                System.err.println("Could not save ticket to file: " + e.getMessage());
            }
        }
    }
}