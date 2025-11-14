package com.example.unscramble.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.unscramble.data.allWords
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.update
import com.example.unscramble.data.SCORE_INCREASE
import com.example.unscramble.data.MAX_NO_OF_WORDS

/*
 *
 * In previous codelabs, you learned about configuration changes in Android.
 * When a configuration change occurs, Android restarts the activity from scratch,
 * running all the lifecycle startup callbacks.
 *
 * The ViewModel stores the app-related data that isn't destroyed when the Android
 * framework destroys and recreates activity.
 * ViewModel objects are automatically retained and they are not destroyed
 * like the activity instance during configuration change.
 * The data they hold is immediately available after the recomposition.
 *
 */

class GameViewModel : ViewModel() {
    // Game UI state

    // Backing property to avoid state updates from other classes
    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private lateinit var currentWord: String

    // Set of words used in the game
    private var usedWords: MutableSet<String> = mutableSetOf()

    // "private set" se usa para que una propiedad sea pública para leerla,
    // pero su setter sea privado, es decir, solo la clase puede modificarla.
    var userGuess by mutableStateOf("")
        private set

    init {
        resetGame()
    }

    fun resetGame() {
        usedWords.clear()
        _uiState.value = GameUiState(currentScrambledWord = pickRandomWordAndShuffle())
    }

    private fun pickRandomWordAndShuffle(): String {
        // Continue picking up a new random word until you get one that hasn't been used before
        currentWord = allWords.random()
        if (usedWords.contains(currentWord)) {
            return pickRandomWordAndShuffle()
        } else {
            usedWords.add(currentWord)
            return shuffleCurrentWord(currentWord)
        }
    }

    private fun shuffleCurrentWord(word: String): String {
        val tempWord = word.toCharArray()
        // Scramble the word
        tempWord.shuffle()
        while (String(tempWord).equals(word)) {
            tempWord.shuffle()
        }
        return String(tempWord)
    }

    fun updateUserGuess(guessedWord: String) {
        userGuess = guessedWord
    }

    private fun updateGameState(updatedScore: Int) {
        if (usedWords.size == MAX_NO_OF_WORDS){
            // Last round in the game, update isGameOver to true, don't pick a new word
            _uiState.update { currentState ->
                currentState.copy(
                    isGuessedWordWrong = false,
                    score = updatedScore,
                    isGameOver = true
                )
            }
        } else{
            // Normal round in the game
            _uiState.update { currentState ->
                currentState.copy(
                    currentScrambledWord = pickRandomWordAndShuffle(),
                    currentWordCount = currentState.currentWordCount.inc(),
                    isGuessedWordWrong = false,
                    score = updatedScore
                )
            }
        }
    }

    fun checkUserGuess() {
        if (userGuess.equals(currentWord, ignoreCase = true)) {
            // User's guess is correct, increase the score
            // and call updateGameState() to prepare the game for next round
            val updatedScore = _uiState.value.score.plus(SCORE_INCREASE)
            updateGameState(updatedScore)
        } else {
            // User's guess is wrong, show an error
            _uiState.update { currentState ->
                currentState.copy(isGuessedWordWrong = true)
            }
        }
        // Reset user guess
        updateUserGuess("")
    }

    fun skipWord() {
        updateGameState(_uiState.value.score)
        // Reset user guess
        updateUserGuess("")
    }
}
