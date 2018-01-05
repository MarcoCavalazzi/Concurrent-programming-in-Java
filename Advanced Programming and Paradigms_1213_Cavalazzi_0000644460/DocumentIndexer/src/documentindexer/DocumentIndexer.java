/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package documentindexer;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

/**
 *
 * @author Marco Carlo Cavalazzi
 */
public class DocumentIndexer extends Application {
    public static Boolean globalPauseFlag = false;      // This variable will tell the Threads if they have to pause the execution or not.
    public static Boolean globalStopFlag = false;       // This variable will tell the Threads if they have to pause the execution or not.
    public static Boolean globalIndexingFlag = false;   // This variable will tell the Threads if they have to pause the execution or not.
    static Boolean automaticIndexUpdaterHasBeenCreated = false;   // This variable will tell us if we have already created the Thread that will take care of the automatic updating of the index.
    Thread automaticIndexUpdater;
    
    @Override
    public void start(Stage primaryStage) { // Function that creates the GUI and sets the event links between buttons and functions.
        
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.TOP_CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25, 25, 25, 25));
        
        int rowIndex = 0;
        
        Text scenetitle = new Text("Welcome!");
        scenetitle.setFont(Font.font("Tahoma", FontWeight.NORMAL, 22));
        grid.add(scenetitle, 0, rowIndex, 2, 1);
    //(column, row, columnSpan, rowSpan)
        rowIndex++;
        
        Text programDescription = new Text("The Document Indexer is a utility that indexes and keeps indexed all the documents stored in a directory (including its\n" +
                                            "subdirectories), so as to make it possible to retrieve the list of documents containing some words in a very efficient\n" +
                                            "and fast way.");
        grid.add(programDescription, 0, rowIndex, 4, 1);
        rowIndex++;
        Text programDescription2 = new Text("The first thing to do is to select a folder and build an index for it.");
        grid.add(programDescription2, 0, rowIndex, 3, 1);
        rowIndex += 2;
        
        Label directoryString = new Label("Choose a directory:");
        grid.add(directoryString, 0, rowIndex);
        
        TextField directoryURLField = new TextField("");    // example: C:\\Users\\Mark\\Desktop
        directoryURLField.setTooltip(new Tooltip("Please write the URL of the directory to analize or use the 'Browse...' button on the right."));
        grid.add(directoryURLField, 1, rowIndex);
        
        Button browseButton = new Button("Browse...");
        // Creating the event that will allow the User to select a folder when pressing the 'Browse...' button.
        browseButton.setOnAction((event) -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            final File selectedDirectory = directoryChooser.showDialog(primaryStage);   // Opening the directory chooser window.
            if (selectedDirectory != null) {
                String directoryURLString = selectedDirectory.getAbsolutePath();    // Reading the selected URL.
                directoryURLField.setText(directoryURLString);      // Pasting the URL into the GUI for a visual check from the User.
            }
        });
        grid.add(browseButton, 2, rowIndex);
        rowIndex++;
        
        HBox hbButtons = new HBox();
        hbButtons.setAlignment(Pos.BOTTOM_CENTER);
        hbButtons.setSpacing(10.0);
        
        // Elements defined here for the "btnStart" event hadler.
        Text loadingText = new Text("                                                    Building the index...");     // Section in which we will display some information while the index is being built and at the end of the process.
        TextArea indexingProcessArea = new TextArea();
        ConcurrentHashMap<Object, Object> globalIndex = new ConcurrentHashMap<Object, Object>(0, 0.9f);     // The index. We want it to start with no elements inside and keep a load factor of 90%.
        ObservableList filesList = FXCollections.observableArrayList();     // The list of files that appear in the ListView in the GUI.
        FlowPane enteredKeywordsPane = new FlowPane();  // Here we will write all the keywords typed from the User in the GUI.
        Button btnQuery = new Button("Launch Query");
        Button btnStart = new Button("Build Index");
        Button btnPause = new Button("Pause Indexing");
        Button btnStop = new Button("Stop Indexing");
        btnStart.setOnAction((event) -> { buildIndexButton_onclick(globalIndex, indexingProcessArea, loadingText, directoryURLField.getText(), btnStart, btnQuery, automaticIndexUpdater); });
        btnPause.setOnAction((event) -> { pauseIndexing(btnPause); } );
        btnStop.setOnAction((event) -> { stopIndex(loadingText, btnStart, btnPause, btnQuery); });
        btnQuery.setOnAction((event) -> { launchQuery(globalIndex, btnQuery, filesList, enteredKeywordsPane); });
        hbButtons.getChildren().addAll(btnStart, btnPause, btnStop);
        grid.add(hbButtons, 0, rowIndex, 3, 1);
        rowIndex++;
        
        // Creating the information that will be shown while the index is being built.
        loadingText.setFont(Font.font("Tahoma", FontWeight.NORMAL, 10));
        loadingText.setOpacity(0.75);
        loadingText.setTextAlignment(TextAlignment.CENTER);
        loadingText.setVisible(false);      // It will be displayed during the index creation process (see "buildIndex()" function).
        grid.add(loadingText, 0, rowIndex, 3, 1);
        rowIndex++;
        
        Text queryInstructions = new Text("Once the index has been built it is possible to choose one or more keywords\n" +
                                          "and launch a query that will list all the .txt files available in the selected \n" +
                                          "directory having those keywords in them. Attention: the search is \"case sensitive\".");
        grid.add(queryInstructions, 0, rowIndex, 3, 1);
        rowIndex++;
        
        Label keywordsLabel = new Label("Choose the keywords:");
        grid.add(keywordsLabel, 0, rowIndex);

        TextField keywords = new TextField("Write a keyword and press 'Enter'.");
        keywords.setMinWidth(192); // This way all the suggestion is shown.
        keywords.setTooltip(new Tooltip("For each keyword, write it and press 'Enter'."));
        keywords.setOpacity(0.75);
        keywords.focusedProperty().addListener( new ChangeListener<Boolean>(){
            @Override
            public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue){
                if (newPropertyValue){
                    // Textfield on focus
                    keywords.clear();   // Deleting the text in the TextField (the suggestion this way will be deleted automatically, but it will remain as a tooltip if needed).
                } //else Textfield out focus
            }
        });
        
        keywords.setOnKeyPressed((event) -> {
            if (event.getCode() == KeyCode.ENTER) { // If 'Enter' is pressed
                // The word is copied in the field below and the User will be free to write another keyword.
                Button temp = new Button(keywords.getText().trim());    // Creating a button with the word written in input (taking away the spaces).
                temp.setTooltip(new Tooltip("Left click on a keyword to delete it."));
                temp.setOnAction((ev) -> { deleteKeyword(enteredKeywordsPane, temp.getText()); });
                enteredKeywordsPane.getChildren().add(temp);
                keywords.clear(); // Clearing the text in the TextField
            }
        });
        grid.add(keywords, 1, rowIndex);
        
        grid.add(btnQuery, 2, rowIndex);
        rowIndex++;

        // Here we will display every keyword inserted from the User.
        // Definig the width after which the new elements will appear in a new line.
        enteredKeywordsPane.setMinWidth(200);
        enteredKeywordsPane.setMaxWidth(200);
        // Setting the spacing between elements in the FlowPane
        enteredKeywordsPane.setVgap(5);
        enteredKeywordsPane.setHgap(5);
        grid.add(enteredKeywordsPane, 1, rowIndex);
        rowIndex++;
        rowIndex++;
        rowIndex++;
        
        indexingProcessArea.setEditable(false); // It is just a window that shows the indexing process. It is needed only for output purposes.
        indexingProcessArea.setMinWidth(427);
        indexingProcessArea.setMaxWidth(427);
        grid.add(indexingProcessArea, 0, rowIndex, 3, 10);
    //(column, row, columnSpan, rowSpan)
        rowIndex++;
        
        ListView<Object> filesInfoArea = new ListView<>();
        filesInfoArea.setEditable(false);    // It is just a window that shows the indexing process. It is needed only for output purposes.
        filesInfoArea.setMinWidth(190);
        filesInfoArea.setMaxWidth(190);
        filesInfoArea.setItems(filesList);   // The ListView will take the data from this list.
        grid.add(filesInfoArea, 4, 3, 1, 19);
    //(column, row, columnSpan, rowSpan)
        rowIndex++;
        
        
        Scene scene = new Scene(grid, 696, 570);
        
        primaryStage.setTitle("Document Indexer");
        primaryStage.getIcons().add(new Image("file:icons/icon.png"));  // Defining the icon of the application.
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(final WindowEvent e){
                try {
                    automaticIndexUpdater.interrupt(); // Terminate the automatic index updater Thread.
                } catch (Exception ex) {}
                finally{
                    Platform.exit();    // Close the program.
                }
            }
        });
        primaryStage.show();
        
    }
    
    
    // Function that builds the index of the .txt files and the words present in them starting from the directory passed in input as URL string.
    private void buildIndexButton_onclick(ConcurrentHashMap<Object, Object> globalIndex, TextArea indexingProcessArea, Text loadingText, String url, Button btnStart, Button btnQuery, Thread automaticIndexUpdater){
        if(globalIndexingFlag){     // If it is "true" it means that the updating Thread is working with the index. We should wait until the operation is completed.
            CustomMethods.createPopUpWindow("An automatic indexing update process is running.\nPlease wait until the process completes.");
            return;
        }else{
            globalIndexingFlag = true;      // Starting to build the index.
        }
        if( url.length() < 3 ){      // Consistency check. If the URL is not valid we launch a pop-up window saying that the URL has to be reviewed.
            CustomMethods.createPopUpWindow("Please enter a valid URL for the directory first.");
            globalIndexingFlag = false;
            return;
        }
        globalIndex.clear();            // Resetting the Index. It is going to be updated from zero every time.
        
        System.out.println("Starting indexing process.");
        
        globalStopFlag = false;         // If the index has been stopped before we put the variable back to false to allow the Threads to do their job undisturbed.
        indexingProcessArea.clear();    // Clearing the content of the indexing output area in the GUI.
        
        (new Thread(new IndexBuilderThread(loadingText, indexingProcessArea, url, globalIndex, btnStart, btnQuery, automaticIndexUpdater))).start();  // Launching the Thread that will index it.
    }
    
    
    /* This method is fired from the "Pause indexing" button in the GUI. If clicked the button will change the global variable 'globalPauseFlag',
     * that is checked from the Threads while they are indexing the files in the selected folder and sub-folders.
     * If it is "false" the Thread will continue, otherwise they will enter in a keep-waiting loop until the variable is set back to "true" through 
     * the press of the "Continue indexing" button in the GUI. */
    private void pauseIndexing(Button btnPause) {
        if(globalPauseFlag){
            globalPauseFlag = false;
            btnPause.setText("Pause indexing");
        }else{
            globalPauseFlag = true;
            btnPause.setText("Continue indexing");
        }
    }
    
    private void stopIndex(Text loadingText, Button btnStart, Button btnPause, Button btnQuery){
        if(globalPauseFlag){    // If the system is in pause we want the global variable to go back to its initial state and the button to retake its initial value.
            globalPauseFlag = false;
            btnPause.setText("Pause indexing");
        }
        
        globalStopFlag = true;      // It will be put back to "false" in the buildIndex() function.
        
        btnStart.setDisable(false);     // The User can now launch a new indexing process.
        loadingText.setVisible(false);    // Hides the progress bar.
        btnQuery.setDisable(true);      // The User cannot launch a query without a well built index.
    }
    
    // Function that deletes a keyword from the chosen keywords 
    private void deleteKeyword(FlowPane keywordsGroup, String keyword){
        // Reading the keywords from the GUI
        ObservableList<Node> chosenKeywords = keywordsGroup.getChildren();
        // Deleting EVERY keyword with the text corresponding to the "keyword" string variable.
        chosenKeywords.removeIf( (elem) -> elem.toString().endsWith("'"+ keyword +"'") );
    }
    
    private void launchQuery(ConcurrentHashMap<Object, Object> globalIndex, Button btnQuery, ObservableList outputFilesList, FlowPane enteredKeywordsPane){
        btnQuery.setDisable(true);  // Disabling the button for the time needed to execute the query.
        
        // Consistency check
        if(globalIndex.size() < 1){     // Consistency check: there has to be at least one word in the index.
            CustomMethods.createPopUpWindow("The index is empty. No word has been found during the indexing process.\nPlease check the directory's URL and try again.");
            btnQuery.setDisable(false);     // Re-enabling the query button
            return;
        }
        
        outputFilesList.clear();  // Cleaning the list linked to the listView in the GUI containing all the resulting file names
        Object[] keywords = enteredKeywordsPane.getChildren().toArray();
        keywords = CustomMethods.removeButtonsDuplicates(keywords);
        
        int keywordsLength = keywords.length;
        if(keywordsLength < 1){     // Consistency check, If there are no keywords chosen for the query we can stop the execution sgnaling the problem to the User.
            CustomMethods.createPopUpWindow("Please enter a keyword first. Then press \"Launch Query\"");
            btnQuery.setDisable(false);     // Re-enabling the query button
            return;
        }
        
        // Statement and cycle with debugging purpose.
        System.out.println(keywordsLength +" keywords:");
        for(Object o : keywords){
            System.out.println("  "+ o );
        }
        
        long startTime = System.nanoTime();
        
        ArrayList<File> outputFiles = new ArrayList<File>();
        String word;
        if(keywordsLength == 1){
            word = (String) keywords[0];     // Keyword considered.
            if( globalIndex.containsKey( word ) ){
                ArrayList<File> filesList = (ArrayList<File>) globalIndex.get(word);
                
                for(File file : filesList){
                    // Copying the filenames in the GUI as hyperlinks for the User. This way it is possible to click on a filename and open directly the file.
                    // It will be possible to see the full URL of the file hovering it with the mouse.
                    outputFilesList.add(  CustomMethods.hyperlinkMe(file.getName(), file.getPath())  );   // Updating the list of files satisfying the query.
                }
            }else{
                CustomMethods.createPopUpWindow("The entered keyword is not in the indexed files.");
            }
        }else{  // The User chose more than one keyword for the query
            
            ArrayList<File> tempList;   // temporary list used in the following cycle to examine the files containing a keyword
            int i=0;    // Proceeding with the next element in the list of entered keywords.
            while( i < keywordsLength ){
                word = (String) keywords[i];     // Reading the next keyword
                // Debug statement System.out.println("Inside the second cycle. Word: "+ word+ "  is in the index?  "+ globalIndex.containsKey(word));
                if( globalIndex.containsKey(word) ){        // If it is indexed...
                    // For every file in 'outputFiles' we have to check if they are also in the list of files containing the last keyword considered.
                    // If not, we will remove them from the list.
                    tempList = (ArrayList<File>) globalIndex.get(word);
                    ArrayList<File> outputFilesCopy = (ArrayList<File>) outputFiles.clone();    // This copy is necessary to handle the possible removal of elements from 'outputFiles' inside the cycle.
                    for(File f : outputFilesCopy){
                        // Debug statement System.out.println("contains("+ f +")  the word "+ word +"? -> "+ tempList.contains(f));
                        if(!tempList.contains(f)){      // If the temporary list does not contain the file in the 'outputList' it means that that file does not have both of the keywords. We can, thus, remove it.
                            outputFiles.remove(f);      // This file had some keywords but not the last one. Since we would like to keep only the files with all of the keywords we remove this one form the output list.
                        }
                    }
                }else{
                    // One of the entered keywords is not in the index. It means that no file has it. Thus, we erase the 'outputFiles' list.
                    outputFiles.clear();
                }
                
                // Consistency check. If the list of files containing every keyword considered so far is already empty we can stop the algorithm and exit the function.
                if(outputFiles.isEmpty()){
                    CustomMethods.createPopUpWindow("None of the files contains all of the entered keywords at the same time.");
                    btnQuery.setDisable(false);     // Re-enabling the query button
                    return;
                }
                
                i++;
            }
            
            
            for(File file : outputFiles){
                // Copying the filenames in the GUI as hyperlinks for the User. This way it is possible to click on a filename and open directly the file.
                // It will be possible to see the full URL of the file hovering it with the mouse.
                outputFilesList.add(  CustomMethods.hyperlinkMe(file.getName(), file.getPath())  );   // Updating the list of files satisfying the query.
            }
        }
        
        long elapsedTime = System.nanoTime() - startTime;   // Reading the time used to execute the query.
        outputFilesList.add("> Query executed successfully <");
        Date date = new Date();
        outputFilesList.add("Time elapsed: "+ (elapsedTime/1000000) +"ms");
        outputFilesList.add("Time of the day  "+ date.getHours() +":"+ date.getMinutes() +":"+ date.getSeconds());
        btnQuery.setDisable(false);     // Re-enabling the query button
    }
    
    
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    
}
