import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.StringJoiner;

/**
 * Manages the logic for seat selection, including fetching booked seats,
 * displaying the seat map, and validating user input.
 */
public class SeatManager {

    private final Connection connection;
    private static final Scanner scanner = new Scanner(System.in);

    public SeatManager(Connection dbConnection) {
        this.connection = dbConnection;
    }

    /**
     * Fetches all currently booked seats for a given flight from the database.
     * @param flightId The ID of the flight.
     * @return A Set of strings, where each string is a booked seat number (e.g., "1A").
     */
    public Set<String> getBookedSeats(int flightId) throws SQLException {
        Set<String> bookedSeats = new HashSet<>();
        String query = "SELECT seat_numbers FROM reservations WHERE flight_id = ? AND seat_numbers IS NOT NULL;";
        ResultSet rs = (ResultSet) Database.databaseQuery(query, flightId);

        if (rs != null) {
            while (rs.next()) {
                String seatsStr = rs.getString("seat_numbers");
                if (seatsStr != null && !seatsStr.isEmpty()) {
                    String[] seats = seatsStr.split(",\\s*");
                    for (String seat : seats) {
                        bookedSeats.add(seat.toUpperCase());
                    }
                }
            }
        }
        return bookedSeats;
    }
    /**
     * Displays a visual seat map in the console.
     * @param capacity The total seat capacity of the flight.
     * @param bookedSeats A set of already booked seats.
     */
    public void displaySeatMap(int capacity, Set<String> bookedSeats) {
        AirlinesReservationSystem.printCentered("--- Select Your Seat(s) ---");
        System.out.println("\n\t\t[  ] = Available, [XX] = Booked\n");

        char[] seatLetters = {'A', 'B', 'C', ' ', 'D', 'E', 'F'};
        int rows = (int) Math.ceil(capacity / 6.0);

        // Print header row with letters
        System.out.print("\t\t\t\t");
        for (char letter : seatLetters) {
            if (letter == ' ') {
                System.out.print("   "); // Aisle space
            } else {
                System.out.printf(" [%c] ", letter);
            }
        }
        System.out.println("\n");


        for (int i = 1; i <= rows; i++) {
            // Print row number at the start
            System.out.printf("\t\tRow %-5d\t", i);

            for (char letter : seatLetters) {
                if (letter == ' ') {
                    System.out.print("   "); // Aisle space
                    continue;
                }
                String seatId = "" + i + letter;
                if (bookedSeats.contains(seatId)) {
                    System.out.print("[XX]");
                } else {
                    // CORRECTED: This now correctly prints the seat ID like 1A, 12F
                    System.out.printf("[%2s]", seatId);
                }
            }
            System.out.println();
        }
    }

    /**
     * Prompts the user to select a specific number of seats and validates their choice.
     * @param numToSelect The number of seats the user needs to select.
     * @param bookedSeats A set of already booked seats.
     * @return A comma-separated string of the selected seat numbers.
     */
    public String selectSeats(int numToSelect, Set<String> bookedSeats) {
        StringJoiner selectedSeats = new StringJoiner(", ");
        int seatsSelected = 0;
        while (seatsSelected < numToSelect) {
            System.out.printf("\n\t\t\t\tEnter desired seat number for passenger %d (e.g., 1A, 12F): ", seatsSelected + 1);
            String input = scanner.nextLine().toUpperCase();

            if (bookedSeats.contains(input)) {
                System.out.println(AirlinesReservationSystem.RED + "\t\t\t\tSorry, seat " + input + " is already taken. Please choose another." + AirlinesReservationSystem.RESET);
            } else if (selectedSeats.toString().contains(input)) {
                System.out.println(AirlinesReservationSystem.YELLOW + "\t\t\t\tYou have already selected that seat." + AirlinesReservationSystem.RESET);
            } else {
                // Basic validation for format (e.g., number followed by letter)
                if (input.matches("^\\d+[A-F]$")) {
                    selectedSeats.add(input);
                    seatsSelected++;
                    System.out.println(AirlinesReservationSystem.GREEN + "\t\t\t\tSeat " + input + " selected." + AirlinesReservationSystem.RESET);
                } else {
                    System.out.println(AirlinesReservationSystem.RED + "\t\t\t\tInvalid seat format. Please use format like '1A', '2B', etc." + AirlinesReservationSystem.RESET);
                }
            }
        }
        return selectedSeats.toString();
    }
}