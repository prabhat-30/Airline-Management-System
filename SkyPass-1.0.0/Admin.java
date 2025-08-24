import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

public class Admin {

    private final User currentUser;
    private static final Scanner scanner = new Scanner(System.in);

    public Admin(User user) {
        this.currentUser = user;
    }

    private void showAppTitle() {
        AirlinesReservationSystem.clearScreen();
        AirlinesReservationSystem.printCentered("\n");
        AirlinesReservationSystem.printCentered("╔══════════════════════════════════════════════════════╗");
        AirlinesReservationSystem.printCentered("║            Welcome to Skypass Admin Portal           ║");
        AirlinesReservationSystem.printCentered("╚══════════════════════════════════════════════════════╝");
        AirlinesReservationSystem.printCentered("\tLogged in as: " + AirlinesReservationSystem.YELLOW + currentUser.getFirstName() + " " + currentUser.getLastName() + AirlinesReservationSystem.RESET);
        AirlinesReservationSystem.showDisplayMessage();
    }

    // **NEW**: Method to display all users
    private void viewAllUsers() throws SQLException {
        ResultSet rs = (ResultSet) Database.databaseQuery("SELECT id, username, first_name, last_name, phone_no, role FROM users;");
        if (rs == null) {
            AirlinesReservationSystem.setDisplayMessage(AirlinesReservationSystem.RED + "\tNo users found in the system." + AirlinesReservationSystem.RESET);
            return;
        }

        String format = "║ %-6s │ %-20s │ %-25s │ %-15s │ %-10s ║\n";
        System.out.print(
                """
                ╔════════╤══════════════════════╤═══════════════════════════╤═════════════════╤════════════╗
                ║   ID   │       Username       │           Name            │   Phone Number  │    Role    ║
                ╠════════╪══════════════════════╪═══════════════════════════╪═════════════════╪════════════╣
                """);

        while (rs.next()) {
            String fullName = rs.getString("first_name") + " " + rs.getString("last_name");
            System.out.printf(format,
                    rs.getInt("id"),
                    rs.getString("username"),
                    fullName,
                    rs.getString("phone_no"),
                    rs.getString("role")
            );
        }
        System.out.print(
                """
                ╙────────┴──────────────────────┴───────────────────────────┴─────────────────┴────────────╜
                """);
        rs.close();
    }
    /**
     * BUG FIX: Now validates that the user exists before promoting them.
     * Removed the confusing logic that created a new user.
     */
    private void addAdmin() {
        System.out.print("\n\t\t\t\tEnter username to promote to admin: ");
        String username = scanner.nextLine();

        // Get the number of affected rows from the UPDATE query
        int affectedRows = (int) Database.databaseQuery("UPDATE users SET role = 'admin' WHERE username = ? AND role = 'passenger';", username);

        // Check if the update was successful
        if (affectedRows > 0) {
            AirlinesReservationSystem.setDisplayMessage(AirlinesReservationSystem.GREEN + "\tUser '" + username + "' has been promoted to an admin." + AirlinesReservationSystem.RESET);
        } else {
            AirlinesReservationSystem.setDisplayMessage(AirlinesReservationSystem.RED + "\tError: User '" + username + "' not found or is already an admin." + AirlinesReservationSystem.RESET);
        }
    }

    /**
     * BUG FIX: Now validates that the admin user exists before demoting them.
     */
    private void removeAdmin() {
        System.out.print("\n\t\t\t\tEnter username of the admin to demote to passenger: ");
        String username = scanner.nextLine();

        if (username.equalsIgnoreCase(currentUser.getUsername())) {
            AirlinesReservationSystem.setDisplayMessage(AirlinesReservationSystem.RED + "\tYou cannot demote yourself." + AirlinesReservationSystem.RESET);
            return;
        }

        // Get the number of affected rows
        int affectedRows = (int) Database.databaseQuery("UPDATE users SET role = 'passenger' WHERE username = ? AND role = 'admin';", username);

        // Check if the demotion was successful
        if (affectedRows > 0) {
            AirlinesReservationSystem.setDisplayMessage(AirlinesReservationSystem.GREEN + "\tAdmin '" + username + "' has been demoted to a passenger." + AirlinesReservationSystem.RESET);
        } else {
            AirlinesReservationSystem.setDisplayMessage(AirlinesReservationSystem.RED + "\tError: Admin user '" + username + "' not found." + AirlinesReservationSystem.RESET);
        }
    }


    public void adminMenu() throws Exception {
        int choice;
        do {
            showAppTitle();
            System.out.println("""
                    \t\t\t\t╔══════════════════════════════════════════════════════╗
                    \t\t\t\t║              -- Flight Management --                 ║
                    \t\t\t\t╟──────────────────────────────────────────────────────╢
                    \t\t\t\t║  1. Add a new simulated flight                       ║
                    \t\t\t\t║  2. Edit details of an existing flight               ║
                    \t\t\t\t║  3. Remove a flight from the system                  ║
                    \t\t\t\t║  4. View all simulated flights                       ║
                    \t\t\t\t╟------------------------------------------------------╢
                    \t\t\t\t║              -- User Management --                   ║
                    \t\t\t\t╟------------------------------------------------------╢
                    \t\t\t\t║  5. View all users                                   ║
                    \t\t\t\t║  6. Add / Promote an admin                           ║
                    \t\t\t\t║  7. Remove an admin                                  ║
                    \t\t\t\t╟──────────────────────────────────────────────────────╢
                    \t\t\t\t║  8. Logout                                           ║
                    \t\t\t\t╚══════════════════════════════════════════════════════╝
                            """);
            System.out.print("\t\t\t\tEnter your choice: ");
            try {
                choice = Integer.parseInt(scanner.nextLine());
                switch (choice) {
                    case 1:
                        showAppTitle();
                        Flight.addFlight();
                        break;
                    case 2:
                        showAppTitle();
                        Flight.showFlightDetails();
                        System.out.print("\n\t\t\t\tEnter the ID of the flight to edit: ");
                        int editId = Integer.parseInt(scanner.nextLine());
                        Flight.editFlight(editId);
                        break;
                    case 3:
                        showAppTitle();
                        Flight.showFlightDetails();
                        System.out.print("\n\t\t\t\tEnter the ID of the flight to remove: ");
                        int removeId = Integer.parseInt(scanner.nextLine());
                        Flight.removeFlight(removeId);
                        break;
                    case 4:
                        showAppTitle();
                        Flight.showFlightDetails();
                        System.out.print("\n\t\t\t\tPress enter to continue...");
                        scanner.nextLine();
                        break;
                    case 5:
                        showAppTitle();
                        viewAllUsers();
                        System.out.print("\n\t\t\t\tPress enter to continue...");
                        scanner.nextLine();
                        break;
                    case 6:
                        showAppTitle();
                        addAdmin();
                        break;
                    case 7:
                        showAppTitle();
                        removeAdmin();
                        break;
                    case 8:
                        AirlinesReservationSystem.setDisplayMessage(AirlinesReservationSystem.GREEN + "\tLogged out successfully" + AirlinesReservationSystem.RESET);
                        return;
                    default:
                        AirlinesReservationSystem.setDisplayMessage(AirlinesReservationSystem.RED + "\t  Invalid choice. Please try again" + AirlinesReservationSystem.RESET);
                }
            } catch (NumberFormatException e) {
                AirlinesReservationSystem.setDisplayMessage(AirlinesReservationSystem.RED + "\t  Invalid input. Please enter a number." + AirlinesReservationSystem.RESET);
                choice = 0;
            }
        } while (choice != 8);
    }
}
