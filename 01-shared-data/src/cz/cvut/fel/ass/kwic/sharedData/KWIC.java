package cz.cvut.fel.ass.kwic.sharedData;

import java.io.*;
import java.util.Arrays;

public class KWIC {

    /**
     * Keeps all the characters.
     */
    private char[] chars = new char[1000];
    private int charCounter = 0;

    /**
     * Keeps indexes of each line starting character.
     */
    private int[] lineIndexes = new int[10];

    /**
     * Keeps 2D array of indexes of circular shifts of the lines.
     * First key is start of the line from lineIndexes. Second index marks where the line starts.
     */
    private int[][] wordIndexes;

    /**
     * Keeps 2D array of indexes of alphabetically sorted circular shifts of the lines.
     * First key is start of the line from lineIndexes. Second index marks where the line starts.
     */
    private int[][] alphabetizedIndexes;

    public KWIC() {
        Arrays.fill(lineIndexes, -1);   // Initialize all line indexes to -1
        Arrays.fill(chars, (char) -1);  // Initialize all chars to -1
    }

    /**
     * Read input characters from file and saves it to shared storage.
     * Input Component.
     * <p>
     * Reads input file and saves its characters to shared "chars" array. It also marks starts of each line by
     * saving indexes of "chars" on which the line starts to "lineIndexes" array.
     * </p>
     *
     * @throws IOException
     */
    public void input(Reader reader) throws IOException {
        int inputChar;
        int lineIterator = 0;
        boolean newLine = true;

        while ((inputChar = reader.read()) != -1) {
            // Mark start of the line
            if (newLine) {
                lineIndexes = expandIfNeeded(lineIndexes, lineIterator + 1);
                lineIndexes[lineIterator++] = charCounter;
                newLine = false;
            }

            // If this is the end of line, mark it and rewrite the character to space
            // (this avoids problems while outputting the lines)
            if (inputChar == '\n') {
                newLine = true;
                inputChar = ' ';
            }

            // Save the character
            chars = expandIfNeeded(chars, charCounter + 1);
            chars[charCounter++] = (char) inputChar;
        }
    }

    /**
     * Circular shifts the lines.
     * Circular Shift Component.
     * <p/>
     * Iterates through lines and searches for words. Each start of the word is then indexed to wordIndexes.
     * Each circular shift begins with different word on the line, therefore each word index is also an index
     * of circular shift.
     * <p/>
     */
    public void circularShift() {
        // Allocate shift indexes
        wordIndexes = new int[lineIndexes.length][30];

        // Iterate through lines
        int lineIterator = 0;
        while (lineIterator + 1 < lineIndexes.length && lineIndexes[lineIterator] != -1) {
            int lineStart = lineIndexes[lineIterator];
            int lineEnd = lineIndexes[lineIterator + 1] != -1 ? lineIndexes[lineIterator + 1] : charCounter;

            Arrays.fill(wordIndexes[lineIterator], -1);

            // Iterate through the line chars and look for new words
            int indexIterator = 0;
            boolean newWord = true;
            for (int charIterator = lineStart; charIterator < lineEnd; ++charIterator) {
                // If this is the end of word, skip it
                if (chars[charIterator] == ' ') {
                    newWord = true;
                    continue;
                }

                // Mark start of new word
                if (newWord) {
                    wordIndexes[lineIterator] = expandIfNeeded(wordIndexes[lineIterator], indexIterator + 1);
                    wordIndexes[lineIterator][indexIterator++] = charIterator;
                    newWord = false;
                }
            }

            // Bump line iterator
            ++lineIterator;
        }
    }

    /**
     * Alphabetically orders all circular shifts of each line in the file.
     * Alphabetizer Component.
     */
    public void alphabetizer() {
        // Fill up alphabetized indexes
        alphabetizedIndexes = new int[lineIndexes.length][30];
        for (int line = 0; line < wordIndexes.length; ++line) {
            Arrays.fill(alphabetizedIndexes[line], -1);
            for (int word = 0; word < wordIndexes[line].length && wordIndexes[line][word] != -1; ++word) {
                alphabetizedIndexes[line][word] = word;
            }
        }

        // Iterate through lines
        for (int lineIterator = 0;
             lineIterator < lineIndexes.length && lineIndexes[lineIterator] != -1;
             ++lineIterator) {
            for (int bubbleIterator = 0; bubbleIterator < alphabetizedIndexes[lineIterator].length - 1; ++bubbleIterator) {
                for (int wordIterator = 0; wordIterator < alphabetizedIndexes[lineIterator].length - bubbleIterator - 1; ++wordIterator) {

                    if (alphabetizedIndexes[lineIterator][wordIterator + 1] == -1) {
                        break;
                    }

                    int wordIndex = wordIndexes[lineIterator][alphabetizedIndexes[lineIterator][wordIterator]];
                    int nextWordIndex = wordIndexes[lineIterator][alphabetizedIndexes[lineIterator][wordIterator + 1]];

                    char wordChar = (char) -1;
                    char nextWordChar = (char) -1;

                    // Read chars until they aren't equal
                    for (; wordChar == nextWordChar && nextWordIndex < chars.length && chars[nextWordIndex] != -1;
                         ++wordIndex, ++nextWordIndex) {
                        wordChar = chars[wordIndex];
                        nextWordChar = chars[nextWordIndex];
                        ++wordIndex;
                        ++nextWordIndex;
                    }

                    // Now that chars aren't equal, compare them, then do the bubble switch
                    if (wordChar > nextWordChar) {
                        // Bubble sort switch
                        int temp = alphabetizedIndexes[lineIterator][wordIterator];
                        alphabetizedIndexes[lineIterator][wordIterator] = alphabetizedIndexes[lineIterator][wordIterator + 1];
                        alphabetizedIndexes[lineIterator][wordIterator + 1] = temp;
                    }
                }
            }
        }
    }

    /**
     * Outputs alphabetically sorted circular shifts of the lines.
     * Output Component.
     */
    public void output(Writer writer) throws IOException {
        for (int line = 0; line < lineIndexes.length && lineIndexes[line] != -1; ++line) {
            for (int word = 0;
                 word < alphabetizedIndexes[line].length && alphabetizedIndexes[line][word] != -1;
                 ++word) {
                int wordStart = wordIndexes[line][alphabetizedIndexes[line][word]];
                int lineStart = lineIndexes[line];
                int lineEnd = lineIndexes[line + 1] != -1 ? lineIndexes[line + 1] : charCounter;

                // From the word start to the end of line
                for (int charIndex = wordStart; charIndex < lineEnd; ++charIndex) {
                    writer.write(chars[charIndex]);
                }
                // From the beginning of the line to the word
                for (int charIndex = lineStart; charIndex < wordStart; ++charIndex) {
                    writer.write(chars[charIndex]);
                }
                writer.write('\n');
            }
            writer.write('\n');
        }
        writer.flush();
    }

    /**
     * Expands the given array if the given index is out of bounds.
     *
     * @param array original array
     * @param index the index
     * @return expanded array
     */
    private int[] expandIfNeeded(int[] array, int index) {
        if (index >= array.length) {
            int[] temp = new int[array.length * 2];
            Arrays.fill(temp, -1);
            System.arraycopy(array, 0, temp, 0, array.length);
            array = temp;
        }
        return array;
    }

    /**
     * Expands the given array if the given index is out of bounds.
     *
     * @param array original array
     * @param index the index
     * @return expanded array
     */
    private char[] expandIfNeeded(char[] array, int index) {
        if (index >= array.length) {
            char[] temp = new char[array.length * 2];
            Arrays.fill(temp, (char) -1);
            System.arraycopy(array, 0, temp, 0, array.length);
            array = temp;
        }
        return array;
    }

    /**
     * Main function.
     * Master Control Component.
     *
     * @param args
     */
    public static void main(String[] args) {
        try {
            KWIC kwic = new KWIC();
            kwic.input(new FileReader(new File("input.txt")));
            kwic.circularShift();
            kwic.alphabetizer();
            kwic.output(new FileWriter(new File("output.txt")));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
