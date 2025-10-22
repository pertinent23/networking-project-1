import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * TestServer.java: Suite de tests de validation pour le TictactoeServer.
 *
 * Cette version 9.0 corrige une erreur critique de conception. Les tests ne
 * suivent plus un script de jeu pré-défini mais analysent l'état réel de la
 * grille après chaque coup du bot pour jouer le coup suivant, garantissant
 * ainsi que le testeur s'adapte au jeu aléatoire du bot.
 *
 * @author Gemini, Expert Java/Socket Assistant
 * @version 9.0 (Logique Réactive Corrigée)
 */
public class TestServer {

    // --- CONFIGURATION ---
    private static final String HOST = "localhost";
    private static final int PORT = 2629;
    private static final int TIMEOUT_MS = 2000;

    // --- OPTIONS DE DÉBOGAGE ---
    private static final boolean DEBUG_MODE = true;

    // --- COULEURS ANSI ---
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_BOLD = "\u001B[1m";

    // --- COMPTEURS ---
    private int testCounter = 1;
    private int testsPassed = 0;
    private int testsFailed = 0;
    private final List<String> failedTestNames = new ArrayList<>();

    public static void main(String[] args) {
        new TestServer().runAllTests();
    }

    public void runAllTests() {
        printHeader("LANCEMENT DE LA SUITE DE TESTS POUR TictactoeServer", "=");
        System.out.println("  Cible -> " + HOST + ":" + PORT + " (Timeout: " + TIMEOUT_MS + "ms, Debug: " + DEBUG_MODE + ")");
        System.out.println("============================================================");

        testProtocolAndConnection();
        testMalformedCommandsAndRobustness();
        testSinglePlayerGameLogic();
        testMultiplayerGameLogic();
        testAdvancedConcurrencyAndState();

        printHeader("RÉSUMÉ DES TESTS", "=");
        System.out.println("  Tests réussis : " + ANSI_GREEN + testsPassed + ANSI_RESET);
        System.out.println("  Tests échoués : " + (testsFailed > 0 ? ANSI_RED : ANSI_GREEN) + testsFailed + ANSI_RESET);
        System.out.println("  Total exécutés: " + (testsPassed + testsFailed));

        if (testsFailed > 0) {
            System.out.println("\n  " + ANSI_RED + "Liste des tests échoués :" + ANSI_RESET);
            for (String failedTest: failedTestNames) {
                System.out.println("    - " + failedTest);
            }
        }
        System.out.println("============================================================");
        if (testsFailed > 0) {
            System.out.println(ANSI_RED + "Certains tests ont échoué. Veuillez vérifier les logs ci-dessus." + ANSI_RESET);
        } else {
            System.out.println(ANSI_GREEN + ANSI_BOLD + "Félicitations ! L'intégralité des tests est passée avec succès." + ANSI_RESET);
        }
    }

    // =========================================================================================
    // SUITES DE TESTS
    // =========================================================================================

    private void testProtocolAndConnection() {
        printHeader("Suite 1: Connexion et Protocole de Base", "-");
        testSimpleConnection();
        testQuitCommand();
        testCommandWithoutGameStart("PUT 0 0");
        testCommandWithoutGameStart("UPDATE");
        testCaseInsensitivityAndWhitespace();
        testInternalWhitespaceTolerance();
    }

    private void testMalformedCommandsAndRobustness() {
        printHeader("Suite 2: Commandes Malformées et Robustesse", "-");
        testWrongCommand();
        testEmptyRequest();
        testGarbageRequest();
        testVeryLongRequest();
        testIncompleteCommand();
        testCommandWithExtraData();
        testStartBotWithInvalidSymbol();
        testPutWithWrongArgCount();
        testPutWithNonNumericArgs();
    }

    private void testSinglePlayerGameLogic() {
        printHeader("Suite 3: Logique de Jeu - Mode Un Joueur (Bot)", "-");
        testStartBotAsX();
        testStartBotAsO();
        testGameAgainstBot_ClientPlaysOptimal('X');
        testGameAgainstBot_ClientPlaysOptimal('O');
        testInvalidMovesInGame();
        testMovesAfterGameEnd();
    }

    private void testMultiplayerGameLogic() {
        printHeader("Suite 4: Logique de Jeu - Mode Multijoueur", "-");
        testMultiplayerConnectionAndRoles();
        testMultiplayerTurnLogic();
        testMultiplayerFullGame_X_Wins();
        testMultiplayerFullGame_Draw();
        testMultiplayerOpponentQuit();
    }

    private void testAdvancedConcurrencyAndState() {
        printHeader("Suite 5: Concurrence Avancée et Gestion d'État", "-");
        testMultiplayerThirdPlayerConnection();
        testRestartGameOnSameSocket();
        testPipelinedCommands();
    }

    // =========================================================================================
    // CAS DE TESTS INDIVIDUELS
    // =========================================================================================

    private void testSimpleConnection() {
        try (Socket s = connect()) {
            pass("Connexion simple au serveur");
        } catch (IOException e) {
            fail("Connexion simple au serveur", "Une connexion réussie.", "Échec : " + e.getMessage(), "Le serveur doit écouter et accepter les connexions TCP.");
        }
    }

    private void testQuitCommand() {
        try (Socket s = connect()) {
            sendMessage(s, "QUIT\r\n\r\n");
            String response = readResponse(s);
            assertEquals(null, response, "Commande QUIT", "Le serveur ne doit rien répondre à QUIT et fermer la connexion (le client lit null).");
        } catch (IOException e) {
            fail("Commande QUIT", "Connexion fermée par le serveur.", "Exception: " + e.getMessage(), "Le serveur ne doit pas répondre.");
        }
    }

    private void testCommandWithoutGameStart(String command) {
        testCommand(command, "NO GAME MODE\r\n\r\n", "Commande '" + command + "' avant de démarrer une partie", "Les commandes de jeu ne sont valides qu'après un START.");
    }

    private void testCaseInsensitivityAndWhitespace() {
        testCommand("  sTaRt   bOt    x  ", "   \r\n   \r\n   \r\n\r\n", "Gestion des espaces superflus et de la casse", "Un serveur robuste (attente du Feedback.pdf) doit nettoyer les entrées mais répondre de manière 100% conforme.");
    }

    private void testInternalWhitespaceTolerance() {
        try (Socket s = connect()) {
            sendMessage(s, "START  BOT  X\r\n\r\n");
            assertEquals("   \r\n   \r\n   \r\n\r\n", readResponse(s), "Tolérance aux espaces internes multiples", "Le serveur doit traiter 'START  BOT' comme 'START BOT'.");
        } catch (IOException e) {
            fail("Tolérance aux espaces internes multiples", "Réponse de grille vide", "Exception: " + e.getMessage(), "Le parsing de la commande doit être robuste.");
        }
    }

    private void testWrongCommand() {
        testCommand("COMMANDE INVALIDE", "WRONG\r\n\r\n", "Commande inconnue", "Le protocole spécifie que toute commande non reconnue doit renvoyer 'WRONG'.");
    }

    private void testEmptyRequest() {
        testCommand("", "WRONG\r\n\r\n", "Requête vide (uniquement le terminateur)", "Une chaîne de commande vide n'est pas une commande valide.");
    }

    private void testGarbageRequest() {
        testCommand("!@#$%^&*()", "WRONG\r\n\r\n", "Requête avec des caractères invalides", "Une chaîne aléatoire n'est pas une commande valide.");
    }

    private void testVeryLongRequest() {
        String longString = "A".repeat(2048);
        testCommand(longString, "WRONG\r\n\r\n", "Requête très longue (test de buffer)", "Le serveur ne doit pas planter et doit rejeter proprement.");
    }

    private void testIncompleteCommand() {
        String testName = "Commande incomplète (doit provoquer un timeout)";
        try (Socket s = connect()) {
            sendMessage(s, "PUT 0 0\r\n");
            readResponse(s);
            fail(testName, "Aucune réponse (timeout).", "Le serveur a répondu à une commande incomplète.", "Le serveur ne doit pas traiter une commande non terminée par '\\r\\n\\r\\n'.");
        } catch (SocketTimeoutException e) {
            pass(testName + " : Le client a timeout comme attendu.");
        } catch (IOException e) {
            fail(testName, "Aucune réponse (timeout).", "Exception: " + e.getMessage(), "Une IOException inattendue s'est produite.");
        }
    }

    private void testCommandWithExtraData() {
        try (Socket s = connect()) {
            sendMessage(s, "START BOT X\r\n\r\nGARBAGE DATA");
            assertEquals("   \r\n   \r\n   \r\n\r\n", readResponse(s), "Commande avec données supplémentaires", "Le serveur doit traiter la première commande valide et ignorer les données suivantes.");
        } catch (IOException e) {
            fail("Commande avec données supplémentaires", "Réponse de grille vide.", "Exception: " + e.getMessage(), "La lecture doit s'arrêter après '\\r\\n\\r\\n'.");
        }
    }

    private void testStartBotWithInvalidSymbol() {
        testCommand("START BOT Z", "WRONG\r\n\r\n", "START BOT avec un symbole invalide", "Le protocole n'autorise que X ou O.");
    }

    private void testPutWithWrongArgCount() {
        String testName = "Commande PUT avec un nombre d'arguments incorrect";
        try (Socket s = connect()) {
            sendMessage(s, "START BOT X\r\n\r\n");
            readResponse(s);
            sendMessage(s, "PUT 1\r\n\r\n");
            assertEquals("WRONG\r\n\r\n", readResponse(s), testName + " (un seul argument)", "PUT requiert deux coordonnées.");
            sendMessage(s, "PUT 1 1 1\r\n\r\n");
            assertEquals("WRONG\r\n\r\n", readResponse(s), testName + " (trois arguments)", "PUT requiert exactement deux coordonnées.");
        } catch (IOException e) {
            fail(testName, "Réponse 'WRONG'.", "Exception: " + e.getMessage(), "");
        }
    }

    private void testPutWithNonNumericArgs() {
        try (var s = connect()) {
            sendMessage(s, "START BOT X\r\n\r\n");
            readResponse(s);
            sendMessage(s, "PUT a b\r\n\r\n");
            assertEquals("WRONG\r\n\r\n", readResponse(s), "Commande PUT avec des arguments non numériques", "Les coordonnées pour PUT doivent être des entiers.");
        } catch (IOException e) {
            fail("Commande PUT avec des arguments non numériques", "WRONG", "Exception: " + e.getMessage(), "");
        }
    }

    private void testStartBotAsX() {
        testCommand("START BOT X", "   \r\n   \r\n   \r\n\r\n", "Démarrer une partie contre le bot en tant que 'X'", "Le serveur doit renvoyer une grille vide car c'est au joueur X de commencer.");
    }

    private void testStartBotAsO() {
        try (Socket s = connect()) {
            sendMessage(s, "START BOT O\r\n\r\n");
            String response = readResponse(s);
            if (response == null || !isGridResponse(response)) {
                fail("Démarrer une partie contre le bot en tant que 'O'", "Une grille de jeu.", response, "Le serveur doit répondre avec une grille valide.");
                return;
            }
            long xCount = response.chars().filter(ch -> ch == 'X').count();
            assertEquals(1L, xCount, "Démarrer une partie contre le bot en tant que 'O'", "Le bot ('X') doit jouer immédiatement. La grille doit contenir un 'X'.");
        } catch (IOException e) {
            fail("Démarrer une partie contre le bot en tant que 'O'", "Une grille avec un 'X'.", "Exception: " + e.getMessage(), "");
        }
    }

    private void testGameAgainstBot_ClientPlaysOptimal(char playerSymbol) {
        String testName = "Jeu contre le bot en tant que '" + playerSymbol + "' (le client joue de manière optimale)";
        String rationale = "Le test valide que le serveur arbitre correctement la partie jusqu'à une fin valide (victoire, défaite ou nul), quelle que soit l'issue.";
        char botSymbol = (playerSymbol == 'X') ? 'O' : 'X';
        char[][] board = {
            {
                ' ',
                ' ',
                ' '
            }, {
                ' ',
                ' ',
                ' '
            }, {
                ' ',
                ' ',
                ' '
            }
        };

        try (Socket s = connect()) {
            sendMessage(s, "START BOT " + playerSymbol + "\r\n\r\n");
            String response = readResponse(s);

            if (playerSymbol == 'O') {
                if (!isGridResponse(response)) {
                    fail(testName, "Une grille de jeu valide", response, "Le serveur doit renvoyer une grille avec le premier coup du bot.");
                    return;
                }
                board = parseGrid(response);
            }

            for (int i = 0; i < 5; i++) {
                Minimax.Move bestMove = Minimax.findBestMove(board, playerSymbol);
                if (bestMove.row == -1) {
                    break;
                }

                sendMessage(s, "PUT " + bestMove.row + " " + bestMove.col + "\r\n\r\n");
                response = readResponse(s);

                if (response == null || response.contains("WRONG") || response.contains("ERROR")) {
                    fail(testName, "Une réponse de jeu valide (grille ou fin de partie).", response, "Le serveur a renvoyé une erreur inattendue.");
                    return;
                }

                if (response.contains("WON") || response.contains("DRAW")) {
                    char[][] finalBoard = parseGrid(response);
                    String expectedStatus = Minimax.getBoardStatus(finalBoard, playerSymbol, botSymbol);
                    if (response.contains(expectedStatus)) {
                        pass(testName + " - Le serveur a correctement annoncé la fin: " + expectedStatus);
                    } else {
                        fail(testName, "Un statut de fin de partie cohérent (" + expectedStatus + ").", extractFinalStatus(response), "Le statut annoncé par le serveur ne correspond pas à l'état final de la grille.");
                    }
                    return;
                }

                if (isGridResponse(response)) {
                    board = parseGrid(response);
                } else {
                    fail(testName, "Une grille de jeu valide.", response, "Le serveur doit renvoyer une grille tant que la partie n'est pas finie.");
                    return;
                }
            }
            fail(testName, "Une fin de partie valide.", "La partie n'a pas été terminée par le serveur après 9 coups.", rationale);
        } catch (Exception e) {
            fail(testName, "Une fin de partie valide.", "Exception: " + e.getMessage(), rationale);
        }
    }

    private void testInvalidMovesInGame() {
        String testName = "Tentative de coups invalides en cours de partie";
        try (Socket s = connect()) {
            sendMessage(s, "START BOT X\r\n\r\n");
            readResponse(s);
            sendMessage(s, "PUT 1 1\r\n\r\n");
            readResponse(s);
            sendMessage(s, "PUT 1 1\r\n\r\n");
            assertEquals("CELL OCCUPIED\r\n\r\n", readResponse(s), testName + " (case occupée)", "Le protocole exige 'CELL OCCUPIED'.");
            sendMessage(s, "PUT 3 3\r\n\r\n");
            assertEquals("INVALID RANGE\r\n\r\n", readResponse(s), testName + " (hors limites > 2)", "Les coordonnées doivent être entre 0 et 2.");
            sendMessage(s, "PUT -1 0\r\n\r\n");
            assertEquals("INVALID RANGE\r\n\r\n", readResponse(s), testName + " (hors limites < 0)", "Les coordonnées ne peuvent pas être négatives.");
        } catch (IOException e) {
            fail(testName, "Réponses d'erreur spécifiques.", "Exception: " + e.getMessage(), "");
        }
    }

    private void testMovesAfterGameEnd() {
        String testName = "Tentative de coup après la fin de la partie";
        String rationale = "Le test joue de manière optimale jusqu'à la fin de la partie, puis vérifie que le serveur refuse tout coup supplémentaire.";
        char playerSymbol = 'X'; // On joue en tant que X pour avoir la main
        char[][] board = {
            {
                ' ',
                ' ',
                ' '
            }, {
                ' ',
                ' ',
                ' '
            }, {
                ' ',
                ' ',
                ' '
            }
        };

        try (Socket s = connect()) {
            sendMessage(s, "START BOT " + playerSymbol + "\r\n\r\n");
            String response = readResponse(s);
            assertTrue(isGridResponse(response), testName + " (Démarrage)", "Le jeu doit commencer.");

            // Boucle de jeu jusqu'à la fin
            for (int i = 0; i < 5; i++) {
                Minimax.Move bestMove = Minimax.findBestMove(board, playerSymbol);
                if (bestMove.row == -1) break;

                sendMessage(s, "PUT " + bestMove.row + " " + bestMove.col + "\r\n\r\n");
                response = readResponse(s);

                if (response == null || (!isGridResponse(response) && !response.contains("WON") && !response.contains("DRAW"))) {
                    fail(testName, "Une réponse de jeu valide.", response, "Le serveur a renvoyé une réponse inattendue avant la fin de la partie.");
                    return;
                }

                if (response.contains("WON") || response.contains("DRAW")) {
                    break; // La partie est finie, on sort de la boucle de jeu
                }
                board = parseGrid(response);
            }

            assertTrue(response != null && (response.contains("WON") || response.contains("DRAW")), testName + " (Pré-requis: Fin de partie)", "Le scénario doit aboutir à une fin de partie.");

            // Le test principal : tenter un coup après la fin.
            sendMessage(s, "PUT 2 2\r\n\r\n");
            assertEquals("GAME FINISHED\r\n\r\n", readResponse(s), testName, "Le protocole spécifie 'GAME FINISHED' pour un coup post-victoire.");

        } catch (IOException e) {
            fail(testName, "'GAME FINISHED'.", "Exception: " + e.getMessage(), rationale);
        }
    }

    private void testMultiplayerConnectionAndRoles() {
        try (Socket s1 = connect(); Socket s2 = connect()) {
            sendMessage(s1, "START PLAYER\r\n\r\n");
            assertEquals("PLAYER X\r\n\r\n", readResponse(s1), "Connexion multijoueur (Joueur 1)", "Le premier joueur doit être 'X'.");
            sendMessage(s2, "START PLAYER\r\n\r\n");
            assertEquals("PLAYER O\r\n\r\n", readResponse(s2), "Connexion multijoueur (Joueur 2)", "Le second joueur doit être 'O'.");
        } catch (IOException e) {
            fail("Connexion multijoueur", "Assignation correcte des rôles X et O.", "Exception: " + e.getMessage(), "Le serveur doit gérer deux connexions simultanées.");
        }
    }

    private void testMultiplayerTurnLogic() {
        String testName = "Logique de tour en multijoueur";
        CountDownLatch latch = new CountDownLatch(1);
        Thread playerX = new Thread(() -> {
            try (Socket s = connect()) {
                sendMessage(s, "START PLAYER\r\n\r\n");
                readResponse(s);
                latch.countDown();
                sendMessage(s, "PUT 0 0\r\n\r\n");
                assertTrue(isGridResponse(readResponse(s)), testName + " (X joue)", "Le premier coup de X doit être accepté.");
                sendMessage(s, "PUT 1 1\r\n\r\n");
                assertEquals("NOT YOUR TURN\r\n\r\n", readResponse(s), testName + " (X joue deux fois)", "Un joueur ne peut pas jouer deux fois de suite.");
            } catch (IOException e) {
                fail(testName, "Respect des tours.", "Exception (X): " + e.getMessage(), "");
            }
        });
        Thread playerO = new Thread(() -> {
            try {
                latch.await();
                try (Socket s = connect()) {
                    sendMessage(s, "START PLAYER\r\n\r\n");
                    readResponse(s);
                    sendMessage(s, "PUT 1 1\r\n\r\n");
                    assertEquals("NOT YOUR TURN\r\n\r\n", readResponse(s), testName + " (O joue en premier)", "Le joueur O ne peut pas commencer.");
                }
            } catch (InterruptedException | IOException e) {
                fail(testName, "Respect des tours.", "Exception (O): " + e.getMessage(), "");
            }
        });
        playerX.start();
        playerO.start();
        try {
            playerX.join();
            playerO.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void testMultiplayerFullGame_X_Wins() {
        String testName = "Partie multijoueur complète (Victoire de X)";
        try (Socket sX = connect(); Socket sO = connect()) {
            sendMessage(sX, "START PLAYER\r\n\r\n");
            readResponse(sX);
            sendMessage(sO, "START PLAYER\r\n\r\n");
            readResponse(sO);
            sendMessage(sX, "PUT 0 0\r\n\r\n");
            readResponse(sX);
            sendMessage(sO, "PUT 1 0\r\n\r\n");
            readResponse(sO);
            sendMessage(sX, "PUT 0 1\r\n\r\n");
            readResponse(sX);
            sendMessage(sO, "PUT 1 1\r\n\r\n");
            readResponse(sO);
            sendMessage(sX, "PUT 0 2\r\n\r\n");
            String finalResponseX = readResponse(sX);
            assertTrue(finalResponseX != null && finalResponseX.contains("X WON"), testName + " (vue de X)", "Le joueur X doit être notifié de sa victoire.");
            sendMessage(sO, "UPDATE\r\n\r\n");
            String finalResponseO = readResponse(sO);
            assertTrue(finalResponseO != null && finalResponseO.contains("X WON"), testName + " (vue de O)", "Le joueur O doit être notifié de la victoire de X.");
        } catch (IOException e) {
            fail(testName, "Notification de victoire pour les deux joueurs.", "Exception: " + e.getMessage(), "");
        }
    }

    private void testMultiplayerFullGame_Draw() {
        String testName = "Partie multijoueur complète (Match Nul)";
        try (Socket sX = connect(); Socket sO = connect()) {
            sendMessage(sX, "START PLAYER\r\n\r\n");
            readResponse(sX);
            sendMessage(sO, "START PLAYER\r\n\r\n");
            readResponse(sO);
            sendMessage(sX, "PUT 0 0\r\n\r\n");
            readResponse(sX);
            sendMessage(sO, "PUT 1 1\r\n\r\n");
            readResponse(sO);
            sendMessage(sX, "PUT 0 1\r\n\r\n");
            readResponse(sX);
            sendMessage(sO, "PUT 0 2\r\n\r\n");
            readResponse(sO);
            sendMessage(sX, "PUT 2 0\r\n\r\n");
            readResponse(sX);
            sendMessage(sO, "PUT 1 0\r\n\r\n");
            readResponse(sO);
            sendMessage(sX, "PUT 1 2\r\n\r\n");
            readResponse(sX);
            sendMessage(sO, "PUT 2 1\r\n\r\n");
            readResponse(sO);
            sendMessage(sX, "PUT 2 2\r\n\r\n");
            String finalResponse = readResponse(sX);
            assertTrue(finalResponse != null && finalResponse.contains("DRAW"), testName, "La partie doit se terminer par 'DRAW'.");
        } catch (IOException e) {
            fail(testName, "Notification de match nul.", "Exception: " + e.getMessage(), "");
        }
    }

    private void testMultiplayerOpponentQuit() {
        String testName = "Déconnexion de l'adversaire en multijoueur";
        String rationale = "Un abandon est une victoire par forfait. Pour respecter 'UPDATE -> grid + GameStatus', le serveur doit envoyer la grille et un statut de victoire.";
        try (Socket sX = connect(); Socket sO = connect()) {
            sendMessage(sX, "START PLAYER\r\n\r\n");
            assertEquals("PLAYER X\r\n\r\n", readResponse(sX), testName + " (Setup P1)", "Le joueur 1 doit être X.");
            sendMessage(sO, "START PLAYER\r\n\r\n");
            assertEquals("PLAYER O\r\n\r\n", readResponse(sO), testName + " (Setup P2)", "Le joueur 2 doit être O.");

            sendMessage(sO, "QUIT\r\n\r\n");

            sendMessage(sX, "UPDATE\r\n\r\n");
            String response = readResponse(sX);

            assertNotNull(response, testName, rationale);
            assertTrue(isGridResponse(response), testName, "La réponse doit contenir une grille. " + rationale);
            assertTrue(response.contains("X WON"), testName, "Le statut doit être 'X WON'. " + rationale);

        } catch (IOException e) {
            fail(testName, "grid + X WON", "Exception: " + e.getMessage(), rationale);
        }
    }

    private void testMultiplayerThirdPlayerConnection() {
        try (Socket s1 = connect(); Socket s2 = connect()) {
            sendMessage(s1, "START PLAYER\r\n\r\n");
            readResponse(s1);
            sendMessage(s2, "START PLAYER\r\n\r\n");
            readResponse(s2);
            try (Socket s3 = connect()) {
                sendMessage(s3, "START PLAYER\r\n\r\n");
                String response = readResponse(s3);
                assertNotNull(response, "Connexion d'un troisième joueur", "Le serveur doit répondre au 3ème joueur (p. ex. le mettre en attente) et ne pas planter.");
            }
        } catch (IOException e) {
            fail("Connexion d'un troisième joueur", "Gestion propre du 3ème joueur.", "Exception: " + e.getMessage(), "Le serveur ne doit pas lever d'exception non gérée.");
        }
    }

    private void testRestartGameOnSameSocket() {
        String testName = "Recommencer une partie sur la même connexion";
        try (Socket s = connect()) {
            sendMessage(s, "START BOT X\r\n\r\n");
            readResponse(s);
            sendMessage(s, "PUT 0 0\r\n\r\n");
            readResponse(s);
            sendMessage(s, "PUT 1 0\r\n\r\n");
            readResponse(s);
            sendMessage(s, "PUT 0 1\r\n\r\n");
            readResponse(s);
            sendMessage(s, "PUT 2 2\r\n\r\n");
            readResponse(s);
            sendMessage(s, "PUT 0 2\r\n\r\n");
            readResponse(s); // Fin de la partie 1

            sendMessage(s, "START BOT O\r\n\r\n");
            String response = readResponse(s);
            assertTrue(isGridResponse(response), testName, "Le serveur doit permettre de lancer une nouvelle partie après la fin de la précédente.");

        } catch (IOException e) {
            fail(testName, "Démarrage d'une nouvelle partie.", "Exception: " + e.getMessage(), "");
        }
    }

    private void testPipelinedCommands() {
        String testName = "Envoi de commandes en rafale (pipelining)";
        String command = "START BOT X\r\n\r\nPUT 0 0\r\n\r\nPUT a b\r\n\r\n";
        String[] expectedResponses = {
            "   \r\n   \r\n   \r\n\r\n", // Réponse à START BOT X
            null, // Réponse à PUT 0 0 (on vérifie juste que c'est une grille valide)
            "WRONG\r\n\r\n" // Réponse à PUT a b
        };

        try (Socket s = connect()) {
            sendMessage(s, command);

            for (int i = 0; i < expectedResponses.length; i++) {
                String actualResponse = readResponse(s);
                String expected = expectedResponses[i];
                String stepName = testName + String.format(" (Réponse n°%d sur %d)", i + 1, expectedResponses.length);

                if (expected == null) {
                    if (!isGridResponse(actualResponse)) {
                        fail(stepName, "Une grille de jeu valide", actualResponse, "Le serveur doit renvoyer une grille après un coup valide.");
                        return;
                    }
                } else if (!expected.equals(actualResponse)) {
                    fail(stepName, expected, actualResponse, "La réponse du serveur pour cette étape est incorrecte.");
                    return;
                }
            }
            pass(testName);
        } catch (IOException e) {
            fail(testName, "Séquence de réponses valides.", "Exception: " + e.getMessage(), "Le serveur doit traiter les commandes en file d'attente.");
        }
    }

    // =========================================================================================
    // MOTEURS ET UTILITAIRES
    // =========================================================================================

    private void testCommand(String command, String expectedResponse, String testName, String rationale) {
        try (Socket s = connect()) {
            sendMessage(s, command + "\r\n\r\n");
            assertEquals(expectedResponse, readResponse(s), testName, rationale);
        } catch (IOException e) {
            fail(testName, expectedResponse, "Exception: " + e.getMessage(), rationale);
        }
    }

    private Socket connect() throws IOException {
        Socket socket = new Socket(HOST, PORT);
        socket.setSoTimeout(TIMEOUT_MS);
        return socket;
    }

    private void sendMessage(Socket socket, String message) throws IOException {
        if (DEBUG_MODE) {
            System.out.println(ANSI_BLUE + "➡️ [DEBUG] Envoi:\n" + ANSI_RESET + formatOutput(message));
        }
        socket.getOutputStream().write(message.getBytes("UTF-8"));
        socket.getOutputStream().flush();
    }

    private String readResponse(Socket socket) throws IOException {
        InputStream in = socket.getInputStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < TIMEOUT_MS) {
            if (in.available() > 0) {
                int bytesRead = in.read(buffer);
                if (bytesRead == -1) {
                    break;
                }
                baos.write(buffer, 0, bytesRead);
                String currentData = baos.toString("UTF-8");
                if (currentData.endsWith("\r\n\r\n")) {
                    if (DEBUG_MODE) {
                        System.out.println(ANSI_PURPLE + "⬅️ [DEBUG] Reçu:\n" + ANSI_RESET + formatOutput(currentData));
                    }
                    return currentData;
                }
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        String finalData = baos.toString("UTF-8");
        if (finalData.isEmpty()) {
            return null; // Timeout propre, aucune donnée reçue
        }

        if (DEBUG_MODE) {
            System.out.println(ANSI_PURPLE + "⬅️ [DEBUG] Reçu (partiel/timeout):\n" + ANSI_RESET + formatOutput(finalData));
        }
        return finalData;
    }

    private char[][] parseGrid(String response) {
        char[][] board = {
            {
                ' ',
                ' ',
                ' '
            }, {
                ' ',
                ' ',
                ' '
            }, {
                ' ',
                ' ',
                ' '
            }
        };
        String[] lines = response.split("\r\n");
        for (int i = 0; i < 3 && i < lines.length; i++) {
            for (int j = 0; j < 3 && j < lines[i].length(); j++) {
                board[i][j] = lines[i].charAt(j);
            }
        }
        return board;
    }

    private String formatOutput(String text) {
        if (text == null) {
            return ANSI_RED + "null" + ANSI_RESET;
        }
        String sanitized = text.replace("\r\n", ANSI_YELLOW + "[CRLF]\n" + ANSI_RESET + "      ");
        if (isGridResponse(text)) {
            return "\n" + formatGrid(text) + "\n      (Raw: " + sanitized.trim() + ")";
        }
        return sanitized;
    }

    private String formatGrid(String gridResponse) {
        StringBuilder sb = new StringBuilder();
        String[] lines = gridResponse.split("\r\n");
        sb.append("      +---+---+---+\n");
        for (int i = 0; i < 3 && i < lines.length; i++) {
            sb.append("      | ");
            for (int j = 0; j < 3 && j < lines[i].length(); j++) {
                char c = lines[i].charAt(j);
                sb.append(c == 'X' ? ANSI_BLUE + "X" : (c == 'O' ? ANSI_YELLOW + "O" : " ")).append(ANSI_RESET).append(" | ");
            }
            sb.replace(sb.length() - 2, sb.length(), "|").append("\n      +---+---+---+\n");
        }
        return sb.toString();
    }

    private boolean isGridResponse(String response) {
        return response != null && response.split("\r\n").length >= 3 && response.length() >= 11;
    }

    private String extractFinalStatus(String response) {
        if (response == null) {
            return "";
        }
        String[] lines = response.split("\r\n");
        for (String line: lines) {
            if (line.contains("WON") || line.contains("DRAW")) {
                return line.trim();
            }
        }
        return response.trim();
    }

    private void printHeader(String title, String borderChar) {
        System.out.println("\n" + ANSI_BOLD + title + ANSI_RESET);
        System.out.println(borderChar.repeat(title.length()));
    }

    private void pass(String testName) {
        System.out.println(String.format("%s[Test #%02d]%s %s[JUSTE]%s %s", ANSI_BOLD, testCounter++, ANSI_RESET, ANSI_GREEN, ANSI_RESET, testName));
        testsPassed++;
    }

    private void fail(String testName, Object expected, Object actual, String rationale) {
        System.out.println(String.format("%s[Test #%02d]%s %s[FAUX]%s  %s", ANSI_BOLD, testCounter++, ANSI_RESET, ANSI_RED, ANSI_RESET, testName));
        if (!rationale.isEmpty()) {
            System.out.println("  ├─ " + ANSI_YELLOW + "Raison du test :" + ANSI_RESET + " " + rationale);
        }
        System.out.println("  ├─ " + ANSI_YELLOW + "Attendu        :" + ANSI_RESET + formatOutput(String.valueOf(expected)));
        System.out.println("  └─ " + ANSI_YELLOW + "Obtenu         :" + ANSI_RESET + formatOutput(String.valueOf(actual)));
        testsFailed++;
        failedTestNames.add(testName);
    }

    private void assertEquals(Object expected, Object actual, String testName, String rationale) {
        if ((expected == null && actual == null) || (expected != null && expected.equals(actual))) {
            pass(testName);
        } else {
            fail(testName, expected, actual, rationale);
        }
    }

    private void assertTrue(boolean condition, String testName, String rationale) {
        if (condition) {
            pass(testName);
        } else {
            fail(testName, "true", "false", rationale);
        }
    }

    private void assertNotNull(Object obj, String testName, String rationale) {
        if (obj != null) {
            pass(testName);
        } else {
            fail(testName, "non-null", "null", rationale);
        }
    }

    static class Minimax {
        static class Move {
            int row, col;
        }

        static String getBoardStatus(char[][] b, char player, char opponent) {
            if (evaluate(b, player, opponent) == 10) return player + " WON";
            if (evaluate(b, player, opponent) == -10) return opponent + " WON";
            if (!isMovesLeft(b)) return "DRAW";
            return "IN PROGRESS";
        }

        static int evaluate(char[][] b, char player, char opponent) {
            for (int row = 0; row < 3; row++) {
                if (b[row][0] == b[row][1] && b[row][1] == b[row][2]) {
                    if (b[row][0] == player) return 10;
                    else if (b[row][0] == opponent) return -10;
                }
            }
            for (int col = 0; col < 3; col++) {
                if (b[0][col] == b[1][col] && b[1][col] == b[2][col]) {
                    if (b[0][col] == player) return 10;
                    else if (b[0][col] == opponent) return -10;
                }
            }
            if (b[0][0] == b[1][1] && b[1][1] == b[2][2]) {
                if (b[0][0] == player) return 10;
                else if (b[0][0] == opponent) return -10;
            }
            if (b[0][2] == b[1][1] && b[1][1] == b[2][0]) {
                if (b[0][2] == player) return 10;
                else if (b[0][2] == opponent) return -10;
            }
            return 0;
        }

        static boolean isMovesLeft(char[][] board) {
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    if (board[i][j] == ' ') {
                        return true;
                    }
                }
            }
            return false;
        }

        static int minimax(char[][] board, int depth, boolean isMax, char player, char opponent) {
            int score = evaluate(board, player, opponent);
            if (score == 10) return score - depth;
            if (score == -10) return score + depth;
            if (!isMovesLeft(board)) return 0;

            if (isMax) {
                int best = -1000;
                for (int i = 0; i < 3; i++) {
                    for (int j = 0; j < 3; j++) {
                        if (board[i][j] == ' ') {
                            board[i][j] = player;
                            best = Math.max(best, minimax(board, depth + 1, false, player, opponent));
                            board[i][j] = ' ';
                        }
                    }
                }
                return best;
            } else {
                int best = 1000;
                for (int i = 0; i < 3; i++) {
                    for (int j = 0; j < 3; j++) {
                        if (board[i][j] == ' ') {
                            board[i][j] = opponent;
                            best = Math.min(best, minimax(board, depth + 1, true, player, opponent));
                            board[i][j] = ' ';
                        }
                    }
                }
                return best;
            }
        }

        static Move findBestMove(char[][] board, char player) {
            char opponent = (player == 'X') ? 'O' : 'X';
            int bestVal = -1000;
            Move bestMove = new Move();
            bestMove.row = -1;
            bestMove.col = -1;
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    if (board[i][j] == ' ') {
                        board[i][j] = player;
                        int moveVal = minimax(board, 0, false, player, opponent);
                        board[i][j] = ' ';
                        if (moveVal > bestVal) {
                            bestMove.row = i;
                            bestMove.col = j;
                            bestVal = moveVal;
                        }
                    }
                }
            }
            return bestMove;
        }
    }
}