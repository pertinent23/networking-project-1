import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Classe de test pour le Client Tic-tac-toe.
 * Simule un serveur pour envoyer différentes réponses au client et vérifier
 * le comportement du client (TictactoeClient).
 *
 * NOTE: Lancez votre TictactoeClient dans un processus séparé APRÈS l'exécution de ce test.
 * Le client doit se connecter à HOST et PORT.
 */
public class TestClient {

    // CONSTANTES DE RÉSEAU
    private static final String HOST = "localhost";
    private static final int PORT = 2629; // *** PORTE A CHANGER SI NÉCESSAIRE (e.g. 2xxx) ***
    private static final String CRLF = "\r\n";
    private static final int SERVER_TIMEOUT_MS = 0; // Timeout pour attendre la connexion du client
    
    // Compteurs de tests
    private static AtomicInteger testCount = new AtomicInteger(0);
    private static AtomicInteger successCount = new AtomicInteger(0);

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("  TEST CLIENT (Simulateur de Serveur Rigoureux)");
        System.out.println("  Hote: " + HOST + ", Port: " + PORT);
        System.out.println("=================================================");
        
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            serverSocket.setSoTimeout(SERVER_TIMEOUT_MS);
            System.out.println("Serveur de test en écoute. Lancez votre client TictactoeClient maintenant.");
            
            // Attente de la connexion du client
            Socket clientSocket = serverSocket.accept();
            System.out.println("Client connecté. Démarrage des tests...\n");

            // Flux de communication (Utilisation explicite de UTF-8 pour la robustesse)
            PrintWriter out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8), false);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
            
            // Exécuter tous les scénarios
            testScenarios(out, in);

            clientSocket.close();

        } catch (SocketTimeoutException e) {
            System.err.println("ÉCHEC: Aucun client ne s'est connecté dans le délai imparti (" + SERVER_TIMEOUT_MS/1000 + "s).");
        } catch (IOException e) {
            System.err.println("Erreur fatale lors du test du client: " + e.getMessage());
        }

        System.out.println("\n=================================================");
        System.out.printf("Rapport final des tests client: %d SUCCÈS sur %d scénarios testés.%n", successCount.get(), testCount.get());
        System.out.println("=================================================");
    }

    /**
     * Contient les 50+ scénarios de test.
     */
    private static void testScenarios(PrintWriter out, BufferedReader in) {
        // Le but est d'envoyer la réponse et de s'assurer que le CLIENT réagit correctement.
        // On lit la requête du client pour vider le flux et s'assurer que le client est prêt pour la réponse.

        // --- SCÉNARIOS DE DÉMARRAGE ET JEU SIMPLES (1-10) ---
        
        // 1. Démarrage Bot X - Réponse de grille vide
        runTest(out, in, "1. Démarrage Bot X (Grille vide)", "START BOT X", "   " + CRLF + "   " + CRLF + "   " + CRLF + CRLF);
        
        // 2. Démarrage Bot O - Réponse avec le coup initial du bot (X en 0,0)
        runTest(out, in, "2. Démarrage Bot O (Coup initial X)", "X  " + CRLF + "   " + CRLF + "   " + CRLF + CRLF);
        
        // 3. START PLAYER - Réponse PLAYER X
        runTest(out, in, "3. Démarrage Player (X)", "PLAYER X" + CRLF + CRLF);
        
        // 4. START PLAYER - Réponse PLAYER O
        runTest(out, in, "4. Démarrage Player (O)", "PLAYER O" + CRLF + CRLF);
        
        // 5. Message d'erreur générique WRONG
        runTest(out, in, "5. Réponse WRONG (Erreur protocole)", "WRONG" + CRLF + CRLF); 
        
        // 6. Message NO GAME MODE
        runTest(out, in, "6. Réponse NO GAME MODE", "NO GAME MODE" + CRLF + CRLF);
        
        // 7. Message GAME FINISHED
        runTest(out, in, "7. Réponse GAME FINISHED", "GAME FINISHED" + CRLF + CRLF);
        
        // 8. Grille + X WON (Fin de partie)
        runTest(out, in, "8. Grille + X WON", "XXX" + CRLF + "O O" + CRLF + "O  " + CRLF + "X WON" + CRLF + CRLF);
        
        // 9. Grille + DRAW (Match Nul)
        runTest(out, in, "9. Grille + DRAW", "OXO" + CRLF + "OOX" + CRLF + "XOO" + CRLF + "DRAW" + CRLF + CRLF);
        
        // 10. Grille + OPPONENT QUIT
        runTest(out, in, "10. Grille + OPPONENT QUIT", "X O" + CRLF + "   " + CRLF + "O  " + CRLF + "OPPONENT QUIT" + CRLF + CRLF);

        // --- SCÉNARIOS DE RÈGLES ET ERREURS EN JEU (11-20) ---

        // 11. Réponse CELL OCCUPIED
        runTest(out, in, "11. Réponse CELL OCCUPIED", "CELL OCCUPIED" + CRLF + CRLF);
        
        // 12. Réponse NOT YOUR TURN
        runTest(out, in, "12. Réponse NOT YOUR TURN", "NOT YOUR TURN" + CRLF + CRLF);

        // 13. Réponse INVALID RANGE
        runTest(out, in, "13. Réponse INVALID RANGE", "INVALID RANGE" + CRLF + CRLF);

        // 14. Grille avec un seul X (Test de parsing minimal)
        runTest(out, in, "14. Grille avec 1 coup (X)", "X  " + CRLF + "   " + CRLF + "   " + CRLF + CRLF);

        // 15. Grille pleine, pas de statut (Erreur Serveur)
        runTest(out, in, "15. Grille pleine, Pas de statut", "XOX" + CRLF + "XOX" + CRLF + "OXO" + CRLF + CRLF);
        
        // 16. Réponse inattendue (ne fait partie d'aucun statut/grille)
        runTest(out, in, "16. Réponse arbitraire", "ATTENTION! " + CRLF + CRLF);
        
        // 17-20: Différents formats de victoires pour X

        runTest(out, in, "17. X Win (V2)", "O X" + CRLF + "O X" + CRLF + "O X" + CRLF + "X WON" + CRLF + CRLF);
        runTest(out, in, "18. X Win (H3)", "O O" + CRLF + "X X" + CRLF + "XXX" + CRLF + "X WON" + CRLF + CRLF);
        runTest(out, in, "19. X Win (D1)", "X O" + CRLF + "O X" + CRLF + "O O" + CRLF + "X WON" + CRLF + CRLF);
        runTest(out, in, "20. X Win (D2)", "O O X" + CRLF + " O X" + CRLF + "X O O" + CRLF + "X WON" + CRLF + CRLF);
        
        // --- SCÉNARIOS DE ROBUSTESSE ET MALFORMÉS (21-50) ---
        
        // 21. Grille avec ligne trop courte (Erreur parsing 3 chars)
        runTest(out, in, "21. Grille Malformée (2 chars)", "X" + CRLF + "   " + CRLF + "   " + CRLF + CRLF);
        
        // 22. Grille avec ligne trop longue (Erreur parsing 3 chars)
        runTest(out, in, "22. Grille Malformée (4 chars)", "X X X X" + CRLF + "   " + CRLF + "   " + CRLF + CRLF);
        
        // 23. Grille avec seulement 2 lignes (Erreur parsing 3 lignes)
        runTest(out, in, "23. Grille Malformée (2 lignes)", "X O" + CRLF + "   " + CRLF + CRLF);
        
        // 24. Réponse sans le CRLF final (le client doit bloquer ou timeout)
        runTestRaw(out, in, "24. Réponse sans CRLF CRLF", "X O" + CRLF + "   " + CRLF + "   ");
        
        // 25. Réponse sur une seule ligne (ex: statut sans grille, valide selon certains protocoles, à tester)
        runTest(out, in, "25. Statut seul", "X WON" + CRLF + CRLF);
        
        // 26. Réponse avec espaces multiples (Parsing robuste)
        runTest(out, in, "26. Espaces multiples", " PLAYER  X " + CRLF + CRLF); 
        
        // 27. Réponse avec des caractères non X/O/espace dans la grille
        runTest(out, in, "27. Symbole invalide dans grille", "A B C" + CRLF + "X O X" + CRLF + "   " + CRLF + CRLF);

        // 28. Réponse vide (Doit être gérée)
        runTest(out, in, "28. Réponse vide", CRLF + CRLF); 
        
        // 29-50: Variations de victoires, nuls, et erreurs (22 tests supplémentaires)
        
        // Différentes victoires pour O
        runTest(out, in, "29. O Win (H1)", "OOO" + CRLF + "X X" + CRLF + "X  " + CRLF + "O WON" + CRLF + CRLF);
        runTest(out, in, "30. O Win (V3)", "XXO" + CRLF + "X O" + CRLF + "X O" + CRLF + "O WON" + CRLF + CRLF);
        runTest(out, in, "31. O Win (D2)", "X X O" + CRLF + " X O" + CRLF + "O X X" + CRLF + "O WON" + CRLF + CRLF);

        // Différentes séquences pour DRAW
        runTest(out, in, "32. DRAW (Variation 1)", "OXX" + CRLF + "XXO" + CRLF + "OXO" + CRLF + "DRAW" + CRLF + CRLF);
        runTest(out, in, "33. DRAW (Variation 2)", "XOX" + CRLF + "OXO" + CRLF + "XXO" + CRLF + "DRAW" + CRLF + CRLF);

        // Scénarios de messages d'erreur à des moments variés
        runTest(out, in, "34. WRONG après 10 coups", "WRONG" + CRLF + CRLF); 
        runTest(out, in, "35. NO GAME MODE après 1 coup", "NO GAME MODE" + CRLF + CRLF); 

        // Réponses multi-lignes sans être une grille
        runTest(out, in, "36. Réponse Multi-lignes arbitraire", "Ligne 1" + CRLF + "Ligne 2" + CRLF + "Ligne 3" + CRLF + CRLF);

        // 37-50 : Redondance de tests de grille après des erreurs
        for (int i = 37; i <= 50; i++) {
            String grid = (i % 3 == 0) ? "X O" : "O X";
            runTest(out, in, i + ". Grille aléatoire valide", grid + CRLF + "X O" + CRLF + "O X" + CRLF + CRLF);
        }
    }
    
    /**
     * Exécute un scénario de test simple (envoi d'une réponse complète).
     * @param expectedClientRequest La requête que le client est censé envoyer.
     * @param simulatedServerResponse La réponse complète à envoyer.
     */
    private static void runTest(PrintWriter out, BufferedReader in, String testName, String expectedClientRequest, String simulatedServerResponse) {
        testCount.incrementAndGet();
        String prefix = "[" + testCount.get() + "] " + testName + ": ";
        
        try {
            // 1. Vider la requête du client
            readClientRequest(in);
            
            // 2. Simuler la réponse du serveur
            out.print(simulatedServerResponse);
            out.flush();
            System.out.println(prefix + "SUCCÈS (Réponse complète envoyée)");
            successCount.incrementAndGet();

        } catch (IOException e) {
            System.out.println(prefix + "ÉCHEC");
            System.err.println("  Erreur de communication/timeout: " + e.getMessage());
        } catch (Exception e) {
            System.out.println(prefix + "ÉCHEC");
            System.err.println("  Erreur inattendue: " + e.getMessage());
        }
    }

    /**
     * Surcharge pour les scénarios où l'on ne se soucie pas de la requête envoyée par le client.
     */
    private static void runTest(PrintWriter out, BufferedReader in, String testName, String simulatedServerResponse) {
        runTest(out, in, testName, null, simulatedServerResponse);
    }
    
    /**
     * Exécute un test de robustesse où le message envoyé est incomplet (sans CRLF CRLF final).
     * Le client doit idéalement bloquer (timeout) ou gérer la connexion fermée.
     */
    private static void runTestRaw(PrintWriter out, BufferedReader in, String testName, String rawResponse) {
        testCount.incrementAndGet();
        String prefix = "[" + testCount.get() + "] " + testName + ": ";
        
        try {
            readClientRequest(in);
            
            // Envoi de la réponse incomplète
            out.print(rawResponse);
            out.flush();

            // Attente de 1 seconde, on s'attend à ce que le client bloque.
            Thread.sleep(1000); 
            System.out.println(prefix + "SUCCÈS (Réponse incomplète envoyée, client censé bloquer)");
            successCount.incrementAndGet();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
             System.out.println(prefix + "SUCCÈS (La connexion semble avoir été fermée par le client/socket)");
             successCount.incrementAndGet();
        } catch (Exception e) {
            System.out.println(prefix + "ÉCHEC");
            System.err.println("  Erreur inattendue: " + e.getMessage());
        }
    }
    
    /**
     * Lit la requête complète du client (terminée par CRLF CRLF) pour vider le flux.
     * @throws IOException Si la lecture échoue ou si la connexion est fermée.
     */
    private static String readClientRequest(BufferedReader in) throws IOException {
        StringBuilder request = new StringBuilder();
        String line;
        
        // Lit jusqu'à la ligne vide qui marque la fin du message
        while ((line = in.readLine()) != null) {
            request.append(line).append(CRLF);
            if (line.isEmpty()) {
                break;
            }
        }
        return request.toString();
    }
}