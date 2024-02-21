package pt.ulisboa.tecnico.meditrack.securedocument;

/*
 * Prints an explanation of the available commands along 
 * with their corresponding arguments.
 */
public class Help {
    public static void main(String[] args) {
        System.out.println("Welcome to the help menu of PLACEHOLDER!");
        System.out.println("These are the available commands:");
        System.out.println("help - This command, shows all the available commands.");
        System.out.println("protect (input-file) (output-file) (client) - Generates output-file from " +
        "input-file, enforcing confidentiality and integrity.");
        System.out.println("check (input-file) (client) - Checks whether input-file's security is valid or not.");
        System.out.println("unprotect (input-file) (output-file) (client) -  Opposite of protect. " +
        "Generates output-file from input-file, removing its security and regenerating the original file");
        System.out.println("Arguments within '()' are *mandatory*.");
    }
}