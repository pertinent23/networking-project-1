
/**
 * This class represnets  the game logic on the server side.
 * It handles the game state, player turns, and win conditions.
*/

public class ServerGame {
    /*
     * playerX is always the first player to play
     */
    private ServerClient playerX;

    /*
     * playerO is always the second player to play
     */
    private ServerClient playerO;

    /*
     * Represnts the grid of the game
     * null means the cell is empty
     * ClientType.X means the cell is occupied by player X
     * ClientType.O means the cell is occupied by player O 
    */
    private ClientType grid[][] = new ClientType[3][3];

    /*
     * currentPlayer indicates whose turn it is
     * It starts with ClientType.X (the first player)
    */
    private ClientType currentPlayer = ClientType.X;

    /**
     * indicates if the game is against a bot
     * true means one player is a bot
     * false means both players are human
    */
    private boolean isBotGame = false;

    /**
     * indicates if the game has ended in a draw
     * true means the game is a draw
     * false means the game is not a draw
     */
    private boolean isDraw = false;

    /**
     * indicates if the game has been won
     * true means one player has won
     * false means the game is still ongoing or is a draw
    */
    private boolean isWin = false;


    /*
     * indicates if the game has been aborded
     * true means the game has been aborded
     * false means the game is ongoing or has ended normally
    */
    private boolean isAborded = false;

    /**
     * indicates the winner of the game
     * null means there is no winner yet or the game is a draw
     * ClientType.X means player X has won
     * ClientType.O means player O has won
    */
    private ClientType winner = null;

    /**
     * return true if the client is a player of this game
     * @param client
     * @return
     */
    public boolean hasPlayer(ServerClient client) {
        return client.equals(playerX) || client.equals(playerO);
    }

    /*
     * Change the current player to the other player
     * If current player is X, it becomes O
     * If current player is O, it becomes X
    */
    private void toogleCurrentPlayer() {
        if (this.currentPlayer.equals(ClientType.X)) {
            this.currentPlayer = ClientType.O;
        } else {
            this.currentPlayer = ClientType.X;
        }
    }

    /**
     * Process a player's move 
     * 
     * @param client The player making the move
     * @param row The row index (0-2)
     * @param col The column index (0-2)
     * @return A string indicating the result of the move ("OK", "NOT YOUR TURN", "INVALID RANGE", "CELL OCCUPIED")
    */
    public synchronized String play(ServerClient client, int row, int col) {
        // check if it's the client's turn
        if (isFinished()) {
            return "GAME FINISHED".concat(TictactoeConst.END_OF_MESSAGE);
        }

        if (!isBotGame && getPlayerCount() < 2) {
            return "NO GAME MODE".concat(TictactoeConst.END_OF_MESSAGE);
        }

        if (!this.currentPlayer.equals(client.getType())) {
            return "NOT YOUR TURN".concat(TictactoeConst.END_OF_MESSAGE);
        }

        // check if the coordinates are valid
        if (row < 0 || col > 2 || col < 0 || row > 2) {
            return "INVALID RANGE".concat(TictactoeConst.END_OF_MESSAGE);
        }

        // check if the cell is already occupied
        if (this.grid[row][col] != null) {
            return "CELL OCCUPIED".concat(TictactoeConst.END_OF_MESSAGE);
        }

        // place the player's mark on the grid
        grid[row][col] = this.currentPlayer;

        // switch to the other player
        toogleCurrentPlayer();

        // update the game state
        updateGameState();

        // if the state is finished (so the state has changed) return the grid state
        if (isFinished()) {
            return getGridState();
        } 

        if (isBotGame && !isFinished()) {
            botPlay();
            updateGameState();
        }

        return getGridState();
    }

    /**
     * count the number of players in the game
     * @return the number of players (0, 1, or 2)
     * 
    */
    public int getPlayerCount() {
        int count = 0;
        if (this.playerX != null) count++;
        if (this.playerO != null) count++;
        return count;
    }
    
    /*
     * Set player X 
     * @param playerX
     * @throws ServerGameException if player X is already set
    */
    public void setPlayerX(ServerClient playerX) throws ServerGameException {
        if (this.playerX != null) {
            throw new ServerGameException("Player X is already set");
        }

        this.playerX = playerX;
    }

    /*
     * Set player O
     * @param playerO
     * @throws ServerGameException if player O is already set or if player X is not set
    */
    public void setPlayerO(ServerClient playerO) throws ServerGameException {
        if (this.playerX == null && !isBotGame) {
            throw new ServerGameException("Player X must be set before setting Player O");
        } else if (this.playerO != null) {
            throw new ServerGameException("Player O is already set");
        } else if (this.playerX != null && this.playerX.equals(playerO)) {
            throw new ServerGameException("Player O cannot be the same as Player X");
        }

        this.playerO = playerO;

        if (isBotGame) {
            botPlay();
            updateGameState();
        }
    }

    /**
     * Check if the game is against a bot
     * @return true if the game is against a bot, false otherwise
    */
    public void setAsBotgame() {
        this.isBotGame = true;
    }

    /**
     * Update the game state to check for a win or draw
     * This method should be called after each move
     * It sets isWin, isDraw, and winner accordingly
    */
    private void updateGameState() {
        // check rows, columns, and diagonals for a win
        // allfilled is true if all cells are filled
        boolean allFilled = true;

        for (int i = 0; i < 3; i++) {
            if (grid[i][0] != null && grid[i][0] == grid[i][1] && grid[i][1] == grid[i][2]) {
                isWin = true;
                winner = grid[i][0];
                return;
            }
            if (grid[0][i] != null && grid[0][i] == grid[1][i] && grid[1][i] == grid[2][i]) {
                isWin = true;
                winner = grid[0][i];
                return;
            }

            if (allFilled) {
                for (int j = 0; j < 3; j++) {
                    if (grid[i][j] == null) {
                        allFilled = false;
                        break;
                    }
                }
            }
        }

        // check diagonals
        if (grid[0][0] != null && grid[0][0] == grid[1][1] && grid[1][1] == grid[2][2]) {
            isWin = true;
            winner = grid[0][0];
            return;
        }

        if (grid[0][2] != null && grid[0][2] == grid[1][1] && grid[1][1] == grid[2][0]) {
            isWin = true;
            winner = grid[0][2];
            return;
        }

        // check for draw
        if (allFilled) {
            isDraw = true;
        }
    }

    /**
     * Check if the game has finished
     * A game is finished if there is a win, a draw, or it has been aborded
     * @return true if the game is finished, false otherwise
    */
    public boolean isFinished() {
        return isDraw || isWin || isAborded;
    }

    /**
     * Abord the game
     * This sets the isAborded flag to true
     * the game is aborder by the server when a player disconnects
    */
    public void abortGame() {
        this.isAborded = true;
    }
    

    /**
     * return the game sttus 
     * but in the real case, ONGOING will be 
     * never use
     * @return
    */
    private String getGameStatus() {
        if (isAborded) {
            return "OPPONENT QUIT";
        } else if (isWin) {
            return winner.toString().toUpperCase() + " WON";
        } else if (isDraw) {
            return "DRAW";
        } else {
            return "";
        }
    }


    /*
     * return true is the game is a bot game
    */
    public boolean hasBot() {
        return isBotGame;
    }

    /**
     * for update request we have to send the game status
     * this method will create the state to send to the user
     * @return
     */
    public String getGridState() {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid.length; j++) {
                if (grid[i][j] == null) {
                    builder.append(" ");
                } else {
                    builder.append(grid[i][j].toString());
                }
            }

            builder.append(TictactoeConst.END_OF_MESSAGE);
        }

        if (isFinished()) {
            builder.append(getGameStatus());
            builder.append(TictactoeConst.END_OF_MESSAGE);
        }

        return builder.toString();
    }

    /**
     * If the game is against a bot, make the bot play its turn
     * The bot plays as player O
    */

    private void botPlay() {
        // simple bot logic: play the first available cell
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (this.grid[i][j] == null) {
                    this.grid[i][j] = this.currentPlayer; // assuming bot is always O
                    toogleCurrentPlayer();
                    return;
                }
            }
        }
    }

}
