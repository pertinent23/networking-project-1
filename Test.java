import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Test {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 2629;

    public static void main(String[] args) {
        Pattern command_regex = Pattern.compile("^([A-Z]+)(\\s+[A-Z]{2,})?(\\s+[A-Z]{2,})?(\\s+[A-Za-z]|\\s+\\-?[0-9]+)?(\\s+[A-Za-z]|\\s+\\-?[0-9]+)?\\s*\\r\\n\\r\\n?$");
        Matcher matcher = command_regex.matcher("NO GAME -50\r\n\r\n");

        if (matcher.find()) {
            System.out.println("Matched");
            int groupCount = matcher.groupCount();
            System.out.println("Number of groups: " + groupCount);
            for (int i = 0; i <= groupCount; i++) {
                System.out.println("Group " + i + ": " + matcher.group(i));
            }
        } else {
            System.out.println("Not matched");
        }

        System.out.println("NO GAME -50\r\n\r\n \r\n\r\n".split("\r\n\r\n").length);

        String groupedRequest = "START BOT X\r\n\r\nPUT a b\r\n\r\n";

        // Le "try-with-resources" garantit que le socket et les flux seront fermés automatiquement.
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             OutputStream out = socket.getOutputStream();
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            System.out.println("✅ Connecté au serveur sur " + SERVER_HOST + ":" + SERVER_PORT);

            // 1. Envoyer la totalité de la requête groupée
            System.out.println("➡️  Envoi de la requête groupée...");
            // System.out.println(groupedRequest); // Décommentez pour voir la requête exacte
            out.write(groupedRequest.getBytes(StandardCharsets.UTF_8));
            out.flush(); // Force l'envoi immédiat des données

            // 2. Lire la ou les réponses du serveur
            System.out.println("⬅️  En attente de la réponse du serveur...");
            String serverResponse;
            // On lit ligne par ligne jusqu'à ce que le serveur ferme la connexion (readLine() retourne null)
            while ((serverResponse = in.readLine()) != null) {
                System.out.println("Reçu:" + serverResponse);
            }

        } catch (IOException e) {
            System.err.println("❌ Erreur de communication: " + e.getMessage());
        }

        System.out.println("🔌 Connexion fermée.");
    }
}
